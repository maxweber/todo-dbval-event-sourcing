(ns app.event-processor
  "Processes events: stores in SQLite and applies to dbval read-model.

   Both operations happen in the same SQLite transaction since dbval
   uses the same SQLite database file."
  (:require [app.event-store :as store]
            [app.projections.core :as proj]
            [app.db-schema :as schema]
            [dbval.core :as d])
  (:import [java.sql Connection]))

(defn- get-sqlite-conn
  "Get the underlying SQLite connection from a dbval connection."
  ^Connection [dbval-con]
  (:conn @dbval-con))

(defn- ensure-events-table!
  "Ensure the events table exists in the dbval SQLite database.
   Does NOT commit - caller is responsible for transaction management."
  [^Connection conn]
  (let [stmt (.createStatement conn)]
    (try
      (.execute stmt "
        CREATE TABLE IF NOT EXISTS events (
          sequence_num INTEGER PRIMARY KEY AUTOINCREMENT,
          id TEXT NOT NULL UNIQUE,
          event_type TEXT NOT NULL,
          aggregate_type TEXT NOT NULL,
          aggregate_id TEXT NOT NULL,
          data TEXT NOT NULL,
          timestamp TEXT NOT NULL,
          tx_id TEXT NOT NULL,
          user_id TEXT,
          version INTEGER NOT NULL DEFAULT 1
        )")
      (.execute stmt "
        CREATE INDEX IF NOT EXISTS idx_events_aggregate
        ON events(aggregate_type, aggregate_id)")
      (.execute stmt "
        CREATE INDEX IF NOT EXISTS idx_events_timestamp
        ON events(timestamp)")
      (.execute stmt "
        CREATE INDEX IF NOT EXISTS idx_events_tx
        ON events(tx_id)")
      (finally
        (.close stmt)))))

(defn process-events!
  "Process events within a single SQLite transaction.

   Takes world-map w with :db/con (dbval connection).
   Takes command-result which is {:ok events} or {:error msg}.

   Both event storage and dbval projection happen in the same transaction,
   ensuring atomicity across the event store and read-model."
  [w command-result]
  (if-let [events (:ok command-result)]
    (let [tx-id (random-uuid)
          user-id nil ;; No auth in simple example
          events-with-meta (mapv #(assoc %
                                         :event/tx-id tx-id
                                         :event/user-id user-id)
                                 events)
          dbval-con (:db/con w)
          ^Connection sqlite-conn (get-sqlite-conn dbval-con)]

      (try
        ;; Ensure events table exists (idempotent, no commit)
        (ensure-events-table! sqlite-conn)

        ;; 1. Store events in SQLite (no commit yet)
        (doseq [event events-with-meta]
          (store/store-event! sqlite-conn event))

        ;; 2. Apply projections to dbval
        (let [tx-data (vec (mapcat proj/apply-event events-with-meta))]
          (when (seq tx-data)
            ;; dbval.transact! commits the transaction internally
            (d/transact! dbval-con tx-data))

          ;; If no projections, commit manually
          (when (empty? tx-data)
            (.commit sqlite-conn)))

        ;; 3. Return success
        (assoc w
               :command/result {:success? true
                                :aggregate-id (:aggregate-id command-result)}
               :event-processor/events events-with-meta)

        (catch Exception e
          ;; Rollback on any error
          (.rollback sqlite-conn)
          (throw e))))

    ;; Error case - no events to process
    (assoc w
           :command/result {:success? false
                            :error (:error command-result)})))

(defn replay-all-events!
  "Rebuild dbval read-model by replaying all events from SQLite.

   DESTRUCTIVE: Drops the dbval table and rebuilds it from events.
   All operations happen in a single SQLite transaction, so queries
   during replay see a consistent state (the old data until commit).

   Returns {:replayed count}."
  [dbval-con]
  (let [^Connection sqlite-conn (get-sqlite-conn dbval-con)
        ;; Read events first (before we drop anything)
        events (store/get-all-events sqlite-conn)
        db-file (:db-file @dbval-con)]

    (try
      ;; Drop the dbval table (within transaction, not visible to others yet)
      (store/drop-dbval-table! sqlite-conn)

      ;; Create fresh dbval table structure
      (let [stmt (.createStatement sqlite-conn)]
        (try
          (.execute stmt "CREATE TABLE IF NOT EXISTS dbval (k BLOB PRIMARY KEY)")
          (finally
            (.close stmt))))

      ;; Replay each event and apply projections
      (doseq [event events]
        (let [tx-data (vec (proj/apply-event event))]
          (when (seq tx-data)
            ;; Use db-with to build up state without committing yet
            (swap! dbval-con d/db-with tx-data))))

      ;; Commit the entire replay as one transaction
      (.commit sqlite-conn)

      {:replayed (count events)}

      (catch Exception e
        ;; Rollback on any error - original data remains intact
        (.rollback sqlite-conn)
        (throw e)))))
