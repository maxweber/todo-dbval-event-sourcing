(ns app.events.core
  "Core event utilities - creating and enriching events.")

(defn make-event
  "Create an event with required metadata.
   Takes a map with :event/type, :event/aggregate, :event/aggregate-id, and :event/data."
  [{:keys [event/type event/aggregate event/aggregate-id event/data]}]
  {:event/id (random-uuid)
   :event/type type
   :event/aggregate aggregate
   :event/aggregate-id aggregate-id
   :event/data data
   :event/timestamp (java.time.Instant/now)
   :event/version 1})
