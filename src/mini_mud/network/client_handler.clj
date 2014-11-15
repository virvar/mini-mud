(ns mini-mud.network.client-handler
  (:require [clojure.string :as str]
            [mini-mud.logic.world-state :as world] :reload-all)
  (:use [clojure.core.match :only (match)]))

(def ^{:private true} players (atom {}))

(def ^{:private true} locations-map {"север" :north, "юг" :south, "запад" :west, "восток" :east})

(defn- location-command?
  [command]
  (contains? locations-map command))

(defn- handle-connection!
  [client-id client-notifier]
  (swap! players assoc client-id (:id (world/add-player! client-notifier)))
  (client-notifier "Hello")
  (println @players))

(defn- handle-player-message
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

(defn- handle-message
  [client-id msg]
  (let [player-id (get @players client-id)
        player (world/get-player player-id)]
    (handle-player-message player msg)))

(defn- handle-disconnection!
  [client-id]
  (let [player-id (get @players client-id)
        player (world/get-player player-id)]
    (world/exit-player! player)
    (swap! players dissoc client-id)))

(def client-handler
  {:handle-connection handle-connection!
   :handle-message handle-message
   :handle-disconnection handle-disconnection!})
