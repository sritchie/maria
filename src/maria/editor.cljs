(ns maria.editor
  (:require [cljsjs.codemirror]
            [cljsjs.codemirror.mode.clojure]
            [cljsjs.codemirror.addon.edit.closebrackets]
            [re-view.core :as v :refer-macros [defview]]
            [re-view.subscriptions :as subs :include-macros true]
            [clojure.string :as string]
            [fast-zip.core :as z]
            [maria.tree.core :as tree]
            [maria.tree.source-edit :as paredit]
            [maria.codemirror :as cm]
            [goog.events :as events]
            [cljs.pprint :refer [pprint]]
            [re-db.d :as d]
            [cljs.core.match :refer-macros [match]]))

(def ^:dynamic *self-op*)

(defn ignore-self-op
  "Editor should not fire 'change' events for self-inflicted operations."
  ;; unsure if this is behaving properly or is necessary
  [_ change]
  (when *self-op*
    (.cancel change)))

;; to support multiple editors
(defn init-local-storage
  "Given a unique id, initialize a local-storage backed source"
  [uid default-src]
  (d/transact! [[:db/add uid :source (or (aget js/window "localStorage" uid) default-src)]])
  (d/listen! [uid :source] (fn [datom]
                             (aset js/window "localStorage" uid (datom 2))))
  uid)

(defn add-view-to-args [f]
  (fn [cm] (f cm (.-view cm))))

(def options
  {:theme             "solarized light"
   :autoCloseBrackets "()[]{}\"\""
   :lineNumbers       false
   :lineWrapping      true
   :mode              "clojure"
   :keyMap            "macDefault"
   :extraKeys         (clj->js (reduce-kv (fn [m k v] (assoc m k (add-view-to-args v))) {} paredit/key-commands))
   })


(defn clear-brackets! [this]
  (doseq [handle (get-in (:state this) [:cursor :handles])]
    (.clear handle))
  (v/swap-state! this update :cursor dissoc :handles))

(defn match-brackets! [this cm node]
  (let [prev-node (get-in (:state this) [:cursor :node])]
    (when (not= prev-node node)
      (clear-brackets! this)
      (when (tree/may-contain-children? node)
        (v/swap-state! this assoc-in [:cursor :handles]
                       (cm/mark-ranges! cm (tree/node-highlights node) #js {:className "CodeMirror-matchingbracket"}))))))

(defn clear-highlight! [this]
  (doseq [handle (get-in (:state this) [:highlight-state :handles])]
    (.clear handle))
  (v/swap-state! this dissoc :highlight-state))

(defn highlight-node! [this cm node]
  (when (and (not= node (get-in (:state this) [:highlight-state :node]))
             (not (.somethingSelected cm))
             (tree/sexp? node))
    (clear-highlight! this)
    (v/swap-state! this assoc :highlight-state
                   {:node    node
                    :handles (cm/mark-ranges! cm (tree/node-highlights node) #js {:className "CodeMirror-eval-highlight"})})))

(defn update-highlights [cm e]
  (let [this (.-view cm)
        {{bracket-loc :bracket-loc cursor-node :node} :cursor
         ast                                          :ast
         zipper                                       :zipper
         :as                                          state} (:state this)]

    (match [(.-type e) (.-which e) (.-metaKey e)]
           ["mousemove" _ true] (highlight-node! this cm (->> (cm/mouse-pos cm e)
                                                              (tree/node-at zipper)
                                                              tree/mouse-eval-region
                                                              z/node))
           ["keyup" 91 false] (clear-highlight! this)
           ["keydown" _ true] (highlight-node! this cm (z/node bracket-loc))
           :else nil)))

(defn update-cursor
  [{{:keys [zipper]} :state :as this} cm]
  (let [position (cm/cursor-pos cm)]
    (when-let [loc (some->> position
                            (tree/node-at zipper))]
      (let [bracket-loc (tree/nearest-bracket-region loc)
            bracket-node (z/node bracket-loc)]
        (match-brackets! this cm bracket-node)
        (v/swap-state! this update :cursor merge {:loc          loc
                                                  :node         (z/node loc)
                                                  :bracket-loc  bracket-loc
                                                  :bracket-node bracket-node
                                                  :pos          position})))))

(defn update-ast
  [cm]
  (when-let [ast (try (tree/ast (.getValue cm))
                      (catch js/Error e (.debug js/console e)))]
    (swap! (.-view cm) assoc
           :ast ast
           :zipper (tree/ast-zip ast))))

(defview editor
         {:subscriptions
          {:source (subs/db [this] (some-> this
                                           (get-in [:props :local-storage])
                                           first
                                           (d/get :source)))}
          :will-mount
          #(some->> (get-in % [:props :local-storage])
                    (apply init-local-storage))
          :did-mount
          (fn [{{:keys [value read-only? on-mount cm-opts local-storage] :as props} :props :as this}]
            (let [dom-node (js/ReactDOM.findDOMNode (v/ref this "editor-container"))
                  editor (js/CodeMirror dom-node
                                        (clj->js (merge cm-opts
                                                        (cond-> options
                                                                read-only? (-> (select-keys [:theme :mode :lineWrapping])
                                                                               (assoc :readOnly "nocursor"))))))]
              (set! (.-view editor) this)

              (v/swap-state! this assoc :editor editor)

              (when-not read-only?
                (.on editor "beforeChange" ignore-self-op)
                (.on editor "cursorActivity" (partial update-cursor this))
                (.on editor "change" update-ast)

                ;; event handlers are passed in as props with keys like :event/mousedown
                (doseq [[event-key f] (filter (fn [[k v]] (= (namespace k) "event")) props)]
                  (let [event-key (name event-key)]
                    (if (#{"mousedown" "click" "mouseup"} event-key)
                      ;; use goog.events to attach mouse handlers to dom node at capture phase
                      ;; (which lets us stopPropagation and prevent CodeMirror selections)
                      (events/listen dom-node event-key f true)
                      (.on editor event-key f))))
                (when on-mount (on-mount editor this)))

              (when-let [initial-source (get-in this [:state :source] value)]
                (.setValue editor (str initial-source)))
              (when-let [local-storage-uid (first local-storage)]
                (.on editor "change" #(d/transact! [[:db/add local-storage-uid :source (.getValue %1)]])))))
          :will-receive-props
          (fn [{{next-value :value} :props
                {:keys [value]}     :prev-props
                :as                 this}]
            (when (not= next-value value)
              (when-let [editor (get-in this [:state :editor])]
                (cm/set-preserve-cursor editor next-value)
                #_(binding [*self-op* true]))))
          :will-receive-state
          (fn [{{next-source :source}   :state
                {:keys [source editor]} :prev-state}]
            (when (not= next-source source)
              (when editor
                (cm/set-preserve-cursor editor next-source)
                #_(binding [*self-op* true]))))
          :should-update
          (fn [_] false)
          :clearHighlight
          clear-highlight!
          }
         [:.h-100 {:ref "editor-container"}])

(defn viewer [source]
  (editor {:read-only? true
           :value      source}))

