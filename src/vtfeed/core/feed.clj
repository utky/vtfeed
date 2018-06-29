(ns vtfeed.core.feed
  (:require [clojure.spec.alpha :as s]
            [clj-time.format :as f]
            [clojure.java.io :as io]))

(def datetime-format (f/formatters :date-time-no-ms))
(def datetime?
  (s/conformer
   #(f/parse datetime-format %)
   #(f/unparse datetime-format %)))
(defn parse-datetime
  [s]
  (f/parse datetime-format s))

(s/def ::id           string?)
(s/def ::channel-id   string?)
(s/def ::video-id     string?)
(s/def ::title        string?)
(s/def ::description  string?)
(s/def ::url          string?)
(s/def ::author-name  string?)
(s/def ::author-uri   string?)
(s/def ::thumbnail    string?)
(s/def ::views        number?)
(s/def ::rate-count   number?)
(s/def ::rate-average double?)
(s/def ::rate-min     number?)
(s/def ::rate-max     number?)
(s/def ::published    datetime?)
(s/def ::updated      datetime?)

(s/def ::feed
  (s/keys
   :req-un [::id
            ::channel-id
            ::title
            ::url
            ::author-name
            ::author-uri
            ::thumbnail
            ::views
            ::rate_count
            ::rate_count
            ::rate_min
            ::rate_max]
   :opt-un [::description
            ::video-id
            ::published
            ::updated]))
