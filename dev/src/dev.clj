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

(defn run-db
  []
  (shell/sh "docker" "run" "-d" "--name" db-name "-p" "5432:5432" "postgres"))

(defn start-db
  []
  (shell/sh "docker" "start" db-name))

(defn stop-db
  []
  (shell/sh "docker" "stop" db-name))

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
