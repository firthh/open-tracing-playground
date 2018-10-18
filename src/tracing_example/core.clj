(ns tracing-example.core
  (:gen-class)
  (:import io.jaegertracing.Configuration
           [io.opentracing Tracer Span Scope]))

;; Tracing

(def ^Tracer tracer (delay (.getTracer (Configuration/fromEnv))))

(def ^{:dynamic true :no-doc true} ^Span span
  nil)

(defmacro create-span [^String span-name & body]
  `(binding [^Span span (.start (.buildSpan @tracer ~span-name))]
     (with-open [^Scope scope# (.activate (.scopeManager @tracer) span true)]
       ~@body)))

;; Batch processing

(defn get-messages []
  (create-span "get-messages"
    (let [ids (range (rand-int 100))]
      (.setTag span "test" "hello")
      (doseq [id ids]
        (create-span "message-id"
          (.setTag span "message-id" (str id))))
      (Thread/sleep 1000)
      ids)))

(defn process-messages [messages]
  (create-span "process-messages"
               (.log span "Hello world")
               (run! (fn [message] (Thread/sleep (rand-int 100)) message) messages)))

(defn publish-messages [messages]
  (create-span "publish-messages"
        (Thread/sleep 500)))

;; Putting all together

(defn -main [& args]
  (prn @tracer)
  (while true
    (do
      (println "Processing batch")
      (create-span "process-batch"
            (-> (get-messages)
                (process-messages)
                (publish-messages)))
      (println "Sleeping")
      (Thread/sleep 5000))))
