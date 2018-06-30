(ns vtfeed.handler.job
  (:require [ataraxy.core :as ataraxy]
            [ataraxy.response :as response] 
            [integrant.core :as ig]
            [clj-time.core :as t]
            [clojure.core.async :as a]
            [vtfeed.youtube :as youtube]
            [vtfeed.boundary.subscription :as subscription]
            [vtfeed.boundary.feed :as feed]
            [duct.logger :refer [log]]))

(defn subscribe
  [db logger {:keys [time limit]}]
  (letfn [(update-last-of-subscription
            [sub]
            (log logger :info ::target-subscription sub)
            (log logger :info ::target-last-updated (:last sub))
            (log logger :info ::target-now-updated time)
            (subscription/update-subscription-last db (:id sub) time)
            sub)]
    (->> (subscription/list-next-subscription db limit)
         (map update-last-of-subscription))))


(defn- handle-fetch
  [logger {:keys [status body error]}]
  (log logger :info :fetch-feed-result {:status status})
  (let [parsed (youtube/read-feed body)]
    (log logger :info :read-body parsed)
    parsed))

(defn collect-feed
  [db logger sub]
  (log logger :info :collect-feed-for (select-keys sub [:id]))
  (->> {:channel-id (:id sub)}
       (youtube/fetch-feed logger)
       (handle-fetch logger)))

(defn collect
  [db logger subscripsions]
  (apply concat (map (partial collect-feed db logger) subscripsions)))

(defn consume-feed
  [db logger feed]
  (log logger :info :save-feed (select-keys feed [:id :title :author-name]))
  (feed/save-feed db feed))

(defn consume
  [db logger feeds]
  (log logger :info :first-of-consume (first feeds))
  (map (partial consume-feed db logger) feeds))

(defmethod ig/init-key :vtfeed.handler.job/create [_ {:keys [db logger]}]
  (fn [{[_] :ataraxy/result}]
    (->> {:time   (t/now)
          :limit  5}
         (subscribe db logger)
         (collect   db logger)
         (consume   db logger)
         (#(doall %))
         (reduce (fn [x y] x) [::response/ok]))))
