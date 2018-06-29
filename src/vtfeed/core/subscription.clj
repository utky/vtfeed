(ns vtfeed.core.subscription
  (:require [clojure.spec.alpha :as s]
            [clj-time.format :as f]))

(def datetime-format (f/formatters :date-time))
(def datetime?
  (s/conformer
   #(f/parse datetime-format %)
   #(f/unparse datetime-format %)))

(s/def ::id      string?)
(s/def ::name    string?)
(s/def ::url     string?)
(s/def ::created datetime?)
(s/def ::last    datetime?)

(s/def ::subscription
  (s/keys
   :req-un [::id ::name ::url]
   :opt-un [::created ::last]))
