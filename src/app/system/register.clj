(ns app.system.register
  (:require [app.ui-page]
            [app.resource]
            [app.todo]
            [parts.ring.route]
            [parts.ring.query-endpoint]
            [parts.ring.command-endpoint]))

(defn redirect-to-todos
  [w]
  (assoc w
         :ring/response
         {:status 302
          :headers {"Location" "/ui/todos"}
          :body ""}))

(defn get-register
  []
  (concat
   app.resource/register
   app.ui-page/register
   app.todo/register
   parts.ring.route/register
   [{:ring/route [:get "/"]
     :ring/handler #'redirect-to-todos}
    {:ring/route [:get "/health"]
     :ring/handler (fn [w]
                     (assoc w
                            :ring/response
                            {:status 200
                             :body "ok"}))}
    {:ring/route [:post "/query"]
     :ring/handler #'parts.ring.query-endpoint/prepare}
    {:ring/route [:post "/command"]
     :ring/handler #'parts.ring.command-endpoint/prepare}]))
