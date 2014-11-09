(ns mini-mud.network.servers.socket-server
  (:require [clojure.string :as str]
            [mini-mud.logic.world-state :as world] :reload-all)
  (:use [clojure.core.match :only (match)])
  (:import [java.net ServerSocket Socket SocketException]
           [java.io InputStreamReader OutputStreamWriter BufferedReader]))

(def locations-map {"север" :north, "юг" :south, "запад" :west, "восток" :east})

(defn create-server
  [accept-socket port]
  (let [socket (ServerSocket. port)]
    (future (while (not (.isClosed socket))
              (try (accept-socket (.accept socket))
                   (catch SocketException e
                     (.close socket)))))
    socket))

(defn create-player-notifier
  [writer]
  (fn [msg]
    (binding [*out* writer]
      (println msg)
      (flush))))

(defn location-command?
  [command]
  (contains? locations-map command))

(defn handle-message
  [player msg]
  (let [msg-words (str/split msg #" " 4)]
    (match [msg-words]
           [["сказать" "пользователю" to-player-name & sending-message]]
           (world/whisper player to-player-name (str/join " " sending-message))
           [["сказать" & sending-message]]
           (world/say player (str/join " " sending-message))
           [[(location :guard location-command?)]]
           (world/move-player! player (get locations-map location))
           [["выход"]]
           (world/exit-player! player)
           :else "Неизвестная команда")))

(defn handle-client
  [ins outs]
  (let [eof (Object.)
        reader (BufferedReader. (InputStreamReader. ins))
        writer (OutputStreamWriter. outs)
        player-id (:id (world/add-player! (create-player-notifier writer)))]
    (binding [*in* reader]
      (loop [msg (read-line)]
        (when (some? msg)
          (let [player (world/get-player player-id)]
            (handle-message player msg)
            (recur (read-line)))))
      (world/exit-player! (world/get-player player-id)))))

(defn handle-socket
  [socket]
  (future (handle-client (.getInputStream socket) (.getOutputStream socket))))

(def server (create-server handle-socket 10100))

(.close server)
