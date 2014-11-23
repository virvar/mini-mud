(ns mini-mud.network.servers.non-blocking-socket-server
  (:import [java.nio ByteBuffer CharBuffer]
           [java.nio.channels SelectionKey Selector ServerSocketChannel
            SocketChannel]
           [java.nio.charset Charset CharsetDecoder CharsetEncoder]))

(def client-sequence (atom 0))

(defn handle-connections
  [server accept-socket port]
  (let [charset (Charset/forName "ISO-8859-1")
        encoder (.newEncoder charset)
        decoder (.newDecoder charset)
        buffer (ByteBuffer/allocate 512)]
    (println "socket created")
    (.configureBlocking server false)
    (.bind (.socket server) (java.net.InetSocketAddress. "127.0.0.1" port))
    (println "bound to port")
    (let [selector (Selector/open)]
      (.register server selector SelectionKey/OP_ACCEPT)
      (while (.isOpen selector)
        (.select selector)
        (doseq [key (.selectedKeys selector)]
          (cond (.isAcceptable key)
                (let [client (.accept server)]
                  (.configureBlocking client false)
                  (println "New client")
                  (let [client-key (.register client selector SelectionKey/OP_READ)]
                    (.attach client-key (swap! client-sequence inc))))
                (.isReadable key)
                (let [client (.channel key)
                      bytes-read (.read client buffer)]
                  (if (= bytes-read -1)
                    (do (.cancel key)
                        (.close client)
                        (println "Client exited"))
                    (do
                      (println "readable")
                      (.flip buffer)
                      (let [msg (.toString (.decode decoder buffer))
                            client-id (.attachment key)
                            response (str client-id ": " msg)]
                        (.clear buffer)
                        (.write client (.encode encoder (CharBuffer/wrap response)))
                        (println msg)))))))
        (-> selector
            (.selectedKeys)
            (.clear))
        (println "next iter")
        (Thread/sleep 1000))
      (println "end"))))

(defn- create-server
  [accept-socket port]
  (let [socket (ServerSocketChannel/open)]
    (future (handle-connections socket accept-socket port))
    socket))

(defn run-server
  [port client-handler]
  (create-server nil port))

(defn stop-server
  [server]
  (.close server))

(def run (run-server 10100 nil))

(stop-server run)
