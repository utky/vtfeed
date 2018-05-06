(ns vtfeed.boundary.subscription
  (:require [clojure.spec.alpha :as s]
            [duct.database.sql]
            [honeysql.core :as sql]
            [honeysql.helpers :refer :all :as helpers]
            [clojure.java.jdbc :as jdbc]
            [clojure.string :as string]
            [clj-time.core :as t]
            [clj-time.jdbc]))

(defn- kebab [col]
  (-> col string/lower-case (string/replace "_" "-")))

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
    (jdbc/execute! (:spec db)
                   (-> (insert-into :subscriptions)
                       (values [subscription])
                       sql/format)
                   :identifiers kebab))

  (delete-subscription [db subscription-id]
    (jdbc/execute! (:spec db)
                   (-> (delete-from :subscriptions)
                       (where [:= :id subscription-id])
                       sql/format)
                   :identifiers kebab))

  (list-subscription [db]
    (jdbc/query (:spec db)
                (-> (sql/build :select :*
                               :from :subscriptions)
                    sql/format)
                :identifiers kebab))

  (get-subscription [db subscription-id]
    (first (jdbc/query (:spec db)
                       (-> (sql/build :select :*
                                      :from :subscriptions
                                      :where [:= :id subscription-id])
                           sql/format)
                       :identifiers kebab)))

  (update-subscription-last [db subscription-id lst]
    (jdbc/execute! (:spec db)
                   (-> (update :subscriptions)
                       (sset {:last lst})
                       (where [:= :id subscription-id])
                       )))

  (list-next-subscription [db limit]
    (jdbc/query (:spec db)
                (-> (sql/build :select :*
                               :from :subscriptions
                               :limit limit
                               :order-by [[:last :asc]])
                    sql/format)
                :identifiers kebab)))

