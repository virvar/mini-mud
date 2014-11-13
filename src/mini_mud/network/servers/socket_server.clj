(ns mini-mud.network.servers.socket-server
  (:import [java.net ServerSocket Socket SocketException]))

(defn- create-server
  [accept-socket port]
  (let [socket (ServerSocket. port)]
    (future (while (not (.isClosed socket))
              (try (accept-socket (.accept socket))
                   (catch SocketException e
                     (.close socket)))))
    socket))

(defn- get-socket-handler
  [client-handler]
  (fn [socket]
    (future (client-handler (.getInputStream socket) (.getOutputStream socket)))))

(defn run-server
  [port client-handler]
  (create-server (get-socket-handler client-handler) port))

(defn stop-server
  [server]
  (.close server))
