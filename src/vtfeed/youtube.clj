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
   :part "snippet"})

(def default-option
  {:timeout (* 60 1000)
   })

(defn request
  [params]
  (let [query-params (assoc (dissoc params :path) :key api-key)
        opts (merge default-option {:query-params query-params})
        path (:path params)
        url (str youtube-v3-api-endpoint path)]
    @(http/get url opts)))

(comment 
(-> {:id "UCp6993wxpyDPHUpavwDFqgg"}
    (merge channels)
    request
    :body
    (#(spit "test/tokino_sora_channels.json" %)))
)
(comment
(-> (slurp "test/tokino_sora_activities.json")
    (json/read-json true)
    :items
    first
    keys)
)
