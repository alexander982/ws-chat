(ns user
  (:require [mount.core :as mount]
            chat.core))

(defn start []
  (mount/start-without #'chat.core/repl-server))

(defn stop []
  (mount/stop-except #'chat.core/repl-server))

(defn restart []
  (stop)
  (start))


