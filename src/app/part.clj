(ns app.part
  (:require [app.db :as db]
            [app.event-processor :as event-processor]
            [ring.util.response :as response]
            [hiccup2.core :as hiccup]))

(defn hiccup-response
  "Returns a Ring HTML response."
  [hiccup]
  (-> (hiccup/html hiccup)
      (str)
      (response/response)
      (response/content-type "text/html")
      (response/charset "UTF-8")))

(defn add-hiccup-response
  "Adds a `:ring/response` and renders `:ring/hiccup-response` as HTML."
  [w]
  (if-let [response (:ring/hiccup-response w)]
    (assoc w
           :ring/response
           (hiccup-response response))
    w))

(defn add-con
  "Adds :db/con to the world map."
  [w]
  (assoc w
         :db/con
         (db/get-conn)))

(defn add-db
  [w]
  (assoc w
         :db/db
         @(:db/con w)))

;; Event sourcing

(defn execute-command!
  "Executes a command function and processes resulting events.
   Uses the dbval connection for both event storage and read-model updates.
   Returns a function that takes w and returns updated w."
  [command-fn]
  (fn [w]
    (let [command-result (command-fn w)]
      (event-processor/process-events! w command-result))))
