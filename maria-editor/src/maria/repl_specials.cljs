(ns maria.repl-specials
  "Special forms that exist only in the REPL."
  (:require [cljs-live.eval :as e :refer [defspecial]]
            [maria.views.repl-specials :as special-views]
            [maria.messages :as messages]
            [maria.live.ns-utils :as ns-utils]
            [clojure.string :as string]))

(defspecial dir
  "Display public vars in namespace"
  [c-state c-env ns]
  (let [ns (or ns (:ns @c-env))]
    {:value (special-views/dir c-state ns)}))

(defspecial what-is
  "Defers to maria.messages/what-is; this is only here to handle the edge case of repl-special functions."
  [c-state c-env thing]
  (e/eval-str c-state c-env (str `(maria.messages/what-is ~(cond (and (symbol? thing) (:macro (ns-utils/resolve-var c-state c-env thing)))
                                                                 :maria.kinds/macro

                                                                 (and (symbol? thing) (ns-utils/special-doc-map thing))
                                                                 :maria.kinds/special-form

                                                                 (contains? e/repl-specials thing)
                                                                 :maria.kinds/function

                                                                 :else thing)))))

(defspecial doc
  "Show documentation for given symbol"
  [c-state c-env name]
  (if-let [the-var (ns-utils/resolve-var-or-special c-state c-env name)]
    {:value (special-views/doc (merge {:expanded?   true
                                       :standalone? true}
                                      the-var))}
    {:error (js/Error. (if (symbol? name) (str "Could not resolve the symbol `" (string/trim-newline (with-out-str (prn name))) "`. Maybe it has not been defined?")
                                          (str (str "`doc` requires a symbol, but a " (cljs.core/name (messages/kind name)) " was passed."))))}))


(defspecial source
  "Show source code for given symbol"
  [c-state c-env name]
  (if-let [the-var (and (symbol? name) (ns-utils/resolve-var-or-special c-state c-env name))]
    {:value (special-views/var-source the-var)}
    {:error (js/Error. (str "Could not resolve the symbol `" (string/trim-newline (with-out-str (prn name))) "`"))}))

(defspecial inject
  "Inject vars into a namespace, preserving all metadata (inc. name)"
  [c-state c-env ns mappings]
  (let [ns (ns-utils/elide-quote ns)]
    (doseq [[inject-as sym] (seq (ns-utils/elide-quote mappings))]
      (e/eval c-state c-env `(def ~inject-as ~sym) {:ns ns})
      (swap! c-state update-in [:cljs.analyzer/namespaces ns :defs inject-as] merge (e/resolve-var c-state c-env sym)))))