(ns mini-mud.core
  (:require [mini-mud.network.client-handler :as client-handler]
            [mini-mud.network.servers.socket-server :as server] :reload-all))

(def game-server (server/run-server 10100 client-handler/client-handler))

(server/stop-server game-server)
