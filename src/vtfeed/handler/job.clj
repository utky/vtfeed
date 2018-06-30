(ns vtfeed.handler.job
  (:require [ataraxy.core :as ataraxy]
            [ataraxy.response :as response] 
            [integrant.core :as ig]
            [clj-time.core :as t]
            [clojure.core.async :as a]))

(defmethod ig/init-key :vtfeed.handler.job/create [_ {:keys [ch]}]
  (fn [{[_] :ataraxy/result}]
    (let [now (t/now)]
      (a/>!! ch now))
    [::response/ok]))
