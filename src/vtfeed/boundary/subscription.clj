(ns vtfeed.boundary.subscription
  (:require [clojure.spec.alpha :as s]
            [duct.database.sql]
            [honeysql.core :as sql]
            [honeysql.helpers :refer :all :as helpers]
            [clojure.java.jdbc :as jdbc]
            [clojure.string :as string]))

(defn- kebab [col]
  (-> col string/lower-case (string/replace "_" "-")))

; Access point from handler to external effect
(defprotocol Subscription
  (create-subscription [db subscription])
  (delete-subscription [db subscription-id])
  (list-subscription [db])
  (get-subscription [db subscription-id]))

(extend-protocol Subscription
  duct.database.sql.Boundary
  (create-subscription [db subscription]
    (jdbc/execute! (:spec db)
                   (do (println subscription)
                   (-> (insert-into :subscriptions)
                       (values [subscription])
                       sql/format))
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
                       :identifiers kebab))))

