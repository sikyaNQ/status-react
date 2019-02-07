(ns status-im.ui.screens.home.views
  (:require-macros [status-im.utils.views :as views])
  (:require [re-frame.core :as re-frame]
            [status-im.ui.components.list.views :as list]
            [status-im.ui.components.list-item.views :as list-item]
            [status-im.ui.components.react :as react]
            [reagent.core :as reagent]
            [status-im.ui.components.animation :as animation]
            [status-im.ui.components.toolbar.view :as toolbar]
            [status-im.ui.components.toolbar.actions :as toolbar.actions]
            [status-im.ui.components.connectivity.view :as connectivity]
            [status-im.ui.components.colors :as colors]
            [status-im.ui.screens.home.views.inner-item :as inner-item]
            [status-im.ui.screens.home.styles :as styles]
            [status-im.utils.platform :as platform]
            [status-im.react-native.resources :as resources]
            [status-im.i18n :as i18n]
            [status-im.ui.components.common.common :as components.common]
            [status-im.ui.components.icons.vector-icons :as icons]
            [status-im.utils.datetime :as time]
            [status-im.ui.components.react :as components]
            [status-im.utils.utils :as utils]))

(defn- toolbar [show-welcome? show-sync-state sync-state latest-block-number logged-in?]
  (when-not (and show-welcome?
                 platform/android?)
    [toolbar/toolbar nil nil
     (when-not show-welcome?
       (if show-sync-state
         [react/view {:style styles/sync-wrapper}
          [components.common/logo styles/toolbar-logo]

          [react/touchable-highlight {:accessibility-label :new-chat-button
                                      :on-press            #(re-frame/dispatch [:home.ui/sync-info-pressed])}
           [react/text {:style styles/sync-info}
            (str "LES: 'latest' #" latest-block-number "\n"
                 (if sync-state
                   (str "syncing " (:currentBlock sync-state) " of " (:highestBlock sync-state) " blocks...")
                   (str "not syncing")))]]]
         [toolbar/content-wrapper
          [components.common/logo styles/toolbar-logo]]))
     (cond
       (and platform/ios?
            logged-in?)
       [toolbar/actions
        [(-> (toolbar.actions/add true #(re-frame/dispatch [:navigate-to :new]))
             (assoc-in [:icon-opts :accessibility-label] :new-chat-button))]]
       platform/ios?
       [react/view {:style styles/spinner-container}
        [components/activity-indicator {:color colors/blue
                                        :animating true}]])]))

(defn- home-action-button [logged-in?]
  [react/view styles/action-button-container
   [react/touchable-highlight {:accessibility-label :new-chat-button
                               :on-press            (when logged-in? #(re-frame/dispatch [:navigate-to :new]))}
    [react/view styles/action-button
     (if-not logged-in?
       [components/activity-indicator {:color :white
                                       :animating true}]
       [icons/icon :main-icons/add {:color :white}])]]])

(defn home-list-item [[home-item-id home-item]]
  (let [delete-action   (if (:chat-id home-item)
                          (if (and (:group-chat home-item)
                                   (not (:public? home-item)))
                            :group-chats.ui/remove-chat-pressed
                            :chat.ui/remove-chat)
                          :browser.ui/remove-browser-pressed)
        inner-item-view (if (:chat-id home-item)
                          inner-item/home-list-chat-item-inner-view
                          inner-item/home-list-browser-item-inner-view)]
    [list/deletable-list-item {:type      :chats
                               :id        home-item-id
                               :on-delete #(do
                                             (re-frame/dispatch [:set-swipe-position :chats home-item-id false])
                                             (re-frame/dispatch [delete-action home-item-id]))}
     [inner-item-view home-item]]))

;;do not remove view-id and will-update or will-unmount handlers, this is how it works
(views/defview welcome [view-id]
  (views/letsubs [handler #(re-frame/dispatch [:set-in [:accounts/create :show-welcome?] false])]
    {:component-will-update  handler
     :component-will-unmount handler}
    [react/view {:style styles/welcome-view}
     [react/view {:style styles/welcome-image-container}
      [react/image {:source (:welcome-image resources/ui)
                    :style  styles/welcome-image}]]
     [react/i18n-text {:style styles/welcome-text :key :welcome-to-status}]
     [react/view
      [react/i18n-text {:style styles/welcome-text-description
                        :key   :welcome-to-status-description}]]]))

(def search-input-height 56)

(defn search-input
  [search-filter {:keys [on-cancel on-focus on-change]}]
  (let [input-is-focused? (reagent/atom false)
        input-ref (reagent/atom nil)]
    (fn [search-filter]
      (let [show-cancel? (or @input-is-focused?
                             (not-empty search-filter))]
        [react/view {:style {:height             search-input-height
                             :flex-direction     :row
                             :padding-horizontal 16
                             :background-color   colors/white
                             :align-items        :center
                             :justify-content    :center}}
         [react/view {:style {:background-color colors/gray-lighter
                              :flex             1
                              :flex-direction   :row
                              :height           36
                              :align-items      :center
                              :justify-content  :center
                              :border-radius    8}}
          [icons/icon :main-icons/search {:color           colors/gray
                                          :container-style {:margin-left  6
                                                            :margin-right 2}}]
          [react/text-input {:placeholder     (i18n/label :t/search)
                             :blur-on-submit  true
                             :multiline       false
                             :number-of-lines 1
                             :ref             #(reset! input-ref %)
                             :style           {:flex        1
                                               :margin      0
                                               :padding     0
                                               :line-height 22
                                               :font-size   15}
                             :default-value   search-filter
                             :on-focus
                             #(do
                                (when on-focus
                                  (on-focus))
                                (reset! input-is-focused? true))
                             :on-change
                             (fn [e]
                               (let [native-event (.-nativeEvent e)
                                     text         (.-text native-event)]
                                 (when on-change
                                   (on-change text))))}]]
         (when show-cancel?
           [react/touchable-highlight
            {:on-press #(do
                          (when on-cancel
                            (on-cancel))
                          (.blur @input-ref)
                          (reset! input-is-focused? false))
             :style {:margin-left 16}}
            [react/text {:style {:color     colors/blue
                                 :font-size 15}}
             (i18n/label :t/cancel)]])]))))

(defn home-empty-view []
  [react/view styles/no-chats
   [react/i18n-text {:style styles/no-chats-text :key :no-recent-chats}]])

(defn home-filtered-items-list [chats browsers]
  [list/section-list
   {:sections [{:title :t/chats
                :data chats}
               {:title :t/browsers
                :data browsers}
               {:title :t/messages
                :data []}]
    :key-fn first
    :render-section-header-fn (fn [{:keys [title data]}]
                                [react/text {:style {:font-size   15
                                                     :margin-left 16
                                                     :margin-top  16
                                                     :color       colors/gray}}
                                 (i18n/label title)])
    :render-section-footer-fn (fn [{:keys [title data]}]
                                (when (empty? data)
                                  [list/big-list-item {:text          (i18n/label (if (= title "messages")
                                                                                    :t/messages-search-coming-soon
                                                                                    :t/no-result))
                                                       :text-color    colors/gray
                                                       :hide-chevron? true
                                                       :action-fn     #()
                                                       :icon          (case title
                                                                        "messages" :main-icons/private-chat
                                                                        "browsers" :main-icons/browser
                                                                        "chats"    :main-icons/message)
                                                       :icon-color    colors/gray}]))
    :render-fn (fn [home-item]
                 [home-list-item home-item])}])

(defn hide-search-input
  [anim-search-input-height]
  (animation/start
   (animation/timing anim-search-input-height
                     {:toValue  0
                      :duration 350
                      :easing   (.in (animation/easing)
                                     (.-quad (animation/easing)))})))

(defn show-search-input
  [anim-search-input-height]
  (animation/start
   (animation/timing anim-search-input-height
                     {:toValue  search-input-height
                      :duration 350
                      :easing   (.out (animation/easing)
                                      (.-quad (animation/easing)))})))

(defn home-items-view
  [search-filter {:keys [chats browsers all-home-items] :as home-items}]
  (let [scrolling-from-top? (reagent/atom false)
        anim-search-input-height (animation/create-value 0)]
    (fn [search-filter {:keys [chats browsers all-home-items] :as home-items}]
      (if home-items
        [react/view
         [react/animated-view {:style {:height anim-search-input-height}}
          [search-input search-filter {:on-cancel #(do
                                                     (re-frame/dispatch [:search/filter-changed nil])
                                                     (hide-search-input anim-search-input-height))
                                       :on-focus  #(when-not search-filter
                                                     (re-frame/dispatch [:search/filter-changed ""]))
                                       :on-change (fn [text]
                                                    (re-frame/dispatch [:search/filter-changed text]))}]]
         (case search-filter
           nil [list/flat-list {:data   all-home-items
                                :key-fn first
                                :on-scroll-begin-drag
                                (fn [e]
                                  (when (and
                                         (not @scrolling-from-top?)
                                         (zero? (.-y (.-contentOffset (.-nativeEvent e)))))
                                    (reset! scrolling-from-top? true)))
                                :on-scroll-end-drag
                                (fn [e]
                                  (when (and @scrolling-from-top?
                                             (zero? (.-y (.-contentOffset (.-nativeEvent e)))))
                                    (reset! scrolling-from-top? false)
                                    (show-search-input anim-search-input-height)))
                                :render-fn
                                (fn [home-item]
                                  [home-list-item home-item])}]
           "" nil
           [home-filtered-items-list chats browsers])]
        [home-empty-view]))))

