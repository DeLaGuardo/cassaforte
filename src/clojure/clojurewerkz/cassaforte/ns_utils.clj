(ns clojurewerkz.cassaforte.ns-utils)

(defn defalias
  "Create a var with the supplied name in the current namespace, having the same
  metadata and root-binding as the supplied var."
  [name ^clojure.lang.Var var]
  (apply intern *ns* (with-meta name (merge (meta var)
                                            (meta name)))
         (when (.hasRoot var) [@var])))

(defn alias-ns
  "Alias all the vars from namespace to the curent namespace"
  [ns-name]
  (require ns-name)
  (doseq [[n v] (ns-publics (the-ns ns-name))]
    (defalias n v)))
