(ns vtfeed.job-test
  (:require  [clojure.test :as t]
             [chime :refer [chime-ch]]
             [clj-time.core :as time]
             [clojure.core.async :as a :refer [<! go-loop]]))

