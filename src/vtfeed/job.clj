(ns vtfeed.job
  (:require [integrant.core :as ig]
            [clojure.core.async :as a]
            [chime :refer [chime-ch]]
            [clj-time.core :as time]
            [clj-time.periodic :as periodic]
            [vtfeed.youtube :as youtube]
            [vtfeed.boundary.subscription :as subscription]
            [vtfeed.boundary.feed :as feed]
            [clojure.tools.logging :as log]))

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

(defn run-subscriber
  [{:keys [db time limit]}]
  (letfn [(update-last-of-subscription
            [sub]
            (prn "update-last-of-subscription " sub)
            (prn "last updated: " (:last sub))
            (prn "now updated: " time)
            (subscription/update-subscription-last db (:id sub) time)
            sub)]
    (->> (subscription/list-next-subscription db limit)
         (map update-last-of-subscription))))

(defmethod ig/init-key :vtfeed.job/subscriber
  [_ {:keys [db concurrency tick subscription]}]
  (a/thread
    (loop []
      (when-let [time (a/<!! tick)]
        (let [subs (run-subscriber {:db db :time time :limit concurrency})]
          (prn "subscriber : " subs)
          (doall (map (fn [sub] (a/>!! subscription sub)) subs)))
        (recur)))))

;; B) fetch actual updates from remote endpoint
;; --------------------------------------------
(defn- handle-subscription
  [{:keys [status headers body error]} subscription]
  (prn "handle subscription status: " status " subscription: " subscription)
  (if error
    {:error error
     :subscription subscription}
    {:data  (youtube/read-feed body)
     :subscription subscription}))

(defn handle-fetch
  [resp]
  (if-let [body (:body resp)]
    (update resp :body youtube/read-feed)
    resp))

(defn run-collector
  [sub]
  (prn "run-collector: " sub)
  (-> (youtube/fetch-feed {:channel-id (:id sub)})
      deref
      log/spy
      handle-fetch))

(defmethod ig/init-key :vtfeed.job/collector
  [_ {:keys [subscription updates]}]
  (a/thread
    (loop []
      (when-let [s (a/<!! subscription)]
        (prn "collector input: " s)
        (let [response (run-collector s)]
          (if-let [err (:error response)]
            (prn err)
            (a/>!! updates (:body response))))
        (recur)))))

;; C) forward subscribed data to somewhereesle
;; --------------------------------------------

(defmethod ig/init-key :vtfeed.job/consumer
  [_ {:keys [db updates]}]
  (a/thread
    (loop []
      (when-let [feeds (a/<!! updates)]
        (doall (map (partial feed/save-feed db) feeds))
        (recur)))))
