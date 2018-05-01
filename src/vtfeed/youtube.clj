(ns vtfeed.youtube
  (:require [org.httpkit.client :as http]
            [clojure.data.json :as json]))

(def youtube-v3-api-endpoint
  "https://www.googleapis.com/youtube/v3/")

(def api-key
  (first (line-seq (clojure.java.io/reader "API_KEY"))))

(def channels
  {:path "channels"
   :part "snippet" })

(def activities
  {:path "activities"
   :part "snippet,contentDetails"})

(def default-option
  {:timeout (* 60 1000)
   })

(defn request
  "Returns promise of http request again youtube api"
  [params]
  (let [query-params (assoc (dissoc params :path) :key api-key)
        opts (merge default-option {:query-params query-params})
        path (:path params)
        url (str youtube-v3-api-endpoint path)]
    (http/get url opts)))

(def youtube-video-feed
  "https://www.youtube.com/feeds/videos.xml")

(defn fetch-feed
  "Fetchs and returns atom data from youtube channel"
  [channel-id callback]
  (http/get youtube-video-feed
            {:timeout      (* 60 1000)
             :query-params {:channel_id channel-id}}
            callback))

(comment 
(-> {:id "UCp6993wxpyDPHUpavwDFqgg"}
    (merge channels)
    request
    :body
    (#(spit "test/tokino_sora_channels.json" %)))

(spit "tokino_sora_activities.json"
      (-> {:channelId "UCp6993wxpyDPHUpavwDFqgg"
           :publishedAfter "2018-04-27T00:00:00.0Z"}
          (merge activities)
          request
          deref
          :body))
)


(comment
(-> (slurp "test/tokino_sora_activities.json")
    (json/read-json true)
    :items
    first
    keys)
)
