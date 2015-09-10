(defproject mini-mud "0.1.0-SNAPSHOT"
  :description "Multithreaded server implementation for learning goals"
  :url "https://github.com/virvar/mini-mud"
  :license {:name "GNU GENERAL PUBLIC LICENSE"
            :url "http://www.gnu.org/licenses/gpl-2.0.html"}
  :plugins [[cider/cider-nrepl "0.8.0-SNAPSHOT"]]
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/core.match "0.2.1"]
                 [aleph "0.4.0-alpha9"]
                 [manifold "0.1.0-beta3"]
                 [byte-streams "0.2.0-alpha4"]]
  :aot [mini-mud.core]
  :main mini-mud.core)
