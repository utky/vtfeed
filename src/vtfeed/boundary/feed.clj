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

; Access point from handler to external effect
(defprotocol Feed
  (create-feed [db feed])
  (get-feed [db feed-id])
  (delete-feed [db feed-id])
  (update-feed [db feed])
  (list-feed [db until limit]))

(extend-protocol Feed
  duct.database.sql.Boundary

  (create-feed [db feed]
    (core/execute! db
                   (-> (insert-into :feeds)
                       (values [feed])
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

  (list-feed [db until limit_]
    (core/query db
                (-> (select :*)
                    (from :feeds)
                    (merge-where
                     (if-not (nil? until)
                      [:>= :published until]))
                    (limit limit_)
                    (order-by [[:updated :desc]])
                    sql/format))))

(defn save-feed
  [db feed]
  (if-let [fetched (get-feed db (:id feed))]
    (update-feed db feed)
    (create-feed db feed)))
