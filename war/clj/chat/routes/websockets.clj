(ns chat.routes.websockets
  (:require [clojure.tools.logging :as log]
            [cheshire.core :as ch])
  (:import (javax.websocket.server ServerEndpoint)
           (javax.websocket OnOpen OnMessage OnError OnClose
                            Session)))

(defonce channels (atom #{}))

(defn close-channel
  [channel]
  (swap! channels #(remove #{channel} %)))

(defn broadcast
  [msg]
  (doseq [channel @channels]
    (-> (.getAsyncRemote channel)
        (.sendText msg))))

(gen-class
 :name ^{ServerEndpoint "/wschat"} WebsocketServer
 :methods [[^{OnOpen true} start [javax.websocket.Session] void]
           [^{OnMessage true} handleMessage [String] String]
           [^{OnClose true} close [javax.websocket.Session] void]
           [^{OnError true} error [javax.websocket.Session
                                   Throwable] void]]
 :state ^String user
 :init init)

(defn -start [this s]
  (log/info "channel open")
  (reset! (.user this) (->  (.getRequestParameterMap s)
                            (.get "name")
                            (.get 0)))
  (.setMaxIdleTimeout s 0)
  (swap! channels conj s)
  (broadcast (ch/encode {:type "connected"
                         :from (deref (.-user this))})))

(defn -handleMessage [this message]
  (broadcast message))

(defn -close [this s]
  (log/info "channel closed")
  (close-channel s)
  (broadcast (ch/encode {:type "exit"
                         :from (deref (.-user this))})))

(defn -error [this s thr]
  (close-channel s)
  (broadcast (ch/encode {:type "exit"
                         :from (deref (.-user this))}))
  (log/error "error on channel:" thr))

(defn- -init []
  [nil (atom "anonymous")])
