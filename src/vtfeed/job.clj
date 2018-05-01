(ns vtfeed.job
  (:require [integrant.core :as ig]
            [clojure.core.async :as a]
            [chime :refer [chime-ch]]
            [clj-time.core :as time]
            [clj-time.periodic :as periodic]
            [vtfeed.youtube :as youtube]
            [org.elasticsearch.client :as es]
            [clojure.data.json :as json]))

;; 1. clock generator
;; tick event emitter
;; --------------------------------------------

(defn- make-periodic-chan
  [seconds]
  (chime-ch (periodic/periodic-seq (time/now) (time/seconds seconds))
            {:ch (a/chan (a/dropping-buffer 1))}))

(defmethod ig/init-key :vtfeed.job/tick
  [_ {:keys [seconds]}]
  (make-periodic-chan seconds))

(defmethod ig/halt-key! :vtfeed.job/tick
  [_ ch]
  (a/close! ch))

;; 2. buffer for array of subscription
;; source of channel update
;; --------------------------------------------
(defmethod ig/init-key :vtfeed.job/subscription
  [_ {:keys [concurrency]}]
  (a/chan concurrency))

(defmethod ig/halt-key! :vtfeed.job/subscription
  [_ ch]
  (a/close! ch))

;; 3. accumulator for subscribed event
;; --------------------------------------------
(defmethod ig/init-key :vtfeed.job/updates
  [_ {:keys [buf-or-n]}]
  (a/chan buf-or-n))

(defmethod ig/halt-key! :vtfeed.job/updates
  [_ ch]
  (a/close! ch))

;; A) fetch target subscriptions to be updated
;; --------------------------------------------
(defmethod ig/init-key :vtfeed.job/subscriber
  [_ {:keys [concurrency tick subscription]}]
  (a/thread
    (loop []
      (when-let [time (a/<!! tick)]
        (a/>!! subscription "UCp6993wxpyDPHUpavwDFqgg")
        (recur)))))

;; B) fetch actual updates from remote endpoint
;; --------------------------------------------
(defn- handle-subscription
  [{:keys [status headers body error]}]
  (if error
    {:error error}
    {:data  (json/read-json body true)}))

(defmethod ig/init-key :vtfeed.job/collector
  [_ {:keys [subscription updates]}]
  (a/thread
    (loop []
      (when-let [s (a/<!! subscription)]
        ;; this line will run asynchronously
        ;; called and immediatelly returnsa so it does not block
        ;; but start thread in http-kit layer
        (a/>!! updates
               (-> {:channelId s
                    :publishedAfter "2018-04-27T00:00:00.0Z"}
                   (merge youtube/activities)
                   youtube/request
                   deref
                   handle-subscription))
        (recur)))))

;; C) forward subscribed data to somewhereesle
;; --------------------------------------------

(defmethod ig/init-key :vtfeed.job/consumer
  [_ {:keys [updates rest-client index]}]
  (a/thread
    (loop []
      (when-let [activities (a/<!! updates)]
        (if (:data activities)
          (let [resp (es/save-bulk rest-client
                                   index
                                   (get-in activities [:data :items]))]
            (prn (.toString resp)))
          (prn "error: " activities))
        (recur)))))
