(ns dev
  (:refer-clojure :exclude [test])
  (:require [clojure.repl :refer :all]
            [fipp.edn :refer [pprint]]
            [clojure.tools.namespace.repl :refer [refresh]]
            [clojure.java.io :as io]
            [duct.core :as duct]
            [duct.core.repl :as duct-repl]
            [duct.repl.figwheel :refer [cljs-repl]]
            [eftest.runner :as eftest]
            [integrant.core :as ig]
            [integrant.repl :refer [clear halt go init prep reset]]
            [integrant.repl.state :refer [config system]]
            [clojure.java.shell :as shell]
            [org.httpkit.client :as http]
            [clojure.data.json :as json]))

(duct/load-hierarchy)

(defn read-config []
  (duct/read-config (io/resource "dev.edn")))

(defn test []
  (eftest/run-tests (eftest/find-tests "test")))

(clojure.tools.namespace.repl/set-refresh-dirs "dev/src" "src" "test")

(when (io/resource "local.clj")
  (load "local"))

(integrant.repl/set-prep! (comp duct/prep read-config))

;; ------------------------------------------------
;; Operations for database container

(def db-name
  "vtfeed")

(def db-image
  "postgres:10.3")

(defn run-db
  []
  (shell/sh "docker" "run" "-d" "--name" db-name
            "-p" "5432:5432" db-image))

(defn start-db
  []
  (shell/sh "docker" "start" db-name))

(defn stop-db
  []
  (shell/sh "docker" "stop" db-name))


(def es-name
  "vtfeed-es")

(def es-image
  "docker.elastic.co/elasticsearch/elasticsearch-oss:6.2.4")

(defn run-es
  []
  (shell/sh "docker" "run" "-d" "--name" es-name
            "-p" "9200:9200"
            "-p" "9300:9300"
            "-e" "\"discovery.type=single-node\""
            es-image))

(defn start-es
  []
  (shell/sh "docker" "start" es-name))

(defn stop-es
  []
  (shell/sh "docker" "stop" es-name))


;; ------------------------------------------------
;; HTTP Client

(defn url [path] (str "http://localhost:3000" path))

(def json-opts
  {:headers {"Content-Type" "application/json; charset=UTF-8"}
   })

(defn api-get
  [path]
  @(http/get (url path) json-opts))

(defn api-post
  [path body]
  @(http/post (url path) (merge json-opts {:body (json/write-str body)})))

(defn api-delete
  [path]
  @(http/delete (url path)))
