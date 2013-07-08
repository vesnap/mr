(ns datamodel

  (:use 
    [net.cgrand.enlive-html :as en-html ])
  (:require
    [clojure.zip :as z] 
  
    [clojure.xml :as xml ]
    [clojure.data.zip.xml :as zf]
     [clojure.java.io :as io]
    ))


(def data-url "http://api.eventful.com/rest/events/search?app_key=4H4Vff4PdrTGp3vV&keywords=music&location=Belgrade&date=Future")

(defn parsing [url](xml/parse url))

(defn zipp [data] (z/xml-zip data))

(defn get-content-from-tags [url & tags]
  (mapcat (comp :content z/node)
          (apply zf/xml->
                 (-> url xml/parse z/xml-zip)
	                  (for [t tags]
	                     (zf/tag= t)
                   ))))

(defn map-tags-contents [url & tags];ovo izvlaci samo jedan tag, poslednji sa contentom,(map-tags-contents data-url :events :event :title)
  (map #(hash-map % (keyword (last tags)))
      (mapcat (comp :content z/node)
          (apply zf/xml->
                 (-> url xml/parse z/xml-zip)
                  (for [t tags]
                     (zf/tag= t)
                   )))))

(defn merge-disjoint
  "Like merge, but throws with any key overlap between maps"
  ([] {})
  ([m] m)
  ([m1 m2]
     (doseq [k (keys m1)]
       (when (contains? m2 k) (throw (RuntimeException. (str "Duplicate key " k)))))
     (into m2 m1))
  ([m1 m2 & maps]
     (reduce merge-disjoint m1 (cons m2 maps))))

(defn join ;ovaj join radi za 1 red, sad treba da posaljem ceo vektor sa mapama
           [m1 m2 key1 key2]
            (when (= (m1 key1) (m2 key2)) (into  m2 m1)))
  ;and i  or za join

  
  
;za spajanje redova
  (def data (vector (into  '[] (map-tags-contents data-url :events :event :title)) (into  '[] (map-tags-contents data-url :events :event :venue_name))))

(def merged-data (map (partial apply merge) 
        (apply map vector data)))
;ovo moze i sa mapv, ali je on od 1.4 a ovaj projekat je 1.2
;(apply mapv merge data)
;mada je malo bzvz, najbolje da se izvuce nekoliko tagova u mapu
(defn merge-lists [& maps]
  (reduce (fn [m1 m2]
            (reduce 
  (fn [m pair] (let [[[k v]] (seq pair)]
                 (assoc m k (cons v (m k))))) 
  {} 
        maps))))


;(defmacro data-snippet [name url &tags];ovo sredi
 ; `(def ~name (map-tags-contents ~url ~tags)))

(def titles (map-tags-contents data-url :events :event :title));macro out of this

(def descriptions (map-tags-contents data-url :events :event :description))

(defn create-map [seq](map conj titles descriptions ));macro out of this

(defn data [url] (en-html/xml-resource url)) 

(defn add-content [url coll]
  (map :contents (z/xml-zip (xml/parse url)) coll))

;import data source - connect to data source, 

;parsing and data mapping

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
;for now mapping will be manual
;(def a_mapping {"att1" :att1 "att2" :att2 "att3" :att3...})
;using fns - keys
;and (into {} (map (juxt identity name) [:a :b :c :d]))
(defn static? [field]
  (java.lang.reflect.Modifier/isStatic
   (.getModifiers field)))

(defn get-record-field-names [record]
    (->> record
        .getDeclaredFieldsto
       (remove static?)
     (map #(.getName %))
     (remove #{"__meta" "__extmap"})))

(defmacro empty-record [record]
  (let [klass (Class/forName (name record))
        field-count (count (get-record-field-names klass))]
    `(new ~klass ~@(repeat field-count nil))))


;dodavanje podataka u mapu
;http://jakemccrary.com/blog/2010/06/06/inserting-values-into-a-nested-map-in-clojure/
;user> (-> (add-to-cache {} :chicago :lakeview :jake)
 ;         (add-to-cache :sf :mission :dan)
  ;        (add-to-cache :chicago :wickerpark :alex))
;{:sf {:mission :dan}, :chicago {:wickerpark :alex, :lakeview :jake}}

(defn add-to-cache [cache key1 key2 data]
  (assoc-in cache [key1 key2] data))



;model to html - enlive in templating

(defn to-keys [& args]
  (for [k args] (map #(keyword %) k)))

;(defn parsing [xz tags-to-pull tags-start]
 ; (for [tagg to-keys tags-to-pull](map (juxt #(zf/xml1-> % tagg text))(zf/xml1-> xz tags-start))
  ;))

;ovo refakorisati tako da zf/xml1-> radi sa jednim po jednim key-em iz rekorda
;trebace defmacro za ovo

;(map #((apply comp (reverse func)) %) tag-collection)


(defn xz [url] (z/xml-zip (xml/parse url)))
(defn xml-zipper [& tags](zf/xml-> (xz data-url) tags))
(def func [#(zf/xml1-> (xml-zipper %1) %2 zf/text)])

;ovo vadi iz razlicitih tagova podatke
(reduce (fn [h item] 
                         (assoc h (zf/xml1-> item :title zf/text) 
                                  (zf/xml1-> item :venue_name zf/text))) 
                       {} (zf/xml-> (xz data-url) :events :event))



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
(defstruct event :event-name :performers :start-time :stop-time)

(defn get-text-from-tag [zipp tag] (zf/xml1-> zipp tag zf/text))

(defn zipped-data [xz &tags] (zf/xml-> xz &tags))
;prvo treba ovo (def root :events :event)
;hocu da mogu ovo da napisem (get-events url root source :title :name start_time :stop_time)
;i iz njega da izadje ovo dole
(defmacro getting-data [fn-name url root &tags] 
  `(do
     (defn ~fn-name [url root &tags] 
       (map (juxt (get-text-from-tag %)) (zip/xml-zip (xml/parse url)))
       )))
(defn do-to-map [amap keyseq f]
  (reduce #(assoc %1 %2 (f (%1 %2))) amap keyseq))

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

; (defn create-map-of-artists-lastfm  []
 ; (map #(apply struct artist-lastfm %) (lastFmToArtist (z/xml-zip (xml/parse "http://ws.audioscrobbler.com/2.0/?method=artist.getinfo&artist=Cher&api_key=b25b959554ed76058ac220b7b2e0a026")))))
 
; (defn create-map-of-artists-musicbrainz  []
  
 ;  (map #(apply struct artist-musicbrainz %) (musicBrainzToArtist (z/xml-zip (xml/parse "http://www.musicbrainz.org/ws/2/artist/?query=artist:cher")))))
 
;(defn events-for-mashup []
 ; (let [title "Events mashup" event-data (vector (create-map-of-events))] 
  ;  (apply struct event-map title event-data)))
 

(defn get-performers []
 ( doseq [event (create-map-of-events)] 
   (let [performer (get event :start-time)]
     (case performer (not(nil?)) println performer))))  

 
;_exchange.getOut().setBody(createEarthquake(title.substring(7), date, title.substring(2,5), latitude, longitude, depth, area))
