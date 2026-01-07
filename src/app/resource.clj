(ns app.resource
  (:require [ring.middleware.file]
            [co.deps.ring-etag-middleware]
            [ring.middleware.content-type]
            [ring.middleware.not-modified]
            ))

(defn wrap-no-cache
  "Add 'Cache-Control: no-cache' to responses.
   This allows the client to cache the response, but requires it to check with
   the server every time to make sure that the response is still valid, before
   using the locally cached file. This avoids stale files being served because of
   overzealous browser caching, while still speeding up load times by caching
   files.

   See: https://danielcompton.net/how-to-serve-clojurescript"
  [handler]
  (fn [req]
    (some-> (handler req)
      (update :headers assoc
              "Cache-Control" "no-cache"))))

(def resources
  (delay
    (-> (fn [_request]
          nil)
        (ring.middleware.file/wrap-file "public")
        (co.deps.ring-etag-middleware/wrap-file-etag)
        (wrap-no-cache)
        (ring.middleware.content-type/wrap-content-type)
        (ring.middleware.not-modified/wrap-not-modified))))

(defn dispatcher
  [w]
  (when-let [response (@resources (:ring/request w))]
    (assoc w
           :ring/response
           response)))

(def register
  [{:ring/dispatcher #'dispatcher}])
