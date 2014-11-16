(ns mini-mud.network.servers.non-blocking-socket-server
  (:import [java.nio ByteBuffer]
           [java.nio.channels SelectionKey Selector ServerSocketChannel
            SocketChannel]
           [java.nio.charset Charset CharsetDecoder CharsetEncoder]))

(defn handle-connections
  [server accept-socket port]
  (let [
        ;server (ServerSocketChannel/open)
        charset (Charset/forName "ISO-8859-1")
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
                  (println "new client")
                  (.register client selector (bit-or SelectionKey/OP_READ
                                                     SelectionKey/OP_WRITE)))
                (.isReadable key)
                (let [client (.channel key)]
                  (println "readable")
                  (.read client buffer)
                  (let [msg (.toString (.decode decoder buffer))]
                    (println msg))
                  (.clear buffer))))
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
