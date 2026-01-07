(ns app.commands.todo
  "Todo commands - pure functions that validate and return events."
  (:require [app.events.todo :as events]
            [dbval.core :as d]
            [clojure.string :as str]))

(defn add-todo
  "Command to create a new todo.
   Pure function: reads from w, returns {:ok events} or {:error msg}."
  [w]
  (let [text (get-in w [:command :command/data :text])]
    (cond
      (or (nil? text) (str/blank? text))
      {:error "Text cannot be empty"}

      (> (count text) 500)
      {:error "Text cannot exceed 500 characters"}

      :else
      (let [id (random-uuid)]
        {:ok [(events/todo-created id (str/trim text))]
         :aggregate-id id}))))

(defn toggle-todo
  "Command to toggle a todo's completion status."
  [w]
  (let [id (get-in w [:command :command/data :id])
        db (:db/db w)
        entity (d/entity db id)]
    (cond
      (nil? entity)
      {:error "Todo not found"}

      (:todo/completed entity)
      {:ok [(events/todo-uncompleted id)]
       :aggregate-id id}

      :else
      {:ok [(events/todo-completed id)]
       :aggregate-id id})))

(defn delete-todo
  "Command to delete a todo."
  [w]
  (let [id (get-in w [:command :command/data :id])
        db (:db/db w)
        entity (d/entity db id)]
    (cond
      (nil? entity)
      {:error "Todo not found"}

      :else
      {:ok [(events/todo-deleted id)]
       :aggregate-id id})))
