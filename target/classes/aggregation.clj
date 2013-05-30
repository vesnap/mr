(ns aggregation
  (:use [clojure.test]
        [info.kovanovic.camelclojure.dsl]
	[test-utils]
 [templating]))
  
(deftest aggregator-pattern
  (let [start (file-comp "calendar.xml")
	end  (mock "mashups")
	f (fn [[body1 headers1] [body2 headers2]]
	    (identity [(indeks body1 (body2))
		       headers1]))

	r (route (from start)
		 (aggregator f "type" :count 2)
		 (to end))
	camel (create r)]
    (start-test camel start end)
     
    (let [messages (get-received-messages end)]
      (is (= (count messages) 2)))
    (stop-test camel)))


