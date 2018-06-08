(ns chat.routes.chat
  (:require [chat.layout :as layout]
            [compojure.core :refer [defroutes GET]]
            [chat.routes.websockets :refer [user-name ws-handler]]))

(defn chat-page []
  (layout/render "chat.html"))

(defroutes chat-routes
  (GET "/chat" [] (chat-page))
  (GET "/wschat" [name]
       (reset! user-name name)
       ws-handler))
