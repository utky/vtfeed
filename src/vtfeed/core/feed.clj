(ns vtfeed.core.feed
  (:require [clojure.spec.alpha :as s]
            [clojure.java.io :as io]))

(defprotocol FeedSchema
  (create-schema-if-absent [this deifinition]))

(defprotocol FeedClient
  (save-feeds [this schema feeds])
  (fetch-feeds [this schema query]))
