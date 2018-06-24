(ns vtfeed.boundary.subscription
  (:require [clojure.spec.alpha :as s]
            [duct.database.sql]
            [honeysql.core :as sql]
            [honeysql.helpers :refer :all :as helpers]
            [clojure.java.jdbc :as jdbc]
            [clojure.string :as string]
            [clj-time.core :as t]
            [clj-time.jdbc]
            [vtfeed.boundary.core :as core]))

; Access point from handler to external effect
(defprotocol Subscription
  (create-subscription [db subscription])
  (delete-subscription [db subscription-id])
  (list-subscription [db])
  (get-subscription [db subscription-id])
  (update-subscription-last [db subscription-id last])
  (list-next-subscription [db limit]))

(extend-protocol Subscription
  duct.database.sql.Boundary
  (create-subscription [db subscription]
    (core/execute! db
                   (-> (insert-into :subscriptions)
                       (values [subscription])
                       sql/format)))

  (delete-subscription [db subscription-id]
    (core/execute! db
                   (-> (delete-from :subscriptions)
                       (where [:= :id subscription-id])
                       sql/format)))

  (list-subscription [db]
    (core/query db
                (-> (sql/build :select :*
                               :from :subscriptions)
                    sql/format)))

  (get-subscription [db subscription-id]
    (first (core/query db
                       (-> (sql/build :select :*
                                      :from :subscriptions
                                      :where [:= :id subscription-id])
                           sql/format))))

  (update-subscription-last [db subscription-id lst]
    (core/execute! db
                   (-> (update :subscriptions)
                       (sset {:last lst})
                       (where [:= :id subscription-id])
                       sql/format)))

  (list-next-subscription [db limit]
    (core/query db
                (-> (sql/build :select :*
                               :from :subscriptions
                               :limit limit
                               :order-by [[:last :asc]])
                    sql/format))))

