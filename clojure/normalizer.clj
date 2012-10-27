
(ns normalizer
  (:use [clojure.test]
        [info.kovanovic.camelclojure.dsl]
    [net.cgrand.enlive-html :as en-html]
    ;[datamodel]
	;[info.kovanovic.camelclojure.test-util]
)
(:import [org.apache.camel.component.mock MockEndpoint]
	   [org.apache.camel.component.direct DirectEndpoint]
	   [org.apache.camel ProducerTemplate]
    [org.apache.camel.component.file FileEndpoint]
    [org.apache.camel.component.file FileComponent]))

(defn file-comp[file-name]
  (FileEndpoint. (str "file://inbox[filename=" file-name "]") (FileComponent. ))) ;+fileName=thefilename.

(defn mock [url]
  (MockEndpoint. (str "mock://" url)))

(defn direct [url]
  (DirectEndpoint. (str "direct://" url)))

(defn- init [context endpoints]
  (if-not (empty? endpoints)
    (do (.setCamelContext (first endpoints) context)
    (recur context (rest endpoints)))))

(defn start-test [camel & endpoints]
  (init camel endpoints)
  (start camel))
(defn add-header[endpoint header-name]
  (.setHeaderName header-name) endpoint)
(defn stop-test [camel]
  (stop camel))
(defn data [url](en-html/select(en-html/xml-resource url) [:calendar]))
(defn get-received-messages [endpoint]
  (map #(.. % getIn getBody) (.getReceivedExchanges endpoint)))

(defn get-received-message [endpoint]
  (first (get-received-messages endpoint)))

(defn count-messages [endpoint]
  (.getReceivedCounter endpoint))

(defn publish [camel endpoint processor-fn]
  (.. camel createProducerTemplate (send endpoint (processor processor-fn))))
(defn send-text-message [camel endpoint message & headers]
  (publish camel endpoint (fn [[m-body m-headers]]
			    (if-not (nil? headers)
			      [message {(first headers)
					(second headers)}]
			      [message m-headers]))))

(defn get-filename[fileendpoint]
  (.getDoneFileName fileendpoint))

(defn count-messages [endpoint]
  (.getReceivedCounter endpoint))
(defn is-message-count [endpoint count]
  (is (= (count-messages endpoint) count)))
;normalizer is combination of router and transformer
(deftest normalizer-pattern
  
  (let [start (file-comp "d:/calendar.xml")
       name-equals #(fn [[f-name _]]
			(= f-name %))
       end   (mock "end")			
    xml-processing 
				  (processor #(en-html/select(en-html/xml-resource (data (.getEndpointUri start))) [:calendar]))
				  
       routing (route (from file-comp)
               (router (name-equals "calendar.xml") xml-processing)
          end)

	camel (create (routing
                 xml-processing))]
    (start-test camel start end)
 
    (is-message-count end 1)
    (stop-test camel)))
