(ns chat.routes.websockets
  (:require [clojure.tools.logging :as log]
            [immutant.web.async :as async]
            [cheshire.core :as ch]))

(defonce channels (atom #{}))
(defonce user-name (atom ""))

(defn broadcast!
  [channel msg]
  (doseq [channel @channels]
    (async/send! channel msg)))

(defn connect! [channel]
  (log/info "channel open")
  (swap! channels conj channel)
  (.attach channel "user-name" @user-name)
  (broadcast! nil (ch/encode {:type "connected"
                              :from (.get channel "user-name")})))

(defn disconnect!
  [channel {:keys [code reason]}]
  (log/info "close code:" code "reason:" reason)
  (swap! channels disj channel)
  (broadcast! nil (ch/encode {:type "exit"
                              :from (.get channel "user-name")})))

(defn error! [channel thr]
  (log/error "Error in channel. Closed" thr)
  (swap! channels disj channel)
  (broadcast! nil (ch/encode {:type "exit"
                              :from (.get channel "user-name")})))

(def websocket-callbacks
  {:on-open connect!
   :on-close disconnect!
   :on-message broadcast!
   :on-error error!})

(defn ws-handler [request]
  (log/info "inside ws-handler")
  (async/as-channel request websocket-callbacks))

