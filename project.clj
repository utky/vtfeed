(defproject vtfeed "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [duct/core "0.6.2"]
                 [duct/module.logging "0.3.1"]
                 [duct/module.web "0.6.4"]
                 [duct/module.ataraxy "0.2.0"]
                 [duct/module.cljs "0.3.2"]
                 [duct/module.sql "0.4.2"]
                 [duct/migrator.ragtime "0.2.1"]
                 [org.postgresql/postgresql "42.1.4"]
                 [honeysql "0.9.2"]
                 [http-kit "2.2.0"]
                 [clj-http "3.9.0"]
                 [org.clojure/data.json "0.2.6"]
                 [clj-time "0.14.3"]
                 [jarohen/chime "0.2.2"]
                 [reagent "0.7.0"]
                 [re-frame "0.10.5"]
                 [day8.re-frame/http-fx "0.1.6"]
                 [cljs-ajax "0.7.3"]
                 [com.andrewmcveigh/cljs-time "0.5.2"]]
  :plugins [[duct/lein-duct "0.10.6"]]
  :main ^:skip-aot vtfeed.main
  :uberjar-name  "vtfeed-standalone.jar"
  :resource-paths ["resources" "target/resources"]
  :prep-tasks     ["javac" "compile" ["run" ":duct/compiler"]]
  :profiles
  {:dev  [:project/dev :profiles/dev]
   :repl {:prep-tasks   ^:replace ["javac" "compile"]
          :repl-options {:init-ns user
                         :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}}
   :uberjar {:aot :all}
   :profiles/dev {:dependencies   [[org.clojure/test.check "0.9.0"]]}
   :project/dev  {:source-paths   ["dev/src"]
                  :resource-paths ["dev/resources"]
                  :dependencies   [[integrant/repl "0.2.0"]
                                   [eftest "0.4.1"]
                                   [kerodon "0.9.0"]]}})
