(ns vtfeed.core.xml
  (:require [clojure.xml :as xml]
            [clojure.zip :as zip]))

(defn- as-byte-stream
  [s]
  (java.io.ByteArrayInputStream. (.getBytes s)))

(defn read-feed
  [input]
  (println input))

(defn tag?
  [k]
  (comp #(= k %) :tag))

(def entry? (tag? :entry))

(defn read-str
  "Read string as XML"
  [s]
  (xml/parse (as-byte-stream s)))

(defn- seq-of-depth-1
  ""
  [zipper]
  (->> zipper
       zip/down
       (iterate zip/right)
       (take-while seq)))


(def attributed-elements
  #{:link :media:content :media:thumbnail})


(defn content
  [e f]
  (if (attributed-elements (:tag e))
    (:attrs e)
    (f (:content e))))

(defn to-map
  ([] {})
  ([x] x)
  ([x & xs]
   (reduce #(assoc %1 (:tag %2) (content %2 (partial apply to-map))) {} (cons x xs))))

(defn entries
  [s]
  (->> s
       read-str
       zip/xml-zip
       seq-of-depth-1
       (filter (comp entry? first))
       (map (comp (partial apply to-map) :content))))
