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

(defn feeds-request
  [time]
  {:method  :get
   :params  {:until (.toISOString time)
             :limit 20}
   :uri     "/feeds"
   :timeout 8000
   :response-format (ajax/json-response-format {:keywords? true})
   :on-success      [:feed-list-success]
   :on-failure      [:feed-list-failure]})

(rf/reg-cofx
 :time
 (fn [coeffects & fs]
   (let [recv (apply comp fs)
         now  (js/Date.)
         changed (recv now)]
     (assoc coeffects :time changed))))

(defn yesterday
  [d]
  (do (.setDate d (- (.getDate d) 1))
      d))

(rf/reg-event-fx              ;; sets up initial application state
  :initialize                 ;; usage:  (dispatch [:initialize])
  [(rf/inject-cofx :time yesterday)]
  (fn [{time :time} _]                   ;; the two parameters are not important here, so use _
    {:db
     {:time (js/Date.)         ;; What it returns becomes the new application state
      :feeds []
      :subscription ""}
     :http-xhrio
     (feeds-request time)}))

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
 (fn [db [_ feeds]]
   (update db :feeds #(concat % feeds))))

(rf/reg-event-db
 :feed-list-failure
 (fn [db _]
   db))

(rf/reg-event-fx
  :timer
  (fn [{:keys [db]} [_ new-time]]
    {:db (assoc db :time new-time)
     :http-xhrio (feeds-request new-time)}))


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
  [:div.field.has-addons
   [:div.control
    [:input.input
     {:type "text"
      :placeholder "Channel ID"
      :value @(rf/subscribe [:subscription])
      :on-change #(rf/dispatch [:subscription-change (-> % .-target .-value)])}]]
   [:div.control
    [:a.button
     {:href "#"
      :on-click #(rf/dispatch [:subscription-add @(rf/subscribe [:subscription])])}
     "+"]]])

(defn feed-entry
  [feed]
  ^{:key (:id feed)}
  [:article.feed.media
   [:figure.media-left
    [:a.image.is-4by3
     {:href (:url feed)
      :style {:width 210}}
     [:img {:src (:thumbnail feed)}]]]
   [:div.media-content
    [:p.title.is-5
     [:a.has-text-dark
       {:href (:url feed)
        :style {:text-decoration "none"}}
       (:title feed)]]
    [:p.subtitle.is-6
     [:a.has-text-grey
      {:href (:author-uri feed)
       :style {:text-decoration "none"}}
      (:author-name feed)]]]
   [:div.media-right
    [:p.has-text-grey-light (:published feed)]]])

(defn feed-list
  []
  [:div
   (for [feed @(rf/subscribe [:feeds])]
     (feed-entry feed))])

(defn messages
  []
  [:div])

(defn menu-brand
  []
  [:div.navbar-brand
   [:a.navbar-item
    {:href "/index.html"}
    "vtfeed"]
   [:a.navbar-burger
    {:role "button"
     :aria-label "menu"
     :aria-expanded "false"}
    [:span {:aria-hidden "true"}]
    [:span {:aria-hidden "true"}]
    [:span {:aria-hidden "true"}]
    ]])

(defn menu-subscription
  []
  [:a.navbar-item
   {:href "#"}
   "Subscription"])

(defn ui
  []
  [:div.container
   [:div
    [messages]]
   [:nav.navbar
    {:role "navigation"
     :aria-label "main navigation"}
    [menu-brand]
    [:div.navbar-menu
     [:div.navbar-start
      [menu-subscription]]]]
   [:div.section
    [add-subscription]
    [:div.columns
     [feed-list]
     ]
    ]
   [:div.footer
    [:p
     "Copyright è¢Ì 2018 Yutaka Imamura, All rights reserved"]]])

;; -- Entry Point -------------------------------------------------------------

(defn ^:export run
  []
  (rf/dispatch-sync [:initialize])     ;; puts a value into application state
  (reagent/render [ui]              ;; mount the application's ui into '<div id="app" />'
    (js/document.getElementById "app")))
