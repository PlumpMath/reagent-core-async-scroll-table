(ns reagent-test-table.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as reagent :refer [atom]]
            [goog.dom :as dom]
            [goog.events :as events]
            [cljs.core.async :as async
             :refer [>! <! put! chan alts!]]))

;; forward declare go in my namespace
(declare go)

;; define app-state
(def app-state
  (atom {:height 500
         :width 800
         :row-count 100
         :row-height 30
         :visible-indices []}))

;; intents
(def scroll-channel (chan))
(go (>! scroll-channel 0))

;; models
(defn make-visible-indices-channel
  [scroll-channel]
  (let [visible-indices-channel (chan)]
    (go (while true
          (let [scroll-top (<! scroll-channel)
                {row-height :row-height
                 row-count :row-count
                 height :height} @app-state
                first-visible-row (Math/floor (/ scroll-top row-height))
                visible-rows (Math/ceil (/ height row-height))
                last-row (+ first-visible-row visible-rows 1)
                visible-indices (range first-visible-row last-row)]
            (>! visible-indices-channel visible-indices))))
    visible-indices-channel))

(def visible-indices-channel (make-visible-indices-channel scroll-channel))
(go (while true
      (let [x (<! visible-indices-channel)]
        (swap! app-state assoc :visible-indices x))))

;; views
(defn row-view
  [r]
  (let [{key :key
         index :index
         row-height :row-height
         column-widths :column-widths} r]
    [:tr {:key key
          :style {:position "absolute"
                  :top (* index row-height)
                  :width "100%"
                  :borderBottom "1px solid black"}}
     [:td {:style {:width (get column-widths 0)}} index]
     [:td {:style {:width (get column-widths 1)}} (* 10 index)]
     [:td {:style {:width (get column-widths 2)}} (* 100 index)]]))

(defn table-body-view
  [r]
  (let [{visible-indices :visible-indices
         row-height :row-height
         column-widths :column-widths} r]
    [:tbody
     (map-indexed (fn [i row-index]
                    (row-view {:key (mod i (count visible-indices))
                               :index row-index
                               :row-height row-height
                               :column-widths column-widths}))
                  visible-indices)]))

(def root-view
  (with-meta
    (fn []
      (let [{height :height
             width :width
             row-count :row-count
             row-height :row-height
             visible-indices :visible-indices} @app-state]
        [:div#app-container
         [:div.scroll-table-container
          {:id "scroll-table-container"
           :style {:position "relative"
                   :overflowX "hidden"
                   :border "1px solid black"
                   :height height
                   :width width}}
          [:table.scroll-table {:style {:height (* row-count row-height)}}
           (table-body-view {:row-height 30
                             :visible-indices visible-indices
                             :column-widths [(/ width 3)
                                             (/ width 3)
                                             (/ width 3)]})]]]))
    {:component-did-mount
     #(let [node (dom/getElement "scroll-table-container")]
        (events/listen node "scroll"
                       (fn [] (go (put! scroll-channel (.-scrollTop node))))))}))

;; app mounter
(defn mount-root []
  (reagent/render [root-view] (.getElementById js/document "app")))

;; start app
(defn init! []
  (mount-root))
