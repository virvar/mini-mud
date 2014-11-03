(ns mini-mud.network.servers.socket-server
  (:require [clojure.string :as str]
            [mini-mud.logic.core :as logic] :reload-all)
  (:import [java.net ServerSocket Socket SocketException]
           [java.io InputStreamReader OutputStreamWriter BufferedReader]))

(defn create-server
  [accept-socket port]
  (let [socket (ServerSocket. port)]
    (future (loop []
              (when-not (.isClosed socket)
                (try (accept-socket (.accept socket))
                     (catch SocketException e
                       (.close socket)))
                (recur))))
    socket))

(defn create-player-notifier
  [writer]
  (fn [msg]
    (binding [*out* writer]
      (println msg)
      (flush))))

(defn handle-message
  [player msg]
  (cond
   (re-matches #"сказать пользователю .*" msg)
   (let [cmd (str/split msg #" " 4)
         to-player-name (nth cmd 2)
         player-msg (nth cmd 3)]
     (logic/whisper player to-player-name player-msg))
   (re-matches #"сказать .*" msg) (logic/say player (nth (str/split msg #" " 2) 1))
   (= msg "север") (logic/move-player! player :north)
   (= msg "юг") (logic/move-player! player :south)
   (= msg "запад") (logic/move-player! player :west)
   (= msg "восток") (logic/move-player! player :east)
   (= msg "выход") (logic/exit-player! player)
   :else "Неизвестная команда"))

(defn handle-client
  [ins outs]
  (let [eof (Object.)
        reader (BufferedReader. (InputStreamReader. ins))
        writer (OutputStreamWriter. outs)
        player-id (:id (logic/add-player! (create-player-notifier writer)))]
    (binding [*in* reader]
      (loop [msg (read-line)]
        (when-not (nil? msg)
          (let [player (logic/get-player player-id)]
            (handle-message player msg)
            (recur (read-line)))))
      (logic/exit-player! (logic/get-player player-id)))))

(defn handle-socket
  [socket]
  (future (handle-client (.getInputStream socket) (.getOutputStream socket))))

(def server (create-server handle-socket 10100))

(.close server)
