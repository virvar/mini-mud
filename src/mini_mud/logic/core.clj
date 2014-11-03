(ns mini-mud.logic.core)

(def world
  (ref {:locations-by-id
        {1 {:id 1, :objects '(:lake), :neighbour-locations-ids {:north 2}, :players-ids #{}},
         2 {:id 2, :objects '(:forrest), :neighbour-locations-ids {:north 3, :south 1}, :players-ids #{}},
         3 {:id 3, :objects '(), :neighbour-locations-ids {:south 2}, :players-ids #{}}},
        :start-location-id 1,
        :players-by-id {}}))

(defn get-location
  [location-id]
  (get-in @world [:locations-by-id location-id]))

(defn get-player
  [player-id]
  (get-in @world [:players-by-id player-id]))

(defn get-neighbour
  [location direction]
  (get-in @world [:locations-by-id (get-in location [:neighbour-locations-ids direction])]))

(defn get-player-location
  [player]
  (get-location (:location-id player)))

(defn notify-player
  [player msg]
  ((:notifier player) msg))

(defn notify-location
  [location msg]
  (doseq [player-id (:players-ids location)
          :let [player (get-player player-id)]]
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
    (dosync
     (alter world assoc-in [:players-by-id player-id] player)
     (alter world update-in [:locations-by-id location-id :players-ids] conj player-id))
    (println (str name " added"))
    player))

(defn get-location-info
  [location]
  (str location))

(defn move-player!
  [player direction]
  (let [current-location (get-player-location player)]
    (when-let [new-location (get-neighbour current-location direction)]
      (dosync
       (alter world assoc-in [:players-by-id (:id player) :location-id] (:id new-location))
       (alter world update-in [:locations-by-id (:id current-location) :players-ids] disj (:id player))
       (alter world update-in [:locations-by-id (:id new-location) :players-ids] conj (:id player)))
                                        ;@FIXME
      (notify-location (get-location (:id current-location)) (str "[" (:name player) " пошел на " direction "]"))
      (notify-location (get-location (:id new-location)) (str "[" (:name player) " пришел]"))
      (let [new-player (get-player (:id player))]
        (notify-player new-player (get-location-info new-player))))))

(defn say
  [player msg]
  (println "say")
  (let [display-msg (str (:name player) ": " msg)]
    (notify-location (get-player-location player) display-msg)))

(defn whisper
  [player to-player-name msg]
  (println "whisper")
  (let [to-player (first (filter #(= to-player-name (:name %)) (vals (:players-by-id @world))))
        display-msg (str (:name player) " шепчет: " msg)]
    (notify-player to-player display-msg)))

(defn exit-player!
  [player]
  (let [location (get-player-location player)]
    (dosync
     (alter world update-in [:players-by-id] dissoc (:id player))
     (alter world update-in [:locations-by-id (:id location) :players-ids] disj (:id player)))
    (notify-location (get-location (:id location)) (str "[" (:name player) " вышел]")))
  (println (str (:name player) " exited")))
