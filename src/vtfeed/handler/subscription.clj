(ns vtfeed.handler.subscription
  (:require [ataraxy.core :as ataraxy]
            [ataraxy.response :as response] 
            [clj-time.core :as t]
            [integrant.core :as ig]
            [vtfeed.core.subscription :as core]
            [vtfeed.boundary.subscription :as boundary]
            [clojure.spec.alpha :as s]))

(defmethod ig/init-key :vtfeed.handler.subscription/create [_ {:keys [db]}]
  (fn [{[_ subscription] :ataraxy/result}]
    {:pre [(s/valid? ::core/subscription subscription)]}
    (boundary/create-subscription
     db
     (-> subscription
         (assoc :created (t/now))
         (assoc :last    (t/now))))
    [::response/ok]))

(defmethod ig/init-key :vtfeed.handler.subscription/list [_ {:keys [db]}]
  (fn [{[_] :ataraxy/result}]
    [::response/ok (boundary/list-subscription db)]))

(defmethod ig/init-key :vtfeed.handler.subscription/delete [_ {:keys [db]}]
  (fn [{[_ subscription-id] :ataraxy/result}]
    {:pre [(s/valid? ::core/id subscription-id)]}
    (boundary/delete-subscription db subscription-id)
    [::response/ok]))

(defmethod ig/init-key :vtfeed.handler.subscription/get [_ {:keys [db]}]
  (fn [{[_ subscription-id] :ataraxy/result}]
    {:pre [(s/valid? ::core/id subscription-id)]}
    [::response/ok (boundary/get-subscription db subscription-id)]))
