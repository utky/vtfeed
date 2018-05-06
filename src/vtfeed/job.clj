(ns vtfeed.job
  (:require [integrant.core :as ig]
            [clojure.core.async :as a]
            [chime :refer [chime-ch]]
            [clj-time.core :as time]
            [clj-time.periodic :as periodic]
            [vtfeed.youtube :as youtube]
            [org.elasticsearch.client :as es]
            [clojure.data.json :as json]
            [vtfeed.boundary.subscription :as boundary]))

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
  [_ {:keys [db concurrency tick subscription]}]
  (a/thread
    (loop []
      (when-let [time (a/<!! tick)]
        (prn "tick " time)
        (letfn [(send-next-subscription
                  [sub]
                  (prn "send-next-subscription " sub)
                  (do (a/>!! subscription sub)
                      sub))
                (update-last-of-subscription
                  [sub]
                  (prn "update-last-of-subscription " sub)
                  (boundary/update-subscription-last db (:id sub) time))]
          (->> (doall (boundary/list-next-subscription db concurrency))
               (map send-next-subscription)
               (map update-last-of-subscription)))
        (recur)))))

;; B) fetch actual updates from remote endpoint
;; --------------------------------------------
(defn- handle-subscription
  [{:keys [status headers body error]} subscription]
  (prn "handle subscription status: " status " subscription: " subscription)
  (if error
    {:error error
     :subscription subscription}
    {:data  (json/read-json body true)
     :subscription subscription}))

(defmethod ig/init-key :vtfeed.job/collector
  [_ {:keys [subscription updates]}]
  (a/thread
    (loop []
      (when-let [s (a/<!! subscription)]
        (letfn [(send-or-skip
                  [result]
                  (when-not (empty? (get-in result [:data :items]))
                    (a/>!! updates result)))]
          (-> {:channelId      (:id s)
               :publishedAfter (:last s)}
              (merge youtube/activities)
              youtube/request
              deref
              (handle-subscription s)
              (send-or-skip)))
        (recur)))))

;; C) forward subscribed data to somewhereesle
;; --------------------------------------------

(defmethod ig/init-key :vtfeed.job/consumer
  [_ {:keys [db updates rest-client index]}]
  (a/thread
    (loop []
      (when-let [activities (a/<!! updates)]
        (if (:data activities)
          (when-let [saved? (es/save-bulk rest-client
                                        index
                                        (get-in activities [:data :items]))]
            (boundary/update-subscription-last db
                                      (get-in activities [:subscription :id])
                                      (get-in activities [:subscription :last])))
          (prn "error: " activities))
        (recur)))))
