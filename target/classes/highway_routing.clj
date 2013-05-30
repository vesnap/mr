(ns highway-routing
(:use [info.kovanovic.camelclojure.dsl])
(:use [templating]))

;normalizer, content enricher i dynamic router

;from api-calls to-?, ne mogu da provalim gde da saljem koja komponenta 

;processori koji ukljucuju funkcije iz datamodela i templatinga

;1.camel context
;2. doda se ruta from, to
;3. addComponent
;4. start the context
;stop the context

(let [get-data (route (from "http://api.eventful.com/rest/events/search?app_key=4H4Vff4PdrTGp3vV&keywords=music&location=Belgrade&date=Future")
(to "jms:api_results"))]
start[(create get-data)])





;(defn create [& routes]
 ; (co/create-camel routes))
 
 ;Exchange createMashup{Exchange exchange =context.getEndpoint("direct:a").createExchange(); 
 
 ;<message msg=exchange.getIn();msg.setParametre....; return msg;}

 ;funkcija koja izvlaci podatke iz feeda
 ;(route (eventful-events)
;(process #(identity [(upper-case (first %))
;(second %)]))
;(to "jms:example2"))
 
 ;integer-processing (route (from "jms://process-numbers-input")
;(process times-ten)
;(to "jms://combine"))
 ;; procesiranje String poruka
;string-processing (route (from "jms://process-strings-input")
;(process to-upper-case)
;(to "jms://combine-input"))

 ;; Nakon procesiranja spoji jednu Integer i jednu String poruku
;(aggregate (route (from "jms://combine-input")
 ;(aggregator join-bodies "messageId" :count 2)
 ;(to "jms://final-output"))]
 ;(create split-and-route
;integer-processing
 ;string-processing
; aggregate))
 



;za normalizer
;from("url")
   ; .choice() ;da li postoji ovaj choice
    ;    .when().xpath("/employee").to("bean:normalizer?method=employeeToPerson");kako when jel sa defmulti, fja koja prebacuje pod u struct
        ;.when().xpath("/customer").to("bean:normalizer?method=customerToPerson")
   ; .end()
   ; .to("mock:result");gde da ga prebacim
   
   
   ;za content enricher

;new AggregationStrategy() {
;public Exchange aggregate(Exchange oldExchange,
;Exchange newExchange) {
;if (newExchange == null) {
;return oldExchange;
;}
;String http = oldExchange.getIn()
;.getBody(String.class);
;String ftp = newExchange.getIn()
;.getBody(String.class);
;String body = http + "\n" + ftp;
;oldExchange.getIn().setBody(body);
;return oldExchange;}})
;.to("file://riders/orders");
   