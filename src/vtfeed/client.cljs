(ns vtfeed.client
  (:require [reagent.core :as reagent]
            [re-frame.core :as rf]
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
(defonce do-timer (js/setInterval dispatch-timer-event 60000))


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

(rf/reg-event-db
 :subscription-add
 (fn [db [_ value]]
   db))


(rf/reg-event-db                 ;; usage:  (dispatch [:timer a-js-Date])
  :timer                         ;; every second an event of this kind will be dispatched
  (fn [db [_ new-time]]          ;; note how the 2nd parameter is destructured to obtain the data value
    (assoc db :time new-time)))  ;; compute and return the new application state


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
            :on-click #(rf/dispatch [:subscription-add (fn [e] @(rf/subscribe [:subscription]))])}]])

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

(defn ui
  []
  [:div
   [add-subscription]
   [clock]
   [feed-list]])

;; -- Entry Point -------------------------------------------------------------

(defn ^:export run
  []
  (rf/dispatch-sync [:initialize])     ;; puts a value into application state
  (reagent/render [ui]              ;; mount the application's ui into '<div id="app" />'
    (js/document.getElementById "app")))
