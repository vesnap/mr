(ns mashup-dsl
  (:use [info.kovanovic.camelclojure.dsl]
     [clojure.test])
  (:import [org.apache.camel.component.mock MockEndpoint]
	   [org.apache.camel.component.direct DirectEndpoint]
	   [org.apache.camel ProducerTemplate]))


(defn get-received-messages [endpoint]
  (map #(.. % getIn getBody) (.getReceivedExchanges endpoint)))

(defn get-received-message [endpoint]
  (first (get-received-messages endpoint)))

(defn file-endpoint [file-name]
((str "file://" file-name)))

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

(defn send-text-message [camel endpoint message & headers]
  (publish camel endpoint (fn [[m-body m-headers]]
			    (if-not (nil? headers)
			      [message {(first headers)
					(second headers)}]
			      [message m-headers]))))


(defn stop-test [camel]
  (stop camel))

  (let [start (file-endpoint "calendar.xml")
	end   (mock "end")

	camel (create (route (from start)
				   (router (if (true)) (process ((data (en-html/xml-resource "calendar.xml"))
(en-html/select data [:events]))
							]))
				   (to end)))]
    (start-test camel start end)
    (send-text-message camel start "message")
    (let [messages (get-received-messages end)]
      (is (= (count messages) 2))
   
    (stop-test camel)))
;(defn get-data-from-feed [feed keyword]
 ; (
  ;  let[xml (events-url)
   ;    zipp (zip/xml-zip xml)
    ;   elements(-> zipp zip/down zip/children)
     ;  events (filter #(= :keyword (:tag %)) elements)]
    ;(map create-map events)
   ; ))

;normalizer
;from("direct:start")
 ;   .choice();message router
  ;      .when().xpath("/employee").to("bean:normalizer?method=employeeToPerson");message translator
   ;     .when().xpath("/customer").to("bean:normalizer?method=customerToPerson")
    ;.end()
    ;.to("mock:result");

;processor functions

;ad sam pogeldao
; i fora je da taj mock i test su moje pomoæne 
;fje za testiranje i ja takodje svaki put u svakom testu pozivam start-test 
;koji radi postavljanje tog camel context-a. 
;Fora je ova init fja koja pravi novi context i na sve endpointe ga podesava

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

(defn stop-test [camel]
  (stop camel))




;evo ga kako moj test izgleda





(deftest publish-subscribe-channel-pattern
  (let [start (direct "start")
    end1  (mock "end1")
    end2  (mock "end2")
    end3  (mock "end3")
    
    camel (create (route (from start)
                 (to :multicast
                 end1
                 end2
                 end3)))]
    (start-test camel start end1 end2 end3)

;;test body

    (stop-test camel)))
;aggregator functions for content enricher
;aggregate(old_message,new_message)
;old message is html template
;new is html template with event and artist data
 ;parsing functions

;operator data-source name, url, type, main-element

(defn send-text-message [camel endpoint message & headers]
  (publish camel endpoint (fn [[m-body m-headers]]
			    (if-not (nil? headers)
			      [message {(first headers)
					(second headers)}]
			      [message m-headers]))))
;main-element leftout, maybe it should go somewhere else
(defmacro create-data-source
[feed-name url e]
`(defn ~feed-name []((let [s# (direct ~url)
	kraj#   (mock ~e)
	camel# (create (route (from s#)
			     (to kraj#)))]
    (start camel# )))))

(defn is-message-count [endpoint count]
  (is (= (count-messages endpoint) count)))
(defn publish [camel endpoint processor-fn]
  (.. camel createProducerTemplate (send endpoint (processor processor-fn))))
(defn message-filter-pattern []
  (let [start (direct "start")
	end   (mock "end")

	camel (create (route (from start)
			     (extract #(not= (first %) "filtered"))
			     (to end)))]
    (start camel start end)
    (send-text-message camel start "filtered")
    (send-text-message camel start "text1")
    (send-text-message camel start "filtered")
    (send-text-message camel start "filtered")
    (send-text-message camel start "text2")
    (send-text-message camel start "text3")
    (send-text-message camel start "filtered")
    (is-message-count end 3)
    (stop camel)))

  ;combine, merge(with a condition) - aggregator different agg strategies

  
  ;extract- xml-zip to su neki procesori, koji element da izvuce

  
  ;filter za upite po vrednostima
  
  
;transform
  
  
  ;publish to what type
