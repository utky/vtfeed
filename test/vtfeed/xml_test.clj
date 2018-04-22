(ns vtfeed.xml-test
  (:require  [clojure.test :as t]
             [vtfeed.core.xml :as xml]))


(defn tokino-sora
  []
  (slurp "test/tokino_sora.xml"))

(t/deftest test-entries
  (t/testing "number of entries"
    (t/is (= 15 (count (xml/entries (tokino-sora)))))))
