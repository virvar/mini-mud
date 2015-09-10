(ns mini-mud.logic.world-state
  (:require [mini-mud.logic.core :as logic] :reload-all))

(def world
  (atom {:locations-by-id
         {1 {:id 1, :objects [:lake], :neighbour-locations-ids {:north 2}, :players-ids #{}},
          2 {:id 2, :objects [:forrest], :neighbour-locations-ids {:north 3, :south 1},
             :players-ids #{}},
          3 {:id 3, :objects [], :neighbour-locations-ids {:south 2}, :players-ids #{}}},
         :start-location-id 1,
         :players-by-id {}}))

(defn get-player
  [player-id]
  (logic/get-player @world player-id))

(def players-sequence (atom 0))

(defn- generate-player-id!
  []
  (swap! players-sequence inc))

(defn add-player! [notifier]
  (let [player-id (generate-player-id!)
        name (str "player" player-id)]
    (swap! world logic/add-player player-id name notifier)
    (println (str name " added"))
    (get-player player-id)))

(defn move-player!
  [player direction]
  (let [new-world (swap! world logic/move-player player direction)
        new-player (logic/get-player new-world (:id player))]
    (println (str (:name player) " went to " direction))
    (logic/notify-location new-world (logic/get-player-location new-world player)
                           (str "[" (:name player) " went to " direction "]"))
    (logic/notify-location new-world (logic/get-player-location new-world new-player)
                           (str "[" (:name player) " came]"))
    (logic/notify-player new-player (logic/get-location-info
                                     (logic/get-player-location new-world new-player)))))

(defn say
  [player msg]
  (logic/say @world player msg))

(defn whisper
  [player to-player-name msg]
  (logic/whisper @world player to-player-name msg))

(defn exit-player!
  [player]
  (let [new-world (swap! world logic/exit-player player)
        location (logic/get-player-location new-world player)]
    (logic/notify-location world
                           (logic/get-location world (:id location))
                           (str "[" (:name player) " exited]"))
    (println (str (:name player) " exited"))))
