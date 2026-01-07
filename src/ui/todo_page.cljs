(ns ui.todo-page
  (:require [parts.replicant.query :as query]))

(def todos-query
  {:query/kind :query/todos})

(defn fetch-todos-action []
  [[:data/query todos-query]])

(defn add-todo-action [text]
  [[:data/command
    {:command/kind :command/add-todo
     :command/data {:text text}}
    {:on-success (fetch-todos-action)}]
   [:store/dissoc :new-todo-text]])

(defn toggle-todo-action [id]
  [[:data/command
    {:command/kind :command/toggle-todo
     :command/data {:id id}}
    {:on-success (fetch-todos-action)}]])

(defn delete-todo-action [id]
  [[:data/command
    {:command/kind :command/delete-todo
     :command/data {:id id}}
    {:on-success (fetch-todos-action)}]])

(defn render-todo-item [{:keys [id text completed]}]
  [:li {:class ["flex" "items-center" "gap-3" "p-3" "bg-white" "rounded-lg" "shadow-sm"]}
   [:input {:type "checkbox"
            :checked completed
            :class ["w-5" "h-5" "text-blue-600" "rounded" "focus:ring-blue-500"]
            :on {:change (toggle-todo-action id)}}]
   [:span {:class (cond-> ["flex-1"]
                    completed (conj "line-through" "text-gray-400"))}
    text]
   [:button {:class ["px-3" "py-1" "text-sm" "text-white" "bg-red-500" "rounded" "hover:bg-red-600"]
             :on {:click (delete-todo-action id)}}
    "Delete"]])

(defn render-todo-page [state]
  (let [todos (-> (query/get-result state todos-query)
                  :todos)
        loading? (query/loading? state todos-query)
        new-todo-text (:new-todo-text state "")]
    [:div {:class ["max-w-md" "mx-auto" "mt-8" "p-6"]}
     [:h1 {:class ["text-3xl" "font-bold" "text-gray-800" "mb-6"]}
      "Todo List"]

     [:div {:class ["flex" "gap-2" "mb-6"]}
      [:input {:type "text"
               :placeholder "Add a new todo..."
               :value new-todo-text
               :class ["flex-1" "p-3" "border" "border-gray-300" "rounded-lg" "focus:outline-none" "focus:ring-2" "focus:ring-blue-500"]
               :on {:input [[:store/assoc :new-todo-text :event/target.value]]
                    :keydown (into [[:js/on-enter]]
                                   (add-todo-action new-todo-text))}}]
      [:button {:class ["px-4" "py-3" "text-white" "bg-blue-500" "rounded-lg" "hover:bg-blue-600" "disabled:bg-gray-400"]
                :disabled (empty? new-todo-text)
                :on {:click (add-todo-action new-todo-text)}}
       "Add"]]

     (if loading?
       [:p {:class ["text-gray-500"]} "Loading..."]
       (if (empty? todos)
         [:p {:class ["text-gray-500" "text-center" "py-8"]} "No todos yet. Add one above!"]
         [:ul {:class ["space-y-2"]}
          (for [todo (sort-by :text todos)]
            ^{:key (:id todo)}
            (render-todo-item todo))]))]))

(defn on-load [_location]
  (fetch-todos-action))

(def register
  [{:page-id :page/todos
    :route [["ui" "todos"]]
    :on-load #'on-load
    :render #'render-todo-page}])
