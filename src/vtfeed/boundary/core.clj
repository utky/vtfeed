(ns vtfeed.boundary.core
  (:require [honeysql.core :as sql]
            [honeysql.helpers :refer :all :as helpers]
            [clojure.java.jdbc :as jdbc]
            [clojure.string :as string]
            [clj-time.core :as t]
            [clj-time.jdbc]))

(defn kebab [col]
  (-> col string/lower-case (string/replace "_" "-")))

(defn query
  [db q]
  (jdbc/query (:spec db) q :identifiers kebab))

(defn execute!
  [db q]
  (jdbc/execute! (:spec db) q :identifiers kebab))
