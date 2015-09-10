(ns mini-mud.core
  (:require [mini-mud.client.core :as client]
            [mini-mud.network.client-handler :as client-handler]
            ; [mini-mud.network.servers.socket-server :as server]
            [mini-mud.network.servers.non-blocking-socket-server :as server]
            :reload-all)
  (:gen-class))

;; (def game-server (server/run-server 10100 client-handler/client-handler))

;; (server/stop-server game-server)

(defn -main
  [& args]
  (case (first args)
    "server"
    (let [port (Integer/parseInt (or (second args) "10100"))]
      (println "port: " port)
      (server/run-server port client-handler/client-handler))
    "client"
    (let [host (or (second args) "localhost")
          port (Integer/parseInt (if (>= (count args) 3)
                                   (nth args 2)
                                   "10100"))]
      (println "host: " host)
      (println "port: " port)
      (client/run host port))))
