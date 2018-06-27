(ns vtfeed.boundary.feed
  (:require [clojure.spec.alpha :as s]
            [duct.database.sql]
            [honeysql.core :as sql]
            [honeysql.helpers :refer :all :as helpers]
            [clojure.java.jdbc :as jdbc]
            [clojure.string :as string]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clj-time.jdbc]
            [vtfeed.boundary.core :as core]))

(def time-format (f/formatters :date-time-no-ms))

(def normalize-def
  {:id            :id
   :channel-id    :yt:channelId
   :title         :title
   :description   [:media:group :media:description]
   :url           [:link :href]
   :thumbnail     [:media:group :media:thumbnail :url]
   :views         [:media:group :media:community :media:statistics :views]
   :rate-count    [:media:group :media:community :media:starRating :count]
   :rate-average  [:media:group :media:community :media:starRating :average]
   :rate-min      [:media:group :media:community :media:starRating :min]
   :rate-max      [:media:group :media:community :media:starRating :max]
   :content       str
   :published    #(->> % :published (f/parse time-format))
   :updated      #(->> % :updated (f/parse time-format))})

(defn normalize
  [m definition]
  (letfn [(xform
            [src dst [k f]]
            (assoc dst
                   k
                   (cond
                     (or (fn? f) (keyword? f))  (f src)
                     :default                   (get-in src f))))]
    (reduce (partial xform m) {} definition)))



; Access point from handler to external effect
(defprotocol Feed
  (create-feed [db feed])
  (get-feed [db feed-id])
  (delete-feed [db feed-id])
  (update-feed [db feed])
  (list-feed [db since limit]))

(extend-protocol Feed
  duct.database.sql.Boundary

  (create-feed [db feed]
    (core/execute! db
                   (-> (insert-into :feeds)
                       (values [(normalize feed normalize-def)])
                       sql/format)))


  (get-feed [db feed-id]
    (first (core/query db
                       (-> (sql/build :select :*
                                      :from :feeds
                                      :where [:= :id feed-id])
                           sql/format))))

  (delete-feed [db feed-id]
    (core/execute! db
                   (-> (delete-from :feeds)
                       (where [:= :id feed-id])
                       sql/format)))

  (update-feed [db feed]
    (core/execute! db
                   (-> (helpers/update :feeds)
                       (sset feed)
                       (where [:= :id (:id feed)])
                       sql/format)))

  (list-feed [db since limit]
    (core/query db
                (-> (sql/build :select :*
                               :from :feeds
                               :where [:>= :published since]
                               :limit limit
                               :order-by [[:updated :asc]])
                    sql/format))))

(defn save-feed
  [db feed]
  (if-let [fetched (get-feed db (:id feed))]
    (update-feed db feed)
    (create-feed db feed)))
