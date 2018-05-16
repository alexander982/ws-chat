(ns chat.handler
  (:require [compojure.core :refer [routes wrap-routes]]
            [chat.layout :refer [error-page]]
            [chat.routes.home :refer [home-routes]]
            [chat.routes.websockets :refer [websocket-routes]]
            [compojure.route :as route]
            [chat.env :refer [defaults]]
            [mount.core :as mount]
            [chat.middleware :as middleware]))

(mount/defstate init-app
                :start ((or (:init defaults) identity))
                :stop  ((or (:stop defaults) identity)))

(def app-routes
  (routes
   websocket-routes
   (-> #'home-routes
       (wrap-routes middleware/wrap-csrf)
       (wrap-routes middleware/wrap-formats))
   (route/not-found
    (:body
     (error-page {:status 404
                  :title "page not found"})))))


(defn app [] (middleware/wrap-base #'app-routes))
