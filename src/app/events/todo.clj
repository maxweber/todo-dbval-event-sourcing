(ns app.events.todo
  "Todo domain events - immutable facts about what happened."
  (:require [app.events.core :as events]))

(defn todo-created
  "Create a todo-created event."
  [id text]
  (events/make-event
   {:event/type :todo/created
    :event/aggregate :todo
    :event/aggregate-id id
    :event/data {:id id
                 :text text}}))

(defn todo-completed
  "Create a todo-completed event."
  [id]
  (events/make-event
   {:event/type :todo/completed
    :event/aggregate :todo
    :event/aggregate-id id
    :event/data {:id id}}))

(defn todo-uncompleted
  "Create a todo-uncompleted event."
  [id]
  (events/make-event
   {:event/type :todo/uncompleted
    :event/aggregate :todo
    :event/aggregate-id id
    :event/data {:id id}}))

(defn todo-deleted
  "Create a todo-deleted event."
  [id]
  (events/make-event
   {:event/type :todo/deleted
    :event/aggregate :todo
    :event/aggregate-id id
    :event/data {:id id}}))
