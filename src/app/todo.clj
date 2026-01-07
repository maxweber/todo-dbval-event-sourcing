(ns app.todo
  (:require [dbval.core :as d]
            [app.part :as part]
            [app.commands.todo :as cmd]))

(defn query-todos
  [w]
  (let [todos (d/q '[:find ?e ?text ?completed
                     :where
                     [?e :todo/text ?text]
                     [?e :todo/completed ?completed]]
                   (:db/db w))]
    (assoc w
           :query/result
           {:todos (mapv (fn [[id text completed]]
                           {:id id
                            :text text
                            :completed completed})
                         todos)})))

(defn prepare
  "Prepare world-map with db connection."
  [w]
  (-> w
      (part/add-con)
      (part/add-db)))

(def register
  [{:query/kind :query/todos
    :query/fn (comp #'query-todos
                    prepare)}
   {:command/kind :command/add-todo
    :command/fn (comp (part/execute-command! #'cmd/add-todo)
                      prepare)}
   {:command/kind :command/toggle-todo
    :command/fn (comp (part/execute-command! #'cmd/toggle-todo)
                      prepare)}
   {:command/kind :command/delete-todo
    :command/fn (comp (part/execute-command! #'cmd/delete-todo)
                      prepare)}])
