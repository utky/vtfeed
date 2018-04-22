(ns vtfeed.job
  (:require [integrant.core :as ig]
            [clojure.core.async :as a]
            [chime :refer [chime-ch]]
            [clj-time.core :as time]
            [clj-time.periodic :as periodic]))

(defmethod ig/init-key :vtfeed.job/channel [_ _]
  (chime-ch (periodic/periodic-seq (time/now) (time/seconds 5))
            {:ch (a/chan (a/dropping-buffer 1))}))

(defmethod ig/halt-key! :vtfeed.job/channel [_ ch]
  (a/close! ch))

(defmethod ig/init-key :vtfeed.job/hello [_ {:keys [ch]}]
  (a/go-loop []
    (when-let [time (a/<! ch)]
      (prn "Hello world at " time)
      (recur))))
