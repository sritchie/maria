(ns maria.commands.blocks
  (:require [maria-commands.registry :refer-macros [defcommand]]
            [maria.blocks.blocks :as Block]
            [re-db.d :as d]))

(defcommand :eval/doc
  "Evaluate whole doc"
  {:bindings ["M1-M2-Enter"]
   :when     :block-list}
  [{:keys [block-list]}]
  (doseq [block (.getBlocks block-list)]
    (when (satisfies? Block/IEval block)
      (Block/eval block :string (Block/emit block)))))

(defcommand :block/next-block
  {:bindings ["Down"
              "Right"]
   :when     #(some-> (:block %) Block/at-end?)}
  [{:keys [block blocks]}]
  (when (Block/at-end? block)
    (some-> (Block/after blocks block)
            (Block/focus! :start))))

(defcommand :block/previous-block
  {:bindings ["Up"
              "Left"]
   :when     #(some-> (:block %) Block/at-start?)}
  [{:keys [block blocks]}]
  (when (Block/at-start? block)
    (some-> (Block/before blocks block)
            (Block/focus! :end))))

(defcommand :selection/expand
  "Expand current selection"
  {:bindings ["M1-]"
              "M1-1"
              "M1-SHIFT-UP"]
   :when     :block}
  [{:keys [block] :as context}]
  (Block/selection-expand block))

(defcommand :selection/shrink
  "Contract selection (reverse of expand-selection)"
  {:bindings ["M1-["
              "M1-2"
              "M1-SHIFT-DOWN"]
   :when     :block}
  [{:keys [block] :as context}]
  (Block/selection-contract block))

(defcommand :block/undo
  {:bindings ["M1-z"]}
  [{:keys [block-list]}]
  (.undo block-list))

(defcommand :block/redo
  {:bindings ["M1-Shift-z"]}
  [{:keys [block-list]}]
  (.redo block-list))