(ns vtfeed.core.feed-test
  (:require [vtfeed.core.feed :as sut]
            [clojure.test :as t]
            [clojure.spec.alpha :as s]
            [clojure.java.io :as io]
            [clojure.spec.gen.alpha :as gen]))

(defn url?
  [x]
  (instance? java.net.URL x))

(s/def ::id string?)
(s/def ::title string?)
(s/def ::description string?)
(s/def ::channel-id string?)
(s/def ::url url?)
(s/def ::thumbnail url?)

(s/def ::entry
  (s/keys
   :req [::id
         ::title
         ::description
         ::channel-id
         ::url
         ::thumbnail]))

;; Sample

(s/explain
 ::entry
 {::id "weraw"
  ::title "hoge"
  ::description "description"
  ::channel-id 1
  ::url (io/as-url "http://localhost/")
  ::thumbnail (io/as-url "http://localhost")})

(t/deftest test-url?
  (t/testing "valid URL"
    (t/are [x] (url? x)
      (java.net.URL. "http://exampl.com")))
  (t/testing "invalid URL"
    (t/are [y] (not (url? y))
      "http://exampl.com"
      (java.net.URI. "http://example.com"))))
