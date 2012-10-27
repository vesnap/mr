(ns datamodel
(:import (java.io ByteArrayInputStream))
  (:use 
    [net.cgrand.enlive-html :as en-html ])
  (:require
    [clojure.zip :as z] 
    [clojure.xml :as xml ]
    [clojure.contrib.zip-filter.xml :as zf]
    ))

 ;  [clojure.xml :as xml ]
  ;          
   ;         [clojure.contrib.zip-filter.xml :as zf]
    ;        [apicalls]
     ;       [clojure.java.io]
      ;      [clojure.contrib.str-utils] ))
 ;[clojure.data.zip.xml :only (attr text xml->)]

(def eventful-events "http://api.eventful.com/rest/events/search?app_key=4H4Vff4PdrTGp3vV&keywords=music&location=Belgrade&date=Future")

;internal data model - mapa sa vektorom za content, u content se stavlja ono sto se dobije iz zip str
(defmacro define-source [source-name title-value link-value updated-value summary-value]
  '(def source-name (hash-map :title title-value, :link link-value, :updated updated-value, :summary summary-value, :content {}) ))

(defmacro create-map
  [& syms]
  (zipmap (map keyword syms) syms))


(defn add-source [source-name title-value link-value updated-value summary-value]
  (let [title title-value  link link-value updated updated-value summary summary-value] (create-map title link updated summary)))


;for the content part
(def data-url "http://api.eventful.com/rest/events/search?app_key=4H4Vff4PdrTGp3vV&keywords=music&location=Belgrade&date=Future")
(def events-data (xml/parse data-url))
(defn zipp [data] (zip/xml-zip data))
(defn contents[] (xml-> (zipp data-url) :events :event))
(defn get-struct-map [xml]
  (if-not (empty? xml)
    (let [stream (ByteArrayInputStream. (.getBytes (.trim xml)))]
      (xml/parse stream))))


(map (fn [elt] (or (:tag elt) elt)) (xml-seq events-data))

(defn get-value [xml & tags]
  (apply zf/xml1-> (zip/xml-zip (get-struct-map xml)) (conj (vec tags) zf/text)))

(defn data [url](en-html/xml-resource url))

(defn select-data[] (en-html/select data [:events]))
; pulls out a list of all of the root att attribute values
(defn values [xml & tags]

  (apply zf/xml1-> (zip/xml-zip (get-struct-map xml)) (conj (vec tags) zf/text)));umesto ovog conj nesto drugo


(defn neka-fn[]
  (map
  (comp z/node z/up)
  (@#'en-html/zip-select-nodes*
   (map z/xml-zip (en-html/xml-resource "calendar.xml"))
     [:holiday])))

(def data (en-html/xml-resource data-url))
(html/select data [:events])



; gets the "column-value" pair for a single column
(defn column-value [z](zf/xml1-> z
           (zf/attr= :Id "cdx9") ; filter on id "cdx9" 
           :XVar ; filter on XVars under it 
           (zf/attr= :Id "TrancheAnalysis.IndexDuration") ; filter on id
           value)) ; apply the value function on the result of above

; creates a map of every column key to it's corresponding value
(apply merge (zf/xml-> zipp (zf/attr= :Id "cdx9") :XVar value))


(defn add-content [url coll]
  (map :contents (zip/xml-zip (xml/parse url)) coll))

;import data source - connect to data source, 

;parsing and data mapping

;structs
(defstruct event :event-name :performers :start-time :end-time)
(defstruct event-map  :title  :event-data)

(defstruct artist-lastfm :name :mbid :url :summary)

(defstruct artist-musicbrainz :gender :country :life-span)

(defstruct tag-list :tag :name :url)
(defstruct venue :id :name :location)
(defstruct location :lat :long :name)
(defstruct image :url :width :height :thumb)
(defstruct category :id)
(def events-url "http://api.eventful.com/rest/events/search?app_key=4H4Vff4PdrTGp3vV&keywords=music&location=Belgrade&date=Future")


 (defn musicBrainzToArtist[xz]
  "Artists from musicBrainz transfeevered to struct from zipper tree made of feed output"
  (map (juxt 
        ;#(zf/xml1-> % :name text)  
        #(zf/xml1-> % :gender text) 
         #(zf/xml1-> % :country text)
         #(zf/xml1-> % :life-span :begin text)
         )
     (zf/xml-> xz :artist-list :artist))
  )

(defn lastFmToArtist[xz]
  "Artists from last.fm transfered to struct from zipper tree made of feed output"
  (map (juxt 
        #(zf/xml1-> % :name text)  
        #(zf/xml1-> % :mbid text) 
         #(zf/xml1-> % :url text)
         #(zf/xml1-> % :bio :summary text)
         )
     (zf/xml-> xz :artist))
  )

(defn get-events
  [xz] 
  (map (juxt 
        #(zf/xml1-> % :title text) 
        #(zf/xml1-> % :performers :performer :name text) 
        #(zf/xml1-> % :start_time text) 
         #(zf/xml1-> % :end_time text)
         )
     (zf/xml-> xz :events :event)))

 (defn create-map-of-events []
   (map #(apply struct event %) (get-events (zip/xml-zip (xml/parse "http://api.eventful.com/rest/events/search?app_key=4H4Vff4PdrTGp3vV&keywords=music&location=Belgrade&date=Future")))))

 (defn create-map-of-artists-lastfm  []
  (map #(apply struct artist-lastfm %) (lastFmToArtist (zip/xml-zip (xml/parse "http://ws.audioscrobbler.com/2.0/?method=artist.getinfo&artist=Cher&api_key=b25b959554ed76058ac220b7b2e0a026")))))
 
 (defn create-map-of-artists-musicbrainz  []
  
   (map #(apply struct artist-musicbrainz %) (musicBrainzToArtist (zip/xml-zip (xml/parse "http://www.musicbrainz.org/ws/2/artist/?query=artist:cher")))))
 
 


(defn events-for-mashup []
  (let [title "Events mashup" event-data (vector (create-map-of-events))] 
    (apply struct event-map title event-data)))
 

(defn get-performers []
 ( doseq [event (create-map-of-events)] 
   (let [performer (get event :start-time)]
     (case performer (not(nil?)) println performer))))  
 
 
 
(def events (xml/parse "http://api.eventful.com/rest/events/search?app_key=4H4Vff4PdrTGp3vV&keywords=music&location=Belgrade&date=Future"))

 (def zipped (zip/xml-zip events))

;_exchange.getOut().setBody(createEarthquake(title.substring(7), date, title.substring(2,5), latitude, longitude, depth, area))
