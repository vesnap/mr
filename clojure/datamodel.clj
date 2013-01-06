(ns datamodel
(:import (java.io ByteArrayInputStream))
  (:use 
    [net.cgrand.enlive-html :as en-html ])
  (:require
    [clojure.zip :as z] 
    [clojure.data.zip.xml :only (attr text xml->)]
    [clojure.xml :as xml ]
    [clojure.contrib.zip-filter.xml :as zf]
    ))

       
   ;         [clojure.contrib.zip-filter.xml :as zf]
    ;        [apicalls]
     ;       [clojure.java.io]
      ;      [clojure.contrib.str-utils] ))
 


;internal data model - mapa sa vektorom za content, u content se stavlja ono sto se dobije iz zip str
;hocu sad ovde ovo da ponovim za svaki tag koji hocu i da spojim dole u mapu
(defn list-of-contents [the-key] (for [x (xml-seq (xml/parse data-url)) :when (= the-key (:tag x))] (first (:content x))))

(defn create-a-map [the-key](zipmap (list-of-contents the-key) (repeat the-key)))

;ovo radi ali ne znam posle kako da ga koristim
(defmacro defmodel [name & field-spec]
  `(do (defstruct ~name ~@(take-nth 2 field-spec))
       (def ~(symbol (str "*" name "-meta*"))
         (reduce #(assoc %1 (first %2) (last %2))
                 {}
                 (partition 2 '~field-spec)))))
;for the content part
(def data-url "http://api.eventful.com/rest/events/search?app_key=4H4Vff4PdrTGp3vV&keywords=music&location=Belgrade&date=Future")


(defn parsing [url](xml/parse url))

(defn zipp [data] (z/xml-zip data))

(defn contents[cont] 
  (zf/xml-> cont :events :event :title))

(defn data [url] (en-html/xml-resource url))

(defn select-data[url] (en-html/select (data url) [:events]))
; pulls out a list of all of the root att attribute values


(defmacro map-cols [seq & columns] (vec (`(hash-map #(nth % ~columns nil) ~seq))))
(defn data [](en-html/xml-resource data-url));vektor sa svim
;(en-html/select data [:events]);mape tih tagova
;select value od :tag bude kao tag u toj mapi a select value od :value bude value
;od ovih mapa hocu da napravim mapu :tag val :content :val



; creates a map of every column key to it's corresponding value
;(apply merge (zf/xml-> zipp (zf/attr= :Id "cdx9") :XVar value))


(defn add-content [url coll]
  (map :contents (z/xml-zip (xml/parse url)) coll))

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
  "Artists from musicBrainz transfered to struct from zipper tree made of feed output"
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
         #(zf/xml1-> % :end_time text))
     (zf/xml-> xz :events :event)))

 (defn create-map-of-events []
   (map #(apply struct event %) (get-events (z/xml-zip (xml/parse "http://api.eventful.com/rest/events/search?app_key=4H4Vff4PdrTGp3vV&keywords=music&location=Belgrade&date=Future")))))

 (defn create-map-of-artists-lastfm  []
  (map #(apply struct artist-lastfm %) (lastFmToArtist (z/xml-zip (xml/parse "http://ws.audioscrobbler.com/2.0/?method=artist.getinfo&artist=Cher&api_key=b25b959554ed76058ac220b7b2e0a026")))))
 
 (defn create-map-of-artists-musicbrainz  []
  
   (map #(apply struct artist-musicbrainz %) (musicBrainzToArtist (z/xml-zip (xml/parse "http://www.musicbrainz.org/ws/2/artist/?query=artist:cher")))))
 
 


(defn events-for-mashup []
  (let [title "Events mashup" event-data (vector (create-map-of-events))] 
    (apply struct event-map title event-data)))
 

(defn get-performers []
 ( doseq [event (create-map-of-events)] 
   (let [performer (get event :start-time)]
     (case performer (not(nil?)) println performer))))  

 
;_exchange.getOut().setBody(createEarthquake(title.substring(7), date, title.substring(2,5), latitude, longitude, depth, area))
