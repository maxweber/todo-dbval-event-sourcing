(ns app.db
  "Simple database connection management."
  (:require [dbval.core :as d]
            [app.db-schema :as schema]))

(defonce connection
  (atom nil))

(def db-file "data/db.db")

(defn get-conn
  "Gets or creates the database connection."
  []
  (if-let [c @connection]
    c
    (let [c (d/create-conn schema/schema
                           {:db-file db-file})]
      (reset! connection c)
      c)))
