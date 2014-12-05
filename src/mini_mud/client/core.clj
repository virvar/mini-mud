(ns mini-mud.client.core
  (:require [aleph.tcp]
            [manifold.stream :as mstream]
            [byte-streams :as bs])
  (:import [java.net ServerSocket Socket SocketException]
           [java.io InputStreamReader OutputStreamWriter BufferedReader])
  (:gen-class :main true))

(defn- handle-messages
  [stream]
  (try
    (loop []
      (if-let [bytes @(mstream/take! stream)]
        (let [msg (bs/to-string bytes)]
          (do (println msg)
              (recur)))
        (do (println "Disconnected")
            (mstream/close! stream))))
    (catch Exception e
      (println (.getMessage e)))))

(defn- send-messages
  [stream]
  (loop [msg (read-line)]
    (if (= msg "quit")
      (println "quit")
      (do @(mstream/put! stream msg)
          (Thread/sleep 50)
          (recur (read-line)))))
  (mstream/close! stream))

(defn- start
  [host port]
  (try (let [stream @(aleph.tcp/client {:host host, :port port})]
         (future (handle-messages stream))
         (future (send-messages stream)))
       (catch Exception e
         (println (.getMessage e)))))

(defn -main
  [& args]
  (start "localhost" 10100))

(-main)
