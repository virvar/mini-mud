(ns mini-mud.network.servers.non-blocking-socket-server
  (:import [java.nio ByteBuffer CharBuffer]
           [java.nio.channels SelectionKey Selector ServerSocketChannel
            SocketChannel]
           [java.nio.charset Charset CharsetDecoder CharsetEncoder]))

(def client-sequence (atom 0))

(defn- create-client-notifier
  [client encoder]
  (fn [msg] (.write client (.encode encoder (CharBuffer/wrap (str msg "\n"))))))

(defn handle-connections
  [server port client-handler]
  (let [charset (Charset/forName "UTF-8")
        encoder (.newEncoder charset)
        decoder (.newDecoder charset)
        buffer (ByteBuffer/allocate 512)]
    (println "socket created")
    (.configureBlocking server false)
    (.bind (.socket server) (java.net.InetSocketAddress. "127.0.0.1" port))
    (println "bound to port")
    (let [selector (Selector/open)]
      (.register server selector SelectionKey/OP_ACCEPT)
      (while (.isOpen server)
        (.select selector)
        (doseq [key (.selectedKeys selector)]
          (cond (.isAcceptable key)
                (let [client (.accept server)]
                  (.configureBlocking client false)
                  (println "New client")
                  (let [client-key (.register client selector SelectionKey/OP_READ)
                        client-id (swap! client-sequence inc)]
                    (.attach client-key client-id)
                    ((:handle-connection client-handler)
                     client-id
                     (create-client-notifier client encoder))))
                (.isReadable key)
                (let [client (.channel key)
                      bytes-read (.read client buffer)]
                  (if (= bytes-read -1)
                    (do (.cancel key)
                        (.close client)
                        (let [client-id (.attachment key)]
                          ((:handle-disconnection client-handler) client-id))
                        (println "Client exited"))
                    (do
                      (println "readable")
                      (.flip buffer)
                      (let [raw-msg (.toString (.decode decoder buffer))
                            msg (first (clojure.string/split-lines raw-msg))
                            client-id (.attachment key)]
                        (.clear buffer)
                        (println msg)
                        ((:handle-message client-handler) client-id msg)))))))
        (-> selector
            (.selectedKeys)
            (.clear))
        (println "next iter"))
      (.close selector)
      (println "end"))))

(defn- create-server
  [port client-handler]
  (let [server (ServerSocketChannel/open)]
    (future (handle-connections server port client-handler))
    server))

(defn run-server
  [port client-handler]
  (create-server port client-handler))

(defn stop-server
  [server]
  (.close server))
