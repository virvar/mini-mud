(ns mini-mud.network.servers.socket-server
  (:import [java.net ServerSocket Socket SocketException]
           [java.io InputStreamReader OutputStreamWriter BufferedReader]))

(def client-sequence (atom 0))

(defn- create-client-notifier
  [writer]
  (fn [msg]
    (binding [*out* writer]
      (println msg)
      (flush))))

(defn- handle-client!
  [ins outs client-handler]
  (let [reader (BufferedReader. (InputStreamReader. ins))
        writer (OutputStreamWriter. outs)
        client-id (swap! client-sequence inc)]
    ((:handle-connection client-handler) client-id (create-client-notifier writer))
    (binding [*in* reader]
      (loop [msg (read-line)]
        (println msg)
        (when (some? msg)
          ((:handle-message client-handler) client-id msg)
          (recur (read-line))))
      ((:handle-disconnection client-handler) client-id))))

(defn- get-socket-handler
  [client-handler]
  (fn [socket]
    (future (handle-client! (.getInputStream socket) (.getOutputStream socket) client-handler))))

(defn- create-server
  [accept-socket port]
  (let [socket (ServerSocket. port)]
    (future (while (not (.isClosed socket))
              (try (accept-socket (.accept socket))
                   (catch SocketException e
                     (println e)))))
    socket))

(defn run-server
  [port client-handler]
  (create-server (get-socket-handler client-handler) port))

(defn stop-server
  [server]
  (.close server))
