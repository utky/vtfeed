(ns vtfeed.handler.subscription
  (:require [ataraxy.core :as ataraxy]
            [ataraxy.response :as response] 
            [integrant.core :as ig]
            [vtfeed.boundary.subscription :as boundary]))

(defmethod ig/init-key :vtfeed.handler.subscription/create [_ {:keys [db]}]
  (fn [{[_ subscription] :ataraxy/result}]
    (do (boundary/create-subscription db subscription)
        [::response/ok])))

(defmethod ig/init-key :vtfeed.handler.subscription/list [_ {:keys [db]}]
  (fn [{[_] :ataraxy/result}]
    (let [subscriptions (boundary/list-subscription db)]
        [::response/ok subscriptions])))

(defmethod ig/init-key :vtfeed.handler.subscription/delete [_ {:keys [db]}]
  (fn [{[_ subscription-id] :ataraxy/result}]
    (do (boundary/delete-subscription db subscription-id)
        [::response/ok])))

(defmethod ig/init-key :vtfeed.handler.subscription/get [_ {:keys [db]}]
  (fn [{[_ subscription-id] :ataraxy/result}]
    (let [subscription (boundary/get-subscription db subscription-id)]
        [::response/ok subscription])))
