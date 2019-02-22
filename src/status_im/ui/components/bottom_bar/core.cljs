(ns status-im.ui.components.bottom-bar.core
  (:require-macros [status-im.utils.views :as views])
  (:require
   [status-im.ui.components.animation :as animation]
   [status-im.ui.components.bottom-bar.styles :as tabs.styles]
   [reagent.core :as reagent]
   [status-im.ui.components.react :as react]
   [status-im.utils.platform :as platform]
   [status-im.ui.components.icons.vector-icons :as vector-icons]
   [status-im.ui.components.common.common :as components.common]
   [status-im.i18n :as i18n]
   [status-im.ui.components.styles :as common.styles]
   [re-frame.core :as re-frame]
   [status-im.ui.components.colors :as colors]))

(defn animate
  ([visible duration to]
   (animate visible duration to nil))
  ([visible duration to callback]
   (animation/start
    (animation/timing visible
                      {:toValue         to
                       :duration        duration
                       :useNativeDriver true})
    callback)))

(def tabs-list-data
  [{:view-id             :chat-stack
    :content             {:title (i18n/label :t/home)
                          :icon  :main-icons/home}
    :count-subscription  :chats/unread-messages-number
    :accessibility-label :home-tab-button}
   {:view-id             :wallet-stack
    :content             {:title (i18n/label :t/wallet)
                          :icon  :main-icons/wallet}
    :count-subscription  :get-wallet-unread-messages-number
    :accessibility-label :wallet-tab-button}
   {:view-id             :profile-stack
    :content             {:title (i18n/label :t/profile)
                          :icon  :main-icons/profile}
    :count-subscription  :get-profile-unread-messages-number
    :accessibility-label :profile-tab-button}])

(defn- tab-content [{:keys [title icon]}]
  (fn [active? count]
    [react/view {:style tabs.styles/tab-container}
     [react/view
      [vector-icons/icon icon (tabs.styles/tab-icon active?)]]
     [react/view
      [react/text {:style (tabs.styles/tab-title active?)}
       title]]
     (when (pos? count)
       [react/view tabs.styles/counter-container
        [react/view tabs.styles/counter
         [components.common/counter count]]])]))

(def tabs-list (map #(update % :content tab-content) tabs-list-data))

(views/defview tab [view-id content active? accessibility-label count-subscription]
  (views/letsubs [count [count-subscription]]
    [react/touchable-highlight
     (cond-> {:style    common.styles/flex
              :disabled active?
              :on-press #(re-frame/dispatch [:navigate-to-tab view-id])}
       accessibility-label
       (assoc :accessibility-label accessibility-label))
     [react/view
      [content active? count]]]))

(def height 52)

:message
:dapp
:wallet
:profile
(defn new-tab [{:keys [icon label active?]}]
  [react/touchable-highlight
   {:style    {:flex   1
               :height height}
    :on-press #(js/alert icon)}
   [react/view
    {:style
     {:flex             1
      :background-color colors/white
      :height           height
      :align-items      :center
      :justify-content  :space-between
      :padding-top      6
      :padding          4}}
    [vector-icons/icon icon
     {:color  (if active? colors/blue colors/gray)
      :height 24
      :width  24}]
    [react/view {:style {:align-self      :stretch
                         :height          14
                         :align-items     :center
                         :justify-content :center}}
     [react/text {:style {:color     (if active? colors/blue colors/gray)
                          :font-size 11}}
      label]]]])

(defn tabs [current-view-id]
  [react/view
   {:style {:height         height
            :align-self     :stretch
            :shadow-radius  4
            :shadow-offset  {:width 0 :height -5}
            :shadow-opacity 0.3
            :shadow-color   "rgba(0, 9, 26, 0.12)"}}
   [react/view {:style {:height         height
                        :align-self     :stretch
                        :background-color colors/white
                        :padding-left    8
                        :padding-right   8
                        :flex-direction :row}}
    [new-tab {:icon    :main-icons/message
              :label   "Chats"
              :active? (= current-view-id :chat-stack)}]
    [new-tab {:icon    :main-icons/dapp
              :label   "DApps"
              :active? (= current-view-id :dapp-stack)}]
    [new-tab {:icon    :main-icons/wallet
              :label   "Wallet"
              :active? (= current-view-id :wallet-stack)}]
    [new-tab {:icon    :main-icons/profile
              :label   "Profile"
              :active? (= current-view-id :profile-stack)}]]]
  #_[react/view {:style tabs.styles/tabs-container}
     (for [{:keys [content view-id accessibility-label count-subscription]} tabs-list]
       ^{:key view-id} [tab view-id content (= view-id current-view-id) accessibility-label count-subscription])])

(defn tabs-animation-wrapper [visible? keyboard-shown? tab]
  [react/animated-view
   {:style {:height    height
            :bottom    0
            :left      0
            :right     0
            :position  (when keyboard-shown? :absolute)
            :transform [{:translateY
                         (animation/interpolate
                          visible?
                          {:inputRange  [0 1]
                           :outputRange [height 0]})}]}}
   [react/safe-area-view [tabs tab]]])

(def disappearance-duration 150)
(def appearance-duration 100)

(defn bottom-bar [_]
  (let [keyboard-shown? (reagent/atom false)
        visible?        (animation/create-value 1)
        listeners       (atom [])]
    (reagent/create-class
     {:component-will-mount
      (fn []
        (when platform/android?
          (reset!
           listeners
           [(.addListener react/keyboard "keyboardDidShow"
                          (fn []
                            (reset! keyboard-shown? true)
                            (animate visible?
                                     disappearance-duration 0)))
            (.addListener react/keyboard "keyboardDidHide"
                          (fn []
                            (reset! keyboard-shown? false)
                            (animate visible? appearance-duration 1)))])))
      :component-will-unmount
      (fn []
        (when (not-empty @listeners)
          (doseq [listener @listeners]
            (when listener
              (.remove listener)))))
      :reagent-render
      (fn [args]
        (let [idx (.. (:navigation args)
                      -state
                      -index)
              tab (case idx
                    0 :chat-stack
                    1 :wallet-stack
                    2 :profile-stack
                    :chat-stack)]
          (if platform/ios?
            [react/safe-area-view [tabs tab]]
            [tabs-animation-wrapper visible? @keyboard-shown? tab])))})))
