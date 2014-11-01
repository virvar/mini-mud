(ns mini-mud.logic.core)

(def game-map {:location-ids #{1 2 3} :start-location-id 1})

(def locations {1 {:id 1 :objects '(:lake) :neighbour-locations {:north 2}}
                2 {:id 2 :objects '(:forrest) :neighbour-locations {:north 3 :south 1}}
                3 {:id 3 :objects '() :neighbour-locations {:south 2}}})

(def players (atom #{}))

(def players-map (atom {}))

(def players-sequence (atom 1))

(defn generate-name []
  (let [name (str "player" @players-sequence)]
    (swap! players-sequence inc)
    name))

(defn add-player []
  (let [location (locations (:start-location-id game-map))
        player {:name (generate-name) :location location}]
    (swap! players conj player)
    (if (contains? @players-map location)
      (swap! players-map update-in [location] conj player)
      (swap! players-map assoc location #{player}))
    player))

(defn move-player
  [player direction]
  (if (contains? (:neighbour-locations (:location player)) direction)
    (let [location (:location player)
          new-location (locations ((:neighbour-locations location) direction))
          new-player (assoc player :location new-location)]
      (swap! players disj player)
      (swap! players conj new-player)
      (swap! players-map update-in [location] disj player)
      (if (contains? @players-map new-location)
        (swap! players-map update-in [new-location] conj new-player)
        (swap! players-map assoc new-location #{new-player}))
      new-player)
    player))
