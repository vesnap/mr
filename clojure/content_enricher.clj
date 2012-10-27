(ns content-enricher

  (:use [clojure.test]
        [info.kovanovic.camelclojure.dsl]
    ;[net.cgrand.enlive-html :as en-html]
    [test-utils]
    [datamodel]
	;[info.kovanovic.camelclojure.test-util]
)
(:import [org.apache.camel.component.mock MockEndpoint]
	   [org.apache.camel.component.direct DirectEndpoint]
	   [org.apache.camel ProducerTemplate]
    [org.apache.camel.component.file FileEndpoint]
    [org.apache.camel.component.file FileComponent])
)

(defn enrich[ex message url &key-data]
;(message is map with starting data, data is list of attributes for data that we want to be added)
((if not (nil? ex)
(let [mess (.. message getIn getBody)]
merge-with union mess (data url))
)))


(deftest content-enricher-pattern
  (let [start (file-comp "d:/calendar.xml")
	end   (mock "end")
 url "http://api.eventful.com/rest/events/search?app_key=4H4Vff4PdrTGp3vV&keywords=music&location=Belgrade&date=Future"
 enriching-with-data (enrich start start url start (data url))
	camel (create (route (from start)
enriching-with-data
			     (to end)))]
    (start-test camel start enriching-with-data end)
    (stop-test camel)))


