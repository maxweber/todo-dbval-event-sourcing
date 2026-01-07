(ns app.ui-page
  "Landing page for the replicant UI."
  (:require [app.part :as part]))

(defn handler
  [w]
  (-> w
      (assoc :ring/hiccup-response
             [:html.overflow-auto
              {:lang "en"}
              [:head
               (:head w)
               [:script {:src "https://cdn.tailwindcss.com"}]]
              [:body
               [:div#app.overflow-auto]
               [:script {:src "/js/ui.js"}]]])
      (part/add-hiccup-response)))

(def register
  [{:ring/route [:get "/ui/*"]
    :ring/handler #'handler}])
