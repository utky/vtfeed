(ns vtfeed.handler.feed
  (:require [ataraxy.core :as ataraxy]
            [ataraxy.response :as response] 
            [clj-time.core :as t]
            [clj-time.format :as f]
            [integrant.core :as ig]
            [vtfeed.boundary.feed :as boundary]))

;; id, name, url are needed
(defmethod ig/init-key :vtfeed.handler.feed/create [_ {:keys [db]}]
  (fn [{[_ feed] :ataraxy/result}]
    (do (boundary/create-feed db feed)
        [::response/ok])))

(defmethod ig/init-key :vtfeed.handler.feed/list [_ {:keys [db]}]
  (fn [{[_ since] :ataraxy/result}]
    (let [feeds (boundary/list-feed db (f/parse (f/formatters :date-time) since) 10)]
        [::response/ok feeds])))

(defmethod ig/init-key :vtfeed.handler.feed/delete [_ {:keys [db]}]
  (fn [{[_ feed-id] :ataraxy/result}]
    (do (boundary/delete-feed db feed-id)
        [::response/ok])))
