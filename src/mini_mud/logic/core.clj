(ns mini-mud.logic.core)

(def game-map {:location-ids #{1 2 3} :start-location-id 1})

(def locations {1 {:id 1 :objects '(:lake) :neighbour-locations {:north 2}}
                2 {:id 2 :objects '(:forrest) :neighbour-locations {:north 3 :south 1}}
                3 {:id 3 :objects '() :neighbour-locations {:south 2}}})

(def players-by-name (ref {}))

(def players-map (ref {}))

(def players-sequence (ref 0))

(defn notify-player
  [player msg]
  ((:notifier player) msg))

(defn generate-name []
  (dosync
   (str "player" (alter players-sequence inc))))

(defn add-player [notifier]
  (let [name (generate-name)
        location (locations (:start-location-id game-map))
        player {:name name :location location :notifier notifier}]
    (dosync
     (alter players-by-name assoc name player)
     (if (contains? @players-map location)
       (alter players-map update-in [location] conj player)
       (alter players-map assoc location #{player})))
    (println (str name " added"))
    player))

(defn get-player-by-name
  [name]
  (players-by-name name))

(defn get-location-info
  [player]
  (str (@players-map (:location player))))

(defn move-player
  [player direction]
  (if (contains? (:neighbour-locations (:location player)) direction)
    (let [location (:location player)
          new-location (locations ((:neighbour-locations location) direction))
          new-player (assoc player :location new-location)]
      (dosync
       (alter players-by-name assoc (:name player) new-player)
       (alter players-map update-in [location] disj player)
       (if (contains? @players-map new-location)
         (alter players-map update-in [new-location] conj new-player)
         (alter players-map assoc new-location #{new-player})))
      (notify-player new-player (get-location-info new-player))
      new-player)
                                        ;@TODO notify affected players
    player))

(defn say
  [player msg]
  (println "say")
  (let [display-msg (str (:name player) ": " msg)]
    (doseq [neighbour-player (players-map (:location player))]
      (notify-player neighbour-player display-msg))))

(defn whisper
  [player to-player-name msg]
  (println "whisper")
  (let [to-player (players-by-name to-player-name)
        display-msg (str (:name player) " шепчет: " msg)]
    (notify-player to-player display-msg)))

(defn exit-player
  [player]
  (dosync
   (alter players-by-name dissoc (:name player))
   (alter players-map update-in [(:location player)] disj player))
                                        ;@TODO notify affected players
  (println (str (:name player) " exited")))
