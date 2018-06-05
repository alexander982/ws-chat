(ns chat.handler
  (:require [clojure.tools.logging :as log]
            [compojure.core :refer [routes wrap-routes]]
            [chat.layout :refer [error-page]]
            [chat.routes.home :refer [home-routes]]
            [chat.routes.chat :refer [chat-routes]]
            [compojure.route :as route]
            [chat.env :refer [defaults]]
            [mount.core :as mount]
            [chat.middleware :as middleware]))

(mount/defstate init-app
                :start ((or (:init defaults) identity))
                :stop  ((or (:stop defaults) identity)))

(defn init
  []
  (doseq [component (:started (mount/start))]
    (log/info component "started")))

(defn destroy
  []
  (doseq [component (:started (mount/start))]
    (log/info component "stopped"))
  (shutdown-agents)
  (log/info "chat has shut down!"))

(def app-routes
  (routes
   chat-routes
   (-> #'home-routes
       (wrap-routes middleware/wrap-csrf)
       (wrap-routes middleware/wrap-formats))
   (route/not-found
    (:body
     (error-page {:status 404
                  :title "page not found"})))))


(def app (middleware/wrap-base #'app-routes))
