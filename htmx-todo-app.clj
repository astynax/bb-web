(ns todo)

(require '[org.httpkit.server :as http])
(require '[hiccup.core :as h])
(import '[java.net URLDecoder])

(defonce db (atom {:seq 1
                   :items {}}))

(defn add-item! [title]
  (swap! db (fn [{s :seq :as m}]
              (-> m
                  (update :seq inc)
                  (update :items
                          assoc s {:title title
                                   :done false})))))

(defn remove-item! [id]
  (swap! db update :items dissoc id))

(defn toggle-item! [id]
  (-> db
      (swap! update :items
             (fn [m]
               (if (m id)
                 (update-in m [id :done] not)
                 m)))
      (get-in [:items id])))

#_(do (reset! db {:seq 1 :items {}})
      (add-item! "Do some stuff")
      (add-item! "Buy milk")
      (toggle-item! 1))

(defn ok-html [& tags]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body
   (h/html tags)})

(defn -read-all [reader]
  (lazy-seq
   (when (pos? (.available reader))
     (cons (char (.read reader))
           (-read-all reader)))))

(defn params [body]
  (reduce
   (fn [m line]
     (clojure.core.match/match
      (clojure.string/split line #"=" 2)
      [k ""] m
      [k v] (assoc m (keyword k)
                   (java.net.URLDecoder/decode v "UTF-8"))
      :else m))
   {} (clojure.string/split-lines body)))

#_(params "foo=\nbar=baz\nbaz=\nqux=doo")
;; => {:bar "baz", :qux "doo"}

(defn cb-done [id done]
  [:input {:type :checkbox
           :checked done
           :hx-trigger "click"
           :hx-swap "this"
           :hx-put (str "/" id)}])

(defn items []
  [:tbody#todos {:hx-swap-oob "true"}
   (for [[id item] (sort-by first (:items @db))]
     [:tr {:scope "row" :id id}
      [:td (cb-done id (item :done))]
      [:td (item :title)]
      [:td [:button
            {:hx-delete (str "/" id)
             :hx-target "closest #todos"
             :hx-swap "outerHTML"}
            "delete"]]
      ])])

(defn input []
  [:div#new-todo
   [:input {:name "title"}]
   [:button
    {:hx-post "/"
     :hx-include "previous input"
     :hx-target "closest #new-todo"}
    "Add"]])

(defn with-id [uri callback]
  (if-let [id (parse-long (subs uri 1))]
    (callback id)
    {:status 400}))

(defn handler [{:keys [request-method uri body]}]
  (case request-method
    :get
    (ok-html
     [:html {:lang "EN"}
      [:head
       [:meta {:charset  "utf-8"}]
       [:meta {:name "viewport"
               :content "width=device-width, initial-scale=1"}]
       [:script {:src "https://unpkg.com/htmx.org@2.0.3"}]
       [:link {:rel "stylesheet"
               :href "https://cdn.jsdelivr.net/npm/@picocss/pico@2.0.6/css/pico.min.css"}]]
      [:body
       [:main.container
        [:section
         [:h2 "Todos"]
         [:table
          [:thead
           [:th {:scope "col" :style "width: 1%;"} "Done"]
           [:th {:scope "col"} "Task"]
           [:th {:scope "col" :style "width: 1%;"} "Action"]]
          (items)]]
        [:section
         [:h2 "Add todo"]
         (input)]
        ]]])

    :delete
    (with-id uri
      (fn [id]
        (remove-item! id)
        (ok-html (items))))

    :put
    (with-id uri
      (fn [id]
        (-> (toggle-item! id)
            :done
            (->> (cb-done id))
            ok-html)))

    :post
    (if-let [title (-> body
                       -read-all
                       clojure.string/join
                       params
                       :title)]
      (do (add-item! title)
          (ok-html
           (items)
           (input)))
      {:status 400})

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
