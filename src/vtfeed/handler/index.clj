(ns vtfeed.handler.index
  (:require [ataraxy.core :as ataraxy]
            [ataraxy.response :as response] 
            [ring.util.response :as ring]
            [integrant.core :as ig]))

(def ^:private body-template
  "<!DOCTYPE html>
<html>
<head>
<title>Hello World</title>
  <meta charset=\"utf-8\">
<meta name=\"description\" content=\"\">
<meta name=\"author\" content=\"\">
<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">
<link rel=\"stylesheet\" href=\"\">
<link rel=\"shortcut icon\" href=\"\">
</head>
<body>
<h1>Hello World</h1>
</body>
</html>")

(defmethod ig/init-key :vtfeed.handler/index [_ options]
  (fn [{[_] :ataraxy/result}]
    (-> {:status 200
         :body body-template}
        (ring/content-type "text/html")
        (ring/charset "utf8"))))
