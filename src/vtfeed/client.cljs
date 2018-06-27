(ns vtfeed.client
  (:require [reagent.core :as reagent]
            [re-frame.core :as rf]
            [day8.re-frame.http-fx]
            [ajax.core :as ajax]
            [clojure.string :as str]))

;; A detailed walk-through of this source code is provided in the docs:
;; https://github.com/Day8/re-frame/blob/master/docs/CodeWalkthrough.md

;; -- Domino 1 - Event Dispatch -----------------------------------------------

(defn dispatch-timer-event
  []
  (let [now (js/Date.)]
    (rf/dispatch [:timer now])))  ;; <-- dispatch used

;; Call the dispatching function every second.
;; `defonce` is like `def` but it ensures only one instance is ever
;; created in the face of figwheel hot-reloading of this file.
(defonce do-timer (js/setInterval dispatch-timer-event 10000))


;; -- Domino 2 - Event Handlers -----------------------------------------------

(rf/reg-event-db              ;; sets up initial application state
  :initialize                 ;; usage:  (dispatch [:initialize])
  (fn [_ _]                   ;; the two parameters are not important here, so use _
    {:time (js/Date.)         ;; What it returns becomes the new application state
     :feeds []
     :subscription ""}))

(rf/reg-event-db
  :subscription-change
  (fn [db [_ value]]
    (assoc db :subscription value)))   ;; compute and return the new application state

(rf/reg-event-fx
 :subscription-add
 (fn [{:keys [db]} [_ value]]
   {:db (assoc db :subscription "")
    :http-xhrio
    {:method  :post
     :params  {:id value :name value :url value}
     :uri     "/subscriptions"
     :timeout 8000
     :format          (ajax/json-request-format)
     :response-format (ajax/json-response-format {:keywords? true})
     :on-success      [:subscription-add-success]
     :on-failure      [:subscription-add-failure]}}))

(rf/reg-event-db
 :subscription-add-success
 (fn [db _]
   db))

(rf/reg-event-db
 :subscription-add-failure
 (fn [db _]
   db))

(rf/reg-event-db
 :feed-list-success
 (fn [db [_ resp]]
   (assoc db :feeds (:body resp))))

(rf/reg-event-db
 :feed-list-failure
 (fn [db _]
   db))

(rf/reg-event-fx
  :timer
  (fn [{:keys [db]} [_ new-time]]
    {:db (assoc db :time new-time)
     :http-xhrio
     {:method  :get
      :params  {:since (.toISOString new-time)}
      :uri     "/feeds"
      :timeout 8000
      :response-format (ajax/json-response-format {:keywords? true})
      :on-success      [:feed-list-success]
      :on-failure      [:feed-list-failure]}}))


;; -- Domino 4 - Query  -------------------------------------------------------

(rf/reg-sub
  :time
  (fn [db _]     ;; db is current app state. 2nd unused param is query vector
    (:time db))) ;; return a query computation over the application state

(rf/reg-sub
  :time-color
  (fn [db _]
    (:time-color db)))

(rf/reg-sub
 :feeds
 (fn [db _]
   (:feeds db)))

(rf/reg-sub
 :subscription
 (fn [db _]
   (:subscription db)))

;; -- Domino 5 - View Functions ----------------------------------------------

(defn clock
  []
  [:div
   (-> @(rf/subscribe [:time])
       .toTimeString
       (str/split " ")
       first)])

(defn add-subscription
  []
  [:div
   "Channel ID: "
   [:input {:type "text"
            :value @(rf/subscribe [:subscription])
            :on-change #(rf/dispatch [:subscription-change (-> % .-target .-value)])}]
   [:input {:type "button"
            :value "+"
            :on-click #(rf/dispatch [:subscription-add @(rf/subscribe [:subscription])])}]])

(defn feed-entry
  [feed]
  [:li
   [:div.feed
    [:div.feed-header
     (:channel feed)]
    [:div.feed-body
     (:title feed)
     (:url feed)]
    [:div.feed-footer
     (:published feed)]]])

(defn feed-list
  []
  [:ul
   (for [feed @(rf/subscribe [:feeds])]
     ^{:key (:id feed)} (feed-entry feed))])

(defn messages
  []
  [:div])

(defn ui
  []
  [:div
   [messages]
   [add-subscription]
   [clock]
   [feed-list]])

;; -- Entry Point -------------------------------------------------------------

(defn ^:export run
  []
  (rf/dispatch-sync [:initialize])     ;; puts a value into application state
  (reagent/render [ui]              ;; mount the application's ui into '<div id="app" />'
    (js/document.getElementById "app")))
