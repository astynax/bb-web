(ns main)

(require '[org.httpkit.server :as http])
(require '[hiccup.core :as h])

(defn handler [{:keys [request-method uri]}]
  (case [request-method uri]
    [:get "/"]
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body
     (h/html
      [:html {:lang "EN"}
       [:head
        [:script {:src "https://cdn.jsdelivr.net/npm/scittle@0.6.17/dist/scittle.js"}]
        [:link {:rel "stylesheet"
                :href "https://cdn.jsdelivr.net/npm/@picocss/pico@2.0.6/css/pico.min.css"}]]
       [:body
        [:main.container
         [:h1 "Hello from Clojure!"]
         [:h2#clock]
         [:progress#progress {:value 0 :max 59}]
         ]
        [:script {:type "application/x-scittle"} "

(def tag (.querySelector js/document \"#clock\"))
(def prog (.querySelector js/document \"#progress\"))

(defn update []
  (let [d (js/Date.)
        t (.toLocaleTimeString d)
        s (.getSeconds d)]
    (set! (.-innerText tag) t)
    (.setAttribute prog \"value\" (str s))))

(update)

(.setInterval js/window update 1000)

"]]])
     }

    {:status 404
     :body "Page not found"}
    ))

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
