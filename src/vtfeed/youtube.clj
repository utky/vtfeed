(ns vtfeed.youtube
  (:require [org.httpkit.client :as http]
            [clojure.xml :as xml]
            [vtfeed.core.feed :as core]))

(def youtube-video-feed
  "https://www.youtube.com/feeds/videos.xml")

(defn fetch-feed
  "Fetchs and returns atom data from youtube channel"
  [{:keys [channel-id]}]
  (http/get youtube-video-feed
            {:timeout      (* 60 1000)
             :query-params {:channel_id channel-id}}))

(defn read-xml-seq
  [content]
  (-> (java.io.ByteArrayInputStream. (.getBytes content "utf-8"))
      (xml/parse)
      (xml-seq)))

(defn contents->map
  [col]
  (letfn [(assoc-tag
            [m {:keys [tag content attrs]}]
            (let [c (contents->map content)]
              (assoc m tag (merge attrs c))))
          (text?
            [cl]
            (and (= 1 (count cl))
                 (string? (first cl))))]
    (if (text? col)
      {:value (first col)}
      (reduce assoc-tag {} col))))

(def normalize-def
  {:id            [:id :value]
   :channel-id    [:yt:channelId :value]
   :video-id      [:yt:videoId :value]
   :title         [:title :value]
   :description   [:media:group :media:description :value]
   :url           [:link :href]
   :author-name   [:author :name :value]
   :author-uri    [:author :uri :value]
   :thumbnail     [:media:group :media:thumbnail :url]
   :views         [:media:group :media:community :media:statistics :views]
   :rate-count    [:media:group :media:community :media:starRating :count]
   :rate-average  [:media:group :media:community :media:starRating :average]
   :rate-min      [:media:group :media:community :media:starRating :min]
   :rate-max      [:media:group :media:community :media:starRating :max]
   :published     [:published :value]
   :updated       [:updated :value]})

(def coercion
  {:views #(Integer/parseInt %)
   :rate-count #(Integer/parseInt %)
   :rate-average #(Float/parseFloat %)
   :rate-min #(Integer/parseInt %)
   :rate-max #(Integer/parseInt %)
   :published core/parse-datetime
   :updated core/parse-datetime
   })

(defn coerce
  [m]
  (reduce (fn
            [m [k f]]
            (update m k f))
          m
          coercion))

(defn normalize
  [m definition]
  (letfn [(xform
            [src dst [k f]]
            (assoc dst k (get-in src f)))]
    (reduce (partial xform m) {} definition)))


(defn read-feed
  [content]
  (->> (read-xml-seq content)
       (filter #(= :entry (:tag %)))
       (map (comp coerce
                  #(normalize % normalize-def)
                  contents->map
                  :content))))
