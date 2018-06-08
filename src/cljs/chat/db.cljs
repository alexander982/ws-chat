(ns chat.db
  (:require [goog.db :as db]
            [goog.events :as events]))

(enable-console-print!)

;;IndexedDB store

(def db-version 6)
(def db-name "chat")

(defn- upgrade-db
  [event db tx]
  (try
    (.deleteObjectStore db "messages")
    (catch js/Error err
      (try
        (.createObjectStore db "messages" #js{:autoIncrement true})
        (catch js/Error err
          (println err "ошибка создания хранилища " "message")))
      (println err "ошибка удаления хранилища " "message"))))

(defn- open-db
  []
  (db/openDatabase db-name db-version upgrade-db))

(defn with-open-db
  [f]
  (let [db (open-db)]
    (.addCallback db f)
    (.addErrback db (js/Error. "ошибка открытия"))))


(defn get-all-messages
  [callback]
  (with-open-db (fn [db]
                  (let [tx (.createTransaction db ["messages"])
                        obs (.objectStore tx "messages")]
                    (.addCallback (.getAll obs) callback)
                    (.addCallback (.wait tx) #(.close db))))))

(defn put-message
  [msg]
  (with-open-db (fn [db]
                  (let [put-tx (.createTransaction db ["messages"] "readwrite")
                        obs (.objectStore put-tx "messages")
                        deferred (.wait put-tx)]
                    (.put obs (clj->js msg))
                    (.addErrback deferred #(js/Error. "ошибка записи в дб"))
                    (.addCallback deferred #(.close db))))))

(defn get-next-messages
  "get next n messages"
  [offset n callback]
  (with-open-db
    (fn [db]
      (let [tx (.createTransaction db ["messages"])
            obs (.objectStore tx "messages")
            cursor (.openCursor obs nil "prev") 
            i (atom n)
            off (atom  offset)
            res (atom [])
            key
            (events/listen cursor
                           "n"
                           (fn [e]
                             (if (> @off 0)
                               (do (.advance (.-cursor_ cursor) offset)
                                   (reset! off 0))
                               (if (> (swap! i dec) 0)
                                 (do (swap! res conj (js->clj
                                                      (.getValue cursor)
                                                      :keywordize-keys true))
                                     (.next cursor))
                                 (callback @res)))))]
        (-> tx
            (.wait)
            (.addCallback (fn [db]
                            (println "close db after get-next-messages")
                            (events/unlistenByKey key)
                            (.close db))))))))
(defn get-last-messages
  "get last n messages"
  [n callback]
  (get-next-messages 0 n callback))

(defn clear []
  (with-open-db
    (fn [db]
      (let [tx (.createTransaction db ["messages"] "readwrite")
            obs (.objectStore tx "messages")
            deferred (.wait tx)]
        (.clear obs)
        (.addCallback deferred #(.close db))
        (.addErrback deferred #(println "error"))))))
