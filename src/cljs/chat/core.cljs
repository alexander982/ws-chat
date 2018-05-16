(ns chat.core
  (:require [reagent.core :as r]
            [clojure.string :as s]
            [chat.db :as db]
            [goog.net.cookies :as cookie])
  (:import [goog.date DateTime]))

(enable-console-print!)

(defonce ws (atom nil))

(defn make-websocket!
  [url handler]
  (println "attempting to connect websocket")
  (if-let [chan (js/WebSocket. url)]
    (do
      (set! (.-onmessage chan) handler)
      (reset! ws chan)
      (println "Websocket connection esteblished with: " url))
    (throw (js/Error. "Websocket connection failed!"))))

;;; UI

(defonce messages (r/atom []))
(defonce user-name (r/atom (cookie/get "user-name"
                                       (str "anonymous" (rand-int 1000)))))
(defonce users (r/atom #{}))

(def hidden {:display :none})
(def visible {:display :inline})

(defn clj->json [m]
  (.stringify js/JSON (clj->js m)))

(defn json->clj [s]
  (js->clj (.parse js/JSON s) :keywordize-keys true))

(defn send! [msg]
  (if @ws
    (.send @ws msg)
    (throw (js/Error. "Websocket is not available!"))))

(defn user-name-field []
  (let [state (r/atom {:value @user-name
                       :old ""
                       :inp-style hidden
                       :btn-style hidden})]
    (fn []
      [:div {:on-mouse-over #(swap! state assoc :btn-style visible)
             :on-mouse-out #(swap! state assoc :btn-style hidden)} 
       [:strong @user-name
        [:input
         {:type :edit
          :value (:value @state)
          :style (:inp-style @state)
          :on-change (fn [e] (swap! state assoc
                                    :value (-> e .-target .-value)))
          :on-key-down #(when (= (.-keyCode %) 13)
                          (let [value (s/trim (:value @state))]
                            (if (and (> (count value) 0)
                                     (< (count value) 32)
                                     (not (some #{value} @users)))
                              (do (send! (clj->json {:type "nchange"
                                                     :from (:old @state)
                                                     :new value}))
                                  (reset! user-name value)
                                  (cookie/set "user-name" value 65700000))
                              (reset! user-name (:old @state))))
                          (swap! state assoc :inp-style hidden))}]
        [:a.btn.btn-link {:title "Редактировать ник"
                          :type "button"
                          :style (:btn-style @state)
                          :on-click
                          (fn [e] (do (swap! state assoc
                                             :inp-style visible
                                             :old @user-name)
                                      (reset! user-name nil)))}
         "\u270f"]
        [:a.btn.btn-link {:title "Очистить чат"
                          :style (:btn-style @state)
                          :on-click (fn [e]
                                      (reset! messages [])
                                      (db/clear))}
         "☠"]]])))

(def warn-style    {:style {:color :red}})
(def info-style    {:style {:color :green}})
(def success-style {:style {:color :blue}})

(defn message-list []
  [:ul#list.list-unstyled
   (for [[i message] (map-indexed vector @messages)]
     ^{:key i}
     (cond (= "msg" (:type message))
           [:li {:key i}
            (str "[" (:time message) "]"
                 "[" (:from message) "]: " (:msg message))]
           ;; name change
           (= "nchange" (:type message))
           [:li  (assoc warn-style :key i)
            (str "Пользователь " (:from message) " сменил ник на "
                 (:new message))]
           ;; user connected
           (= "connected" (:type message))
           [:li (assoc success-style :key i)
            (str (:from message) " вошёл в чат")]
           ;; user exited
           (= "exit" (:type message))
           [:li (assoc warn-style :key i)
            (str (:from message) " вышел")]
           ;; great new user
           (= "hi" (:type message))
           nil
           :default
           [:li (assoc info-style :key i) message]))])

(defn message-input []
  (let [value (r/atom nil)]
    (fn []
      [:input.form-control
       {:type :text
        :placeholder "Введите сообщение"
        :value @value
        :on-change #(reset! value (-> % .-target .-value))
        :on-key-down
        #(when (= (.-keyCode %) 13)
           ;;send
           (when (> (count @value) 0)
             (let [dt (DateTime.)]
               (send! (clj->json {:type "msg"
                                  :from @user-name
                                  :msg @value
                                  :time (.toIsoTimeString dt)}))))
           (reset! value nil))}])))

(defn user-list []
  [:ul.list-unstyled
   (for [[i user] (map-indexed vector @users)]
     [:li {:key i} user])])

(def messages-per-page 50)

(defn echo-page []
  [:div 
   [:div#user-name.row
    [:div.col-md-12
     [user-name-field]]]
   [:div.row
    [:div#message-list.col-sm-9 {:on-scroll
                                 (fn [e]
                                   (let [t (.-target e)]
                                     (when (= (.-scrollTop t)
                                              (- (.-scrollHeight t)
                                                 (.-clientHeight t)))
                                       (db/get-next-messages
                                        (count @messages)
                                        messages-per-page
                                        (fn [val]
                                          (swap! messages
                                                 #(apply conj % val)))))))
                                 :style {:height "400px"
                                         :overflow :auto}}
     [message-list]]
    [:div.col-sm-3 {:style {:height "400px"
                            :overflow :auto}}
     [user-list]]]
   [:div.row
    [:div.col-sm-9
     [message-input]]]])

(defn handle-message!
  [event]
  (let [message (json->clj (.-data event))]
    (swap! messages #(vec (cons message %)))
    (db/put-message message)
    (cond
      ;;exit
      (= "exit" (:type message))
      (do
        (swap! users #(disj % (:from message)))
        )
      ;;connected
      (= "connected" (:type message))
      (do 
        (send! (clj->json {:type "hi"
                          :from @user-name})))
      ;;hi
      (= "hi" (:type message))
      (do
        (swap! users conj (:from message))
        )
      ;;name change
      (= "nchange" (:type message))
      (do (swap! users #(disj % (:from message)))
          (swap! users conj (:new message))))))

(defn mount-components []
  (r/render-component [#'echo-page] (.getElementById js/document "chat")))

(defn init! []
  (println "init...")
  (make-websocket! (str "ws://"
                        (.-host js/location)
                        js/context (str "/wschat?name=" @user-name))
                   handle-message!)
  (db/get-last-messages
   messages-per-page
   (fn [value]
     (swap! messages #(apply conj % value))))
  (mount-components))
