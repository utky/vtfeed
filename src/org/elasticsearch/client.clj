(ns org.elasticsearch.client
  (:require [integrant.core :as ig]
            [vtfeed.core.feed :as feed]
            [clojure.data.json :as json])
  (:import (org.elasticsearch.client RestClient
                                     RestHighLevelClient)
           (org.apache.http HttpHost)))

(def defualt-mapping
  {})

(defn- create-index
  [client defs]
  (let [schema  (:name defs)
        req     (.. (GetIndexRequest.) (.indices schema))
        exists  (.. client (.indices) (.exists req))]
    (when-not exists
      (let [req-idx      (CreateIndexRequest. schema)
            res-create   (.create client req-idx)
            mapping-json (-> defs
                             (dissoc :name)
                             (json/write-str))
            req-map      (.. (PutMappingRequest. schema)
                             (.source mapping-json XContentType/JSON))
            res-mapping  (.putMapping client req-map)]
        res-mpping))))

(defn- feed->index-req
  [schema feed]
  (.. (IndexRequest. schema)
      (.source (json/write-str feed) XContentType/JSON)))

(defn- create-feeds
  [client schema feeds]
  (let [each-reqs (map (partial feed->index-req schema) feeds)
        req       (reduce #(.add %1 %2) (BulkRequest.) each-reqs)]
    (.. client
        (.bulk req))))

(defn- search-feeds
  "time range query
  https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-range-query.html

  RangeQueryBuilder
  "
  [client {:keys [start end tags]}]
  (let [query  (.. (RangeQueryBuilder. "publishedAt")
                   (.from start)
                   (.to end))
        ssb    (.. (SearchSourceBuilder.) (.query query))
        req    (.. (SearchRequest.) (.source ssb))]
    (.search client req)))

(defrecord ElasticClient [client]
  feed/FeedSchema
  (create-schema-if-absent
    [this deifinitions]
    (create-index this deifinitions))
  feed/FeedLClient
  (save-feeds
    [this feeds]
    (create-feeds this feeds))
  (fetch-feeds
    [this start end & tags]
    (search-feeds this starts ends tags)))

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

(defmethod ig/init-key :org.elasticsearch.client/rest-client [_ hosts]
  (rest-client(doall (http-hosts hosts))))

(defmethod ig/halt-key! :org.elasticsearch.client/rest-client [_ client]
  (.close client))
