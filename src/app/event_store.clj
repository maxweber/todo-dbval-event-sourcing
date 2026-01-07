(ns app.event-store
  "Event store using the same SQLite database as dbval.

   Events are stored in an 'events' table alongside dbval's 'dbval' table,
   enabling atomic transactions across both event storage and read-model updates."
  (:require [clojure.edn :as edn])
  (:import [java.sql Connection PreparedStatement ResultSet]))

(defn- ensure-events-table!
  "Creates the events table if it doesn't exist.
   Must be called with the dbval connection."
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

(defn store-event!
  "Store an event using the given SQLite connection.
   The connection should be the dbval connection for transactional consistency."
  [^Connection conn event]
  (let [sql "INSERT INTO events (id, event_type, aggregate_type, aggregate_id, data, timestamp, tx_id, user_id, version)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
        ^PreparedStatement stmt (.prepareStatement conn sql)]
    (try
      (.setString stmt 1 (str (:event/id event)))
      (.setString stmt 2 (pr-str (:event/type event)))
      (.setString stmt 3 (pr-str (:event/aggregate event)))
      (.setString stmt 4 (str (:event/aggregate-id event)))
      (.setString stmt 5 (pr-str (:event/data event)))
      (.setString stmt 6 (str (:event/timestamp event)))
      (.setString stmt 7 (str (:event/tx-id event)))
      (.setString stmt 8 (some-> (:event/user-id event) str))
      (.setInt stmt 9 (or (:event/version event) 1))
      (.executeUpdate stmt)
      (finally
        (.close stmt)))))

(defn- parse-event
  "Parse a ResultSet row into an event map."
  [^ResultSet rs]
  {:event/id (parse-uuid (.getString rs "id"))
   :event/type (edn/read-string (.getString rs "event_type"))
   :event/aggregate (edn/read-string (.getString rs "aggregate_type"))
   :event/aggregate-id (parse-uuid (.getString rs "aggregate_id"))
   :event/data (edn/read-string (.getString rs "data"))
   :event/timestamp (java.time.Instant/parse (.getString rs "timestamp"))
   :event/tx-id (parse-uuid (.getString rs "tx_id"))
   :event/user-id (some-> (.getString rs "user_id") parse-uuid)
   :event/version (.getInt rs "version")
   :event/sequence-num (.getLong rs "sequence_num")})

(defn get-all-events
  "Get all events from the given connection in order."
  [^Connection conn]
  (let [stmt (.createStatement conn)
        rs (.executeQuery stmt "SELECT * FROM events ORDER BY sequence_num")]
    (try
      (loop [events []]
        (if (.next rs)
          (recur (conj events (parse-event rs)))
          events))
      (finally
        (.close rs)
        (.close stmt)))))

(defn get-events-for-aggregate
  "Get all events for a specific aggregate."
  [^Connection conn aggregate-type aggregate-id]
  (let [sql "SELECT * FROM events WHERE aggregate_type = ? AND aggregate_id = ? ORDER BY sequence_num"
        ^PreparedStatement stmt (.prepareStatement conn sql)]
    (try
      (.setString stmt 1 (pr-str aggregate-type))
      (.setString stmt 2 (str aggregate-id))
      (let [rs (.executeQuery stmt)]
        (try
          (loop [events []]
            (if (.next rs)
              (recur (conj events (parse-event rs)))
              events))
          (finally
            (.close rs))))
      (finally
        (.close stmt)))))

(defn drop-dbval-table!
  "Drop the dbval table for replay. Used when rebuilding the read-model."
  [^Connection conn]
  (let [stmt (.createStatement conn)]
    (try
      (.execute stmt "DROP TABLE IF EXISTS dbval")
      (finally
        (.close stmt)))))
