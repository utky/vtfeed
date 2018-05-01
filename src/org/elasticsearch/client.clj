(ns org.elasticsearch.client
  (:require [integrant.core :as ig]
            [vtfeed.core.feed :refer [FeedSchema FeedClient] :as feed]
            [clojure.data.json :as json])
  (:import (org.elasticsearch.client RestClient
                                     RestHighLevelClient)
           (org.elasticsearch.client Requests)
           (org.elasticsearch.action.admin.indices.create CreateIndexRequest)
           (org.elasticsearch.action.admin.indices.mapping.put PutMappingRequest)
           (org.elasticsearch.common.xcontent XContentType)
           (org.elasticsearch.action.index IndexRequest)
           (org.elasticsearch.action.bulk BulkRequest)
           (org.elasticsearch.index.query RangeQueryBuilder)
           (org.elasticsearch.search.builder SearchSourceBuilder)
           (org.elasticsearch.action.search SearchRequest)
           (org.apache.http HttpHost Header)))

(comment
  " Start here
https://artifacts.elastic.co/javadoc/org/elasticsearch/client/elasticsearch-rest-high-level-client/6.2.4/org/elasticsearch/client/RestHighLevelClient.html
")

(def default-mapping
  {:_doc
   {:properties
    {:kind         { :type "keyword" }
     :etag         { :type "keyword" }
     :id           { :type "keyword" }
     :publishedAt  { :type "date" :format "strict_date_time" }
     :channelId    { :type "keyword" }
     :title        { :type "text" }
     :description  { :type "text" }
     :url          { :type "keyword" }
     :width        { :type "integer" }
     :height       { :type "integer" }
     :channelTitle { :type "text" }
     :type         { :type "keyword" }
     :videoId      { :type "keyword" }}}})

(defn normalize
  [m]
  (let [base              (dissoc m :snippet :contentDetails)
        snippet           (:snippet m)
        default-thumbnail (get-in snippet [:thumbnails :default])
        flat-snippet      (dissoc snippet :thumbnails)
        content-details   (get-in m [:contentDetails :upload])
        ]
    (merge base flat-snippet default-thumbnail content-details)))

(def empty-headers
  (make-array Header 0))

(defn exist-index
  [client index]
  (let [llc  (.getLowLevelClient client)
        res-read (.performRequest llc
                                  "HEAD"
                                  (str "/" index)
                                  (java.util.Collections/emptyMap)
                                  empty-headers)
        status-code (.. res-read (getStatusLine) (getStatusCode))]
    (= 200 status-code)))

(defn create-index
  [client index]
  (let [req-idx      (.. (Requests/createIndexRequest index)
                         (mapping "_doc"
                                  (json/write-str default-mapping)
                                  XContentType/JSON))]
    (.. client indices (create req-idx empty-headers))))

(defn delete-index
  [client index]
  (let [req-idx      (Requests/deleteIndexRequest index)]
    (.. client indices (delete req-idx empty-headers))))

(defn feed->index-req
  [index feed]
  (let [id-value (:videoId feed)
        type-value "_doc"]
    (.. (Requests/indexRequest index)
        (id id-value)
        (type type-value)
        (source (json/write-str feed) XContentType/JSON))))


(defn- only-upload
  [feed]
  (= "upload" (get-in feed [:snippet :type])))


(defn save-bulk
  [client index feeds]
  (let [each-reqs (map (comp (partial feed->index-req index)
                             normalize)
                       (filter only-upload feeds))
        req       (reduce #(.add %1 %2) (BulkRequest.) each-reqs)]
    (.. client
        (bulk req empty-headers)
        (toString))))

(defn search-feeds
  "time range query
  https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-range-query.html

  RangeQueryBuilder
  "
  [client {:keys [start end tags]}]
  (let [query  (.. (RangeQueryBuilder. "publishedAt")
                   (from start)
                   (to end))
        ssb    (.. (SearchSourceBuilder.) (query query))
        req    (.. (SearchRequest.) (source ssb))]
    (.search client req)))

(defn http-hosts
  "expects [{:hostname \"localhost\" :port 9200 :scheme \"http\"}]"
  [hosts]
  (letfn [(make-http-host
            [{:keys [hostname port scheme]}]
            (HttpHost. hostname port scheme))]
    (map make-http-host hosts)))

(defn rest-client
  [hosts]
  (RestHighLevelClient. (RestClient/builder (into-array HttpHost hosts))))

(defn make-rest-client
  [hs]
  (rest-client (doall (http-hosts hs))))

(defmethod ig/init-key :org.elasticsearch.client/rest-client
  [_ {:keys [endpoints index]}]
  (let [client (make-rest-client endpoints)
        exist  (exist-index client index)]
    (when-not exist
      (create-index client index))
    client))

(defmethod ig/halt-key! :org.elasticsearch.client/rest-client [_ client]
  (.close client))

(comment
  (-> [{:hostname "localhost" :port 9200 :scheme "http"}]
      make-rest-client
      (exist-index "tests")
      )
  )

(comment
  (let [c (make-rest-client [{:hostname "localhost" :port 9200 :scheme "http"}])
        req (Requests/getRequest "test")]
    (.ping c empty-headers))
  )
