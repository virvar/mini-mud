(ns mini-mud.network.servers.non-blocking-socket-server
  (:import [java.nio ByteBuffer CharBuffer]
           [java.nio.channels SelectionKey Selector ServerSocketChannel
            SocketChannel]
           [java.nio.charset Charset CharsetDecoder CharsetEncoder]
           [java.io IOException]))

(def client-sequence (atom 0))

(defn- create-client-notifier
  [client encoder]
  (fn [msg] (.write client (.encode encoder (CharBuffer/wrap (str msg "\n"))))))


(defn- handle-connection
  [server selector client-sequence client-handler encoder]
  (let [client (.accept server)]
    (.configureBlocking client false)
    (println "New client")
    (let [client-key (.register client selector SelectionKey/OP_READ)
          client-id (swap! client-sequence inc)]
      (.attach client-key client-id)
      ((:handle-connection client-handler)
       client-id
       (create-client-notifier client encoder)))))

(defn- close-client
  [client-handler key]
  (let [client (.channel key)]
    (do (.cancel key)
      (.close client)
      (let [client-id (.attachment key)]
        ((:handle-disconnection client-handler) client-id))
      (println "Client exited"))))

(defn- handle-message
  [key buffer decoder client-handler]
  (let [client (.channel key)]
    (try
      (let [bytes-read (.read client buffer)]
        (if (= bytes-read -1)
          (close-client client-handler key)
          (do
            (.flip buffer)
            (let [raw-msg (.toString (.decode decoder buffer))
                  msg (first (clojure.string/split-lines raw-msg))
                  client-id (.attachment key)]
              (.clear buffer)
              (println "client: " msg)
              ((:handle-message client-handler) client-id msg)))))
      (catch IOException ex
        (println ex)
        (close-client client-handler key)))))

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
        (try
          (doseq [key (.selectedKeys selector)]
            (cond (.isAcceptable key)
                  (handle-connection server selector client-sequence client-handler encoder)
                  (.isReadable key)
                  (handle-message key buffer decoder client-handler)))
          (catch Exception ex (do (println ex))))
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