(views/defview home [loading?]
  (views/letsubs [show-welcome? [:get-in [:accounts/create :show-welcome?]]
                  view-id [:get :view-id]
                  logging-in? [:get :accounts/login]
                  sync-state [:chain-sync-state]
                  latest-block-number [:latest-block-number]
                  rpc-network? [:current-network-uses-rpc?]
                  network-initialized? [:current-network-initialized?]
                  search-filter [:search/filter]
                  home-items [:home-items]]
    {:component-did-mount
     (fn [this]
       (let [[_ loading?] (.. this -props -argv)]
         (when loading?
           (utils/set-timeout
            #(re-frame/dispatch [:init-rest-of-chats])
            100))))}
    [react/view styles/container
     [toolbar show-welcome? (and network-initialized? (not rpc-network?))
      sync-state latest-block-number (not logging-in?)]
     (cond show-welcome?
           [welcome view-id]
           loading?
           [react/view {:style {:flex            1
                                :justify-content :center
                                :align-items     :center}}
            [connectivity/connectivity-view]
            [components/activity-indicator {:flex 1
                                            :animating true}]]
           :else
           [react/view {:style {:flex 1}}
            [connectivity/connectivity-view]
            [home-items-view search-filter home-items]])
     (when platform/android?
       [home-action-button (not logging-in?)])]))

(views/defview home-wrapper []
  (views/letsubs [loading? [:get :chats/loading?]]
    [home loading?]))
