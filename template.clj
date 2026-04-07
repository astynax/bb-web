(ns main)

(require '[org.httpkit.server :as http])
(require '[hiccup.core :as h])


(defn handler [req]
  {:status 200
   :headers {"content-type" "text/html"}
   :body (h/html {:type :html}
                 [:h1 "TEST"])})


(defonce app (atom nil))

(defn stop []
  (when @app
    (println "Stopping...")
    (@app)))

(defn start []
  (stop) ; just in case
  (println "Starting on http://localhost:8000 ...")
  (reset! app (http/run-server #'handler {:ip "localhost" :port 8000})))

#_(start)
#_(stop)

(when (= *file* (System/getProperty "babashka.file"))
  (start)
  @(promise))
