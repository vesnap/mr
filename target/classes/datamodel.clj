(ns datamodel

  (:use 
    [net.cgrand.enlive-html :as en-html ])
  (:require
    [clojure.zip :as z] 
    [clojure.data.zip.xml :only (attr text xml->) :as xz]
    [clojure.xml :as xml ]
    [clojure.data.zip.xml :as zf]
     [clojure.java.io :as io]
    ))


;internal data model - preko defentity, korisnik definise pa se od keysa napravi mapa preko koje se izvlace podaci
;
;ovo radi ali ne znam posle kako da ga koristim

;for the content part
(def data-url "http://api.eventful.com/rest/events/search?app_key=4H4Vff4PdrTGp3vV&keywords=music&location=Belgrade&date=Future")


(defn parsing [url](xml/parse url))

(defn zipp [data] (z/xml-zip data))

;(defn loc-preds[& tags] 
  ;result should be
  ;[(xz/tag= :events) (xz/tag= :event) (xz/tag= :title)]
 ; (for [t tags] [zf/tag= (key (str(symbol t)))])
  ;)

;(def loc-preds [(xz/tag= :events) (xz/tag= :event) (xz/tag= :title)])

(defn get-content-from-tags [url & tags]
  (mapcat (comp :content z/node)
          (apply xz/xml->
                 (-> url xml/parse z/xml-zip)
                  (for [t tags]
                     (zf/tag= t)
                   ))))

(defn map-tags-contents [url & tags]
  (map #(hash-map % (keyword (last tags)))
      (mapcat (comp :content z/node)
          (apply xz/xml->
                 (-> url xml/parse z/xml-zip)
                  (for [t tags]
                     (zf/tag= t)
                   )))))


(defn merge-lists [& maps]
  (reduce (fn [m1 m2]
            (reduce 
  (fn [m pair] (let [[[k v]] (seq pair)]
                 (assoc m k (cons v (m k))))) 
  {} 
        maps))))

(def titles (map-tags-contents data-url :events :event :title))
(def descriptions (map-tags-contents data-url :events :event :description))

(defn data [url] (en-html/xml-resource url))

(defn create-map [](map conj titles descriptions ))

;(defn select-data[url & tags] 
 ; ((let [v (vec-of-tags tags)]
  ;  en-html/select (data url) v)))

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

;----------!!!!!!!!!!!!!---------------
;----------!G!L!A!V!N!O!---------------
;----------!!!!!!!!!!!!!---------------

;for defining entities in the model of the mashup
;all structs to records
(defmacro defentity [name & values]
  `(defrecord ~name [~@values]))
;this fn call has to go in some kind of mapping on vector made of xml data

(def apis (defentity api-name api-url api-format))

;(defrecord event [event-name performers start-time stop-time])
(def events-url "http://api.eventful.com/rest/events/search?app_key=4H4Vff4PdrTGp3vV&keywords=music&location=Belgrade&date=Future")


;for mapping data from the apis to the model
;we need a macro that will do this
;(def a_mapping {"att1" :att1 "att2" :att2 "att3" :att3...})
;using fns - keys
;and (into {} (map (juxt identity name) [:a :b :c :d]))
(defn get-record-field-names [record]
    (->> record
        .getDeclaredFields
       (remove static?)
     (map #(.getName %))
     (remove #{"__meta" "__extmap"})))
(defmacro empty-record [record]
  (let [klass (Class/forName (name record))
        field-count (count (get-record-field-names klass))]
    `(new ~klass ~@(repeat field-count nil))))
(defmacro mapping [name API_data rec] 
  `(defn ~name [~@API_data ~@entity] 
     (into {} (map (juxt (keys ~@API_data) `( get-record-field-names ~@rec))))))

;schema matching, define attributes that are used for connecting 2 entities
(defn eqauls);to bi ili trebao da bude neki meultimetod ili sl
(defn relationship [ent1 ent2 att1 att2] ())

;model to html - enlive in templating




(defn to-keys [& args]
  (for [k args] (vector (map #(keyword %) k))))


;(defn parsing [xz tags-to-pull tags-start]
 ; (for [tagg to-keys tags-to-pull](map (juxt #(zf/xml1-> % tagg text))(zf/xml1-> xz tags-start))
  ;))

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
