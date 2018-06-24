(ns vtfeed.handler.static
  (:require [ataraxy.core :as ataraxy]
            [ataraxy.response :as response]
            [integrant.core :as ig]))

(defmethod ig/init-key :vtfeed.handler.static/index [_ options]
  (fn [{[_] :ataraxy/result}]
    [::response/found "/index.html"]))
