(ns mini-mud.network.servers.socket-server
  (:require [mini-mud.logic.core :as logic] :reload-all)
  (:import [java.net ServerSocket Socket SocketException]
           [java.io InputStreamReader OutputStreamWriter]
           [clojure.lang LineNumberingPushbackReader]))

(defn on-thread [f]
  (doto (Thread. f) (.start)))

(defn create-server
  [accept-socket port]
  (let [socket (ServerSocket. port)]
    (on-thread #(when-not (.isClosed socket)
                  (try (accept-socket (.accept socket))
                       (catch SocketException e))
                  (recur)))
    socket))

(defn handle-message
  [player msg]
  (logic/move-player player (keyword msg)))

(defn handle-client
  [ins outs]
  (let [eof (Object.)
        reader (LineNumberingPushbackReader. (InputStreamReader. ins))
        writer (OutputStreamWriter. outs)
        init-player (logic/add-player)]
    (loop [player init-player
           msg (read reader false eof)]
      (when-not (= msg eof)
        (println msg)
        (flush)
        (let [new-player (handle-message player msg)]
          (binding [*out* writer]
            (println (logic/get-location-info new-player))
            (flush))
          (recur new-player (read reader false eof)))))))

(defn handle-socket
  [s]
  (on-thread #(handle-client (.getInputStream s) (.getOutputStream s))))

(def server (create-server handle-socket 10100))

(.close server)
