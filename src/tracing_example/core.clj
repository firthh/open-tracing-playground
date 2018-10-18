(ns tracing-example.core
  (:gen-class)
  (:import io.jaegertracing.Configuration
           [io.opentracing Tracer Span Scope]))

;; Tracing

(def ^Tracer tracer (delay (.getTracer (Configuration/fromEnv))))

(defmacro span [^String span-name & body]
  `(with-open [^Scope scope# (.startActive (.buildSpan @tracer ~span-name) true)]
     ~@body))

;; Batch processing

(defn get-messages []
  (span "get-messages"
        (Thread/sleep 1000)
        (range (rand-int 100))))

(defn process-messages [messages]
  (span "process-messages"
        (run! (fn [message] (Thread/sleep (rand-int 100)) message) messages)))

(defn publish-messages [messages]
  (span "publish-messages"
        (Thread/sleep 500)))

;; Putting all together

(defn -main [& args]
  (prn @tracer)
  (while true
    (do
      (println "Processing batch")
      (span "process-batch"
            (-> (get-messages)
                (process-messages)
                (publish-messages)))
      (println "Sleeping")
      (Thread/sleep 5000))))
