(ns mini-mud.logic.core)

(defn get-location
  [world location-id]
  (get-in world [:locations-by-id location-id]))

(defn get-player
  [world player-id]
  (get-in world [:players-by-id player-id]))

(defn get-neighbour
  [world location direction]
  (get-in world [:locations-by-id (get-in location [:neighbour-locations-ids direction])]))

(defn get-player-location
  [world player]
  (get-location world (:location-id player)))

(defn notify-player
  [player msg]
  ((:notifier player) msg))

(defn notify-location
  [world location msg]
  (doseq [player-id (:players-ids location)
          :let [player (get-player world player-id)]]
    (notify-player player msg)))

(defn add-player
  [world player-id name notifier]
  (let [location-id (:start-location-id world)
        player {:id player-id, :name name, :location-id location-id, :notifier notifier}]
    (-> world
        (assoc-in [:players-by-id player-id] player)
        (update-in [:locations-by-id location-id :players-ids] conj player-id))))

(defn get-location-info
  [location]
  (str location))

(defn move-player
  [world player direction]
  (let [current-location (get-player-location world player)]
    (if-let [new-location (get-neighbour world current-location direction)]
      (-> world
          (assoc-in [:players-by-id (:id player) :location-id] (:id new-location))
          (update-in [:locations-by-id (:id current-location) :players-ids]
                     disj (:id player))
          (update-in [:locations-by-id (:id new-location) :players-ids]
                     conj (:id player)))
      world)))

(defn say
  [world player msg]
  (println "say")
  (let [display-msg (str (:name player) ": " msg)]
    (notify-location world (get-player-location world player) display-msg)))

(defn whisper
  [world player to-player-name msg]
  (println "whisper")
  (when-let [to-player (first (filter #(= to-player-name (:name %)) (vals (:players-by-id world))))]
    (let [display-msg (str (:name player) " whispered: " msg)]
      (notify-player to-player display-msg))))

(defn exit-player
  [world player]
  (let [location (get-player-location world player)]
    (-> world
        (update-in [:players-by-id] dissoc (:id player))
        (update-in [:locations-by-id (:id location) :players-ids] disj (:id player)))))
