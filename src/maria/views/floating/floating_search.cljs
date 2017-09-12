(ns maria.views.floating.floating-search
  (:require [re-view.core :as v :refer [defview]]
            [commands.registry :as registry :refer-macros [defcommand]]
            [maria.commands.which-key :as which-key]
            [commands.exec :as exec]
            [maria.views.dropdown :as dropdown]
            [maria.views.floating.float-ui :as ui]
            [clojure.string :as string]
            [maria.blocks.history :as history]
            [maria.views.bottom-bar :as bottom-bar]
            [maria.views.icons :as icons]
            [maria.frames.frame-communication :as frame]
            [maria.persistence.local :as local]
            [re-db.d :as d]
            [maria.pages.docs :as docs]
            [maria.commands.doc :as doc]))

(defview FloatingSearch
  "A floating search bar, displayed near the top-middle of the screen.
  Takes care of saving and returning the current focus/selection."
  {:view/initial-state (fn [] {:q ""})
   :view/did-mount     (fn [{:keys [view/state] :as this}]
                         (.captureSelections this)
                         (some-> (:input @state) (.focus)))
   :capture-selections (fn [{:keys [view/state]}]
                         (swap! state assoc :prev-selections (history/get-selections)))
   :return-selections  (fn [{:keys [view/state]}]
                         (when-let [selections (:prev-selections @state)]
                           (history/put-selections! selections)
                           (v/swap-silently! state dissoc :prev-selections)))
   :view/will-unmount  (fn [this] (.returnSelections this))}
  [{:keys [view/state on-selection on-select! items placeholder] :as this}]
  (let [{:keys [q]} @state
        max-height (- (.-innerHeight js/window)
                      20                                    ;; margin-top
                      34                                    ;; input element
                      35                                    ;; bottom bar
                      )]
    [:.fixed
     {:style {:width       300
              :margin-left -150
              :left        "50%"
              :top         20}}
     [:.bg-darken.br2.pa2
      [:.shadow-4.flex.flex-column.items-stretch
       [:input.outline-0.pa2.bn.f6 {:placeholder placeholder
                                    :style       {:border-bottom "1px solid #eee"}
                                    :ref         #(when % (swap! state assoc :input %))
                                    :value       q
                                    :on-change   #(swap! state assoc :q (.. % -target -value))}]
       (dropdown/numbered-list {:on-select!        (fn [value]
                                                     (.returnSelections this)
                                                     (ui/clear-hint!)
                                                     (on-select! value))
                                :ui/max-height     max-height
                                :default-selection 0
                                :on-selection      on-selection
                                :items             (items q)})]]]))

(defview CommandSearch
  {:view/initial-state #(do {:context (exec/get-context)})}
  [{:keys [view/state]}]
  (let [{:keys [context]} @state
        commands (exec/contextual-commands context)]
    (FloatingSearch {:on-selection #(bottom-bar/show-var! (exec/get-command context %))
                     :placeholder  "Search commands..."
                     :on-select!   #(fn [value] (exec/exec-command-name value context))
                     :items        (fn [q]
                                     (for [{:keys [name private parsed-bindings icon] :as command} commands
                                           :when (and (string/includes? (str name) q)
                                                      (not private))]
                                       {:value name
                                        :label [:.h2.mr2.sans-serif.f7.flex.items-center.flex-auto

                                                ;; Icons may add too much visual clutter
                                                #_[:.gray.pr2 (-> (or icon icons/Blank)
                                                                  (icons/size 16))]
                                                [:.gray (some-> (:display-namespace command)
                                                                (str "/"))]
                                                [:div {:style {:font-weight 500}} (:display-name command)]
                                                [:.flex-auto]
                                                [:.gray
                                                 (some->> (ffirst parsed-bindings)
                                                          (registry/keyset-string))]]}))})))

(defcommand :doc/open
  {:bindings ["M1-O"]}
  []
  (if (= ::open (:kind (ui/current-hint)))
    (ui/clear-hint!)
    (ui/floating-hint! {:component     FloatingSearch
                        :kind          ::open

                        :props         {:placeholder "Open..."
                                        :on-select!  (fn [value]
                                                       (frame/send frame/trusted-frame [:window/navigate value {}]))
                                        :items       (fn [q]
                                                       (for [doc (local/local-get (str (d/get :auth-public :username) "/recent-docs"))
                                                             :let [{:keys [url filename]} (doc/normalize-doc doc)]
                                                             :when (and filename (not= "" filename) (string/includes? filename q))]
                                                         {:value url
                                                          :label [:.sans-serif.f7.pv2.half-b (doc/strip-clj-ext filename)]}))}
                        :cancel-events ["scroll" "mousedown"]
                        :float/pos     [(/ (.-innerWidth js/window) 2)
                                        (+ (.-scrollY js/window) 100)]})))

;; input for text...
;; - auto-focus
;; - 
;; X get suggestions...
;; X put into a dropdown list with numbers

;; store stack of previous/common commands

(defcommand :commands/search
  {:bindings ["M1-P"
              "M1-Shift-P"]
   :icon     icons/Search}
  [context]
  (if (= ::search (:kind (ui/current-hint)))
    (ui/clear-hint!)
    (ui/floating-hint! {:component     CommandSearch
                        :kind          ::search
                        :props         nil
                        :cancel-events ["scroll" "mousedown"]
                        :float/pos     [(/ (.-innerWidth js/window) 2)
                                        (+ (.-scrollY js/window) 100)]}))
  true)