(ns ui.core
  (:require [ui.register]
            [parts.replicant.core]
            [parts.replicant.router :as router]
            [replicant.dom :as r]
            ))

(defonce system
  (atom nil))

(defonce el
  (js/document.getElementById "app"))

(defn add-get-register
  [w]
  (assoc w
         :ui/get-register
         (fn []
           ui.register/register)))

(defn get-render-f [{:keys [ui/state] :as w}]
  (:render
   (router/get-page-by-id
    w
    (-> state :location :location/page-id))
   ))

(defn not-found-page
  [_state]
  [:h1 "Not found"])

(defn render! [{:keys [ui/state ui/routes] :as w}]
  (let [f (or (get-render-f w)
              not-found-page)]
    (r/render el
              [:div
               (f state)]
              {:alias-data {:routes routes}})))

(defn add-render
  [w]
  (assoc w
         :ui/render!
         #'render!))

(defn init!
  [w]
  (-> w
      (parts.replicant.core/add-ui-log)
      (parts.replicant.core/add-store)
      (add-get-register)
      (parts.replicant.core/add-pages)
      (parts.replicant.core/add-routes)
      (add-render)
      (parts.replicant.core/add-render-watcher!)
      (parts.replicant.core/add-event-handler)
      (parts.replicant.core/add-dispatch!)
      (parts.replicant.core/add-store-dispatch!)
      (parts.replicant.core/add-routing-anchor!)
      ))

(defn main! []
  (swap! system
         init!)

  ;; Trigger the initial render
  (router/navigate! @system)
  )
