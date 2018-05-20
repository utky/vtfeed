(ns duct.module.vtfeed.web
  (:require [duct.core :as core]
            [integrant.core :as ig]))

(def ^:private static-config
  {:static    {:resources ["duct/module/web/public"
                           "vtfeed/public"]}
   })

(defn- apply-web-module
  [config options]
  (core/merge-configs config
                      {:duct.middleware.web/defaults static-config}))

(defmethod ig/init-key :duct.module.vtfeed/web [_ options]
  {:req #{:duct.module.web/api}
   :fn (fn [config] (apply-web-module config options))})
