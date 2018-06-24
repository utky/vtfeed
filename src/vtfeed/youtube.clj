(ns vtfeed.youtube
  (:require [org.httpkit.client :as http]
            [clojure.data.json :as json]
            [clojure.xml :as xml]
            [clojure.zip :as zip]
            [clojure.java.io :as io]))

(def youtube-v3-api-endpoint
  "https://www.googleapis.com/youtube/v3/")

(def api-key
;;  (first (line-seq (clojure.java.io/reader "API_KEY"))))
  (first ["hoge"]))

(def channels
  {:path "channels"
   :part "snippet" })

(def activities
  {:path "activities"
   :part "snippet,contentDetails"})

(def default-option
  {:timeout (* 60 1000)
   })

(defn request
  "Returns promise of http request again youtube api"
  [params]
  (let [query-params (assoc (dissoc params :path) :key api-key)
        opts (merge default-option {:query-params query-params})
        path (:path params)
        url (str youtube-v3-api-endpoint path)]
    (http/get url opts)))

(def youtube-video-feed
  "https://www.youtube.com/feeds/videos.xml")

(defn fetch-feed
  "Fetchs and returns atom data from youtube channel"
  [{:keys [channelId]}]
  (http/get youtube-video-feed
            {:timeout      (* 60 1000)
             :query-params {:channel_id channelId}}))

(defn read-xml-seq
  [content]
  (-> (java.io.ByteArrayInputStream. (.geBytes content "utf-8"))
      (xml/parse)
      (xml-seq)))

(defn entry
  [node]
  (let [fields [:id :title :link :author :published :updated]]
    nil))

(defn contents->map
  [col]
  (letfn [(assoc-tag
            [m {:keys [tag content attrs]}]
            (-> m
                (assoc tag (contents->map content))
                (merge attrs)))
          (text?
            [cl]
            (and (= 1 (count cl))
                 (string? (first cl))))]
    (if (text? col)
      (first col)
      (reduce assoc-tag {} col))))

(defn read-feed
  [content]
  (->> (read-xml-seq content)
       (filter #(= :entry (:tag %)))
       (map (comp contents->map :content))))
