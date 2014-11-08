(ns mini-mud.logic.core)

(def world
  (atom {:locations-by-id
         {1 {:id 1, :objects [:lake], :neighbour-locations-ids {:north 2}, :players-ids #{}},
          2 {:id 2, :objects [:forrest], :neighbour-locations-ids {:north 3, :south 1},
             :players-ids #{}},
          3 {:id 3, :objects [], :neighbour-locations-ids {:south 2}, :players-ids #{}}},
         :start-location-id 1,
         :players-by-id {}}))

(defn get-location
  [world location-id]
  (get-in world [:locations-by-id location-id]))

(defn get-player
  [world player-id]
  (get-in world [:players-by-id player-id]))

(defn get-player-default
  [player-id]
  (get-player @world player-id))

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

(def players-sequence (ref 0))

(defn generate-player-id!
  []
  (dosync
   (alter players-sequence inc)))

(defn add-player! [notifier]
  (let [player-id (generate-player-id!)
        name (str "игрок" player-id)
        location-id (:start-location-id @world)
        player {:id player-id, :name name, :location-id location-id, :notifier notifier}]
    (swap! world
           (fn [world]
             (-> world
                 (assoc-in [:players-by-id player-id] player)
                 (update-in [:locations-by-id location-id :players-ids] conj player-id))))
    (println (str name " added"))
    player))

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

(defn move-player!
  [player direction]
  (let [new-world (swap! world move-player player direction)
        new-player (get-player new-world (:id player))]
    (println (str (:name player) " пошел на " direction))
    (notify-location new-world (get-player-location new-world player)
                     (str "[" (:name player) " пошел на " direction "]"))
    (notify-location new-world (get-player-location new-world new-player)
                     (str "[" (:name player) " пришел]"))
    (notify-player new-player (get-location-info
                               (get-player-location new-world new-player)))))

(defn say
  [world player msg]
  (println "say")
  (let [display-msg (str (:name player) ": " msg)]
    (notify-location world (get-player-location world player) display-msg)))

(defn say-default
  [player msg]
  (say @world player msg))

(defn whisper
  [world player to-player-name msg]
  (println "whisper")
  (let [to-player (first (filter #(= to-player-name (:name %)) (vals (:players-by-id world))))
        display-msg (str (:name player) " шепчет: " msg)]
    (notify-player to-player display-msg)))

(defn whisper-default
  [player to-player-name msg]
  (whisper @world player to-player-name msg))

(defn exit-player
  [world player]
  (let [location (get-player-location world player)]
    (-> world
        (update-in [:players-by-id] dissoc (:id player))
        (update-in [:locations-by-id (:id location) :players-ids] disj (:id player)))))

(defn exit-player!
  [player]
  (let [new-world (swap! world exit-player player)
        location (get-player-location new-world player)]
    (notify-location world
                     (get-location world (:id location))
                     (str "[" (:name player) " вышел]"))
    (println (str (:name player) " exited"))))
