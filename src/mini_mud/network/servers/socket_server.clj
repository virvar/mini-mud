(ns mini-mud.network.servers.socket-server
  (:import [java.net ServerSocket Socket SocketException]
           [java.io InputStreamReader OutputStreamWriter]
           [clojure.lang LineNumberingPushbackReader]))

(defn on-thread [f]
  (doto (Thread. f) (.start)))

(defn create-server
  [accept-socket port]
  (let [socket (ServerSocket. port)]
    (on-thread (when-not (.isClosed socket)
                 (try (accept-socket (.accept socket))
                      (catch SocketException e))
                 (recur)))
    socket))

(defn handle-client
  [ins outs]
  (let [eof (Object.)
        reader (LineNumberingPushbackReader. (InputStreamReader. ins))
        writer (OutputStreamWriter. outs)]
    (loop [msg (read reader false eof)]
      (when-not (= msg eof)
        (println msg)
        (flush)
        (binding [*out* writer]
          (println msg)
          (flush))
        (recur (read reader false eof))))))

(defn handle-socket
  [s]
  (on-thread #(handle-client (.getInputStream s) (.getOutputStream s))))

(def server (create-server handle-socket 10100))

(.close server)
