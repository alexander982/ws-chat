(ns chat.routes.chat
  (:require [chat.layout :as layout]
            [compojure.core :refer [defroutes GET]]))

(defn chat-page []
  (layout/render "chat.html"))

(defroutes chat-routes
  (GET "/chat" [] (chat-page)))
