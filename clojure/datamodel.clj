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


;internal data model - preko defentity, korisnik definise pa se od keysa napravi mapa preko koje se izvlace podaci
;
;ovo radi ali ne znam posle kako da ga koristim

;for the content part
(def data-url "http://api.eventful.com/rest/events/search?app_key=4H4Vff4PdrTGp3vV&keywords=music&location=Belgrade&date=Future")


(defn parsing [url](xml/parse url))

(defn zipp [data] (z/xml-zip data))

(defn contents[cont] 
  (zf/xml-> cont :events :event :title))

(defn data [url] (en-html/xml-resource url))

(defn select-data[url] (en-html/select (data url) [:events]))
; pulls out a list of all of the root att attribute values



(defn data [](en-html/xml-resource data-url));vektor sa svim
;(en-html/select data [:events]);mape tih tagova
;select value od :tag bude kao tag u toj mapi a select value od :value bude value
;od ovih mapa hocu da napravim mapu :tag val :content :val


(defn add-content [url coll]
  (map :contents (z/xml-zip (xml/parse url)) coll))

;import data source - connect to data source, 

;parsing and data mapping

;structs u defrecorde
(defstruct event :event-name :performers :start-time :stop-time)
(defstruct event-map  :title  :event-data)

(defstruct artist-lastfm :name :mbid :url :summary)

(defstruct artist-musicbrainz :gender :country :life-span)

(defstruct tag-list :tag :name :url)
(defstruct venue :id :name :location)
(defstruct location :lat :long :name)
(defstruct image :url :width :height :thumb)
(defstruct category :id)

(defmacro defentity [name & values]
  `(defrecord ~name [~@values]))
(def apis (defentity api-name api-url api-format))
;all structs to records
(defrecord event [event-name performers start-time stop-time])
(def events-url "http://api.eventful.com/rest/events/search?app_key=4H4Vff4PdrTGp3vV&keywords=music&location=Belgrade&date=Future")

(defn to-keys [& args]
  (for [k args] (vector (map #(keyword %) k))))


(defn parsing [xz tags-to-pull tags-start]
  (for [tagg to-keys(tags-to-pull)](map (juxt #(zf/xml1-> % tagg text))(zf/xml-> xz tags-start))
  ))

;ovo refakorisati tako da zf/xml1-> radi sa jednim pojednim key-em iz rekorda
;trebace defmacro za ovo
 (defn musicBrainzToArtist[xz]
  "Artists from musicBrainz transfered to struct from zipper tree made of feed output"
  (map (juxt 
        ;#(zf/xml1-> % :name )  
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
        #(zf/xml1-> % :title zf/text) 
        #(zf/xml1-> % :performers :performer :name zf/text) 
        #(zf/xml1-> % :start_time zf/text) 
         #(zf/xml1-> % :stop_time zf/text))
     (zf/xml-> xz  :events :event)))

;ovo probaj da prebacis da bude sa obicnom mapom
 (defn create-map-of-events [event]
   (map #(apply struct event %)(get-events (z/xml-zip (xml/parse "http://api.eventful.com/rest/events/search?app_key=4H4Vff4PdrTGp3vV&keywords=music&location=Belgrade&date=Future")))))

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
