(ns vtfeed.handler.feed
  (:require [ataraxy.core :as ataraxy]
            [ataraxy.response :as response] 
            [clj-time.core :as t]
            [clj-time.format :as f]
            [integrant.core :as ig]
            [vtfeed.boundary.feed :as boundary]
            [cheshire.generate :refer [add-encoder]]))

;; First, add a custom encoder for a class:
(add-encoder org.joda.time.DateTime
             (fn [d jsonGenerator]
               (.writeString jsonGenerator (f/unparse (f/formatters :date-time) d))))

;; id, name, url are needed
(defmethod ig/init-key :vtfeed.handler.feed/create [_ {:keys [db]}]
  (fn [{[_ feed] :ataraxy/result}]
    (do (boundary/create-feed db feed)
        [::response/ok])))

(defmethod ig/init-key :vtfeed.handler.feed/list [_ {:keys [db]}]
  (fn [{[_ until limit] :ataraxy/result}]
    (let [feeds (boundary/list-feed db (when-not (nil? until) (f/parse (f/formatters :date-time) until)) limit)]
      [::response/ok feeds])))

(defmethod ig/init-key :vtfeed.handler.feed/delete [_ {:keys [db]}]
  (fn [{[_ feed-id] :ataraxy/result}]
    (do (boundary/delete-feed db feed-id)
        [::response/ok])))
