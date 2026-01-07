(ns app.projections.todo
  "Projections for todo events - transform events to dbval transactions.")

(defmulti project
  "Project an event to dbval transaction data."
  (fn [event] (:event/type event)))

(defmethod project :todo/created
  [event]
  (let [{:keys [id text]} (:event/data event)]
    [[:db/add id :todo/text text]
     [:db/add id :todo/completed false]]))

(defmethod project :todo/completed
  [event]
  (let [{:keys [id]} (:event/data event)]
    [[:db/add id :todo/completed true]]))

(defmethod project :todo/uncompleted
  [event]
  (let [{:keys [id]} (:event/data event)]
    [[:db/add id :todo/completed false]]))

(defmethod project :todo/deleted
  [event]
  (let [{:keys [id]} (:event/data event)]
    [[:db/retractEntity id]]))

(defmethod project :default
  [event]
  (println "Unknown event type for todo projection:" (:event/type event))
  [])
