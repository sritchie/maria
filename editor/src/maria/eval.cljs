(ns maria.eval
  (:refer-clojure :exclude [macroexpand eval])
  (:require [lark.eval :as e :refer [defspecial]]
            [shadow.cljs.bootstrap.browser :as boot]
            [chia.db :as d]
            [kitchen-async.promise :as p]))

(def bootstrap-path "/js/compiled/bootstrap")

;;;;;;;;;;;;;
;;
;; Compiler state

(defonce c-state e/c-state)
(defonce c-env e/c-env)
(defonce resolve-var e/resolve-var)

;;;;;;;;;;;;;
;;
;; Block error handling

(defonce -block-eval-log (volatile! {}))

(def add-error-position e/add-error-position)

(defn handle-block-error [block-id error]
  (let [eval-log (get @-block-eval-log block-id)
        result (-> (first eval-log)
                   (assoc :error (or error (js/Error. "Unknown error"))
                          :error/kind :eval)
                   (e/add-error-position))]
    (vswap! -block-eval-log update block-id #(cons result (rest %)))
    nil))

(defonce eval-log (atom (list)))

(defn eval-log-wrap [f]
  (fn [& args]
    (let [result (apply f args)]
      (swap! eval-log conj result)
      result)))

(def eval-form*
  (eval-log-wrap (partial e/eval c-state c-env)))

(def eval-str*
  (eval-log-wrap (partial e/eval-str c-state c-env)))

(def compile-str
  (partial e/compile-str c-state c-env))

#_(defn macroexpand-n
    ([form] (macroexpand-n 1000 form))
    ([depth-limit form]
     (loop [form form
            n 0]
       (if (>= n depth-limit)
         form
         (let [expanded (ana/macroexpand-1 c-state form)]
           (if (= form expanded)
             expanded
             (recur expanded (inc n))))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Logged eval fns

(defn log-eval-result! [result]
  (let [result (assoc result :id (d/unique-id))]
    (d/transact! [[:db/update-attr :repl/state :eval-log (fnil conj []) result]])
    result))

(defn eval-str [source]
  (log-eval-result! (eval-str* source)))

(defn eval-form [form]
  (log-eval-result! (eval-form* form)))

(defn eval* [form]
  (get (eval-form* form) :value))

(defonce compiler-ready
  (delay
    (p/promise [resolve reject]
               (boot/init
                c-state
                {:path         bootstrap-path
                 :load-on-init '#{sicmutils.env
                                  maria.user
                                  cljs.spec.alpha
                                  cljs.spec.test.alpha}}
                (fn []
                  (eval-form* '(inject 'cljs.core '{what-is   maria.friendly.kinds/what-is
                                                    load-gist maria.user.loaders/load-gist
                                                    load-js   maria.user.loaders/load-js
                                                    load-npm  maria.user.loaders/load-npm
                                                    html      chia.view.hiccup/element
                                                    ->infix sicmutils.env/->infix
                                                    elliptic-f sicmutils.env/elliptic-f
                                                    Hamiltonian sicmutils.env/Hamiltonian
                                                    integrate-state-derivative sicmutils.env/integrate-state-derivative
                                                    Rx sicmutils.env/Rx
                                                    principal-value sicmutils.env/principal-value
                                                    structure? sicmutils.env/structure?
                                                    basis->basis-over-map sicmutils.env/basis->basis-over-map
                                                    R3-cyl sicmutils.env/R3-cyl
                                                    Ry sicmutils.env/Ry
                                                    partial sicmutils.env/literal-function
                                                    differential sicmutils.env/differential
                                                    Gamma sicmutils.env/Gamma
                                                    negate sicmutils.env/negate
                                                    R2-rect sicmutils.env/R2-rect
                                                    real-part sicmutils.env/real-part
                                                    Christoffel->Cartan sicmutils.env/Christoffel->Cartan
                                                    literal-vector-field sicmutils.env/literal-vector-field
                                                    log sicmutils.env/log
                                                    R3-rect sicmutils.env/R3-rect
                                                    up? sicmutils.env/up?
                                                    acos sicmutils.env/acos
                                                    coordinate-system->vector-basis sicmutils.env/coordinate-system->vector-basis
                                                    Cartan-transform sicmutils.env/Cartan-transform
                                                    vector->down sicmutils.env/vector->down
                                                    state->t sicmutils.env/state->t
                                                    velocity sicmutils.env/velocity
                                                    literal-manifold-function sicmutils.env/literal-manifold-function
                                                    Jacobian sicmutils.env/Jacobian
                                                    tex$$ sicmutils.env/tex$$
                                                    coordinate-system->oneform-basis sicmutils.env/coordinate-system->oneform-basis
                                                    invert sicmutils.env/invert
                                                    literal-manifold-map sicmutils.env/literal-manifold-map
                                                    compose sicmutils.env/compose
                                                    coordinatize sicmutils.env/coordinatize
                                                    S2-Riemann sicmutils.env/S2-Riemann
                                                    F->C sicmutils.env/F->C
                                                    conjugate sicmutils.env/conjugate
                                                    Lie-derivative sicmutils.env/Lie-derivative
                                                    pi sicmutils.env/pi
                                                    components->oneform-field sicmutils.env/components->oneform-field
                                                    Lagrangian->state-derivative sicmutils.env/Lagrangian->state-derivative
                                                    polar-canonical sicmutils.env/polar-canonical
                                                    S2-spherical sicmutils.env/S2-spherical
                                                    s->m sicmutils.env/s->m
                                                    commutator sicmutils.env/commutator
                                                    Lagrangian->Hamiltonian sicmutils.env/Lagrangian->Hamiltonian
                                                    iterated-map sicmutils.env/iterated-map
                                                    compositional-canonical? sicmutils.env/compositional-canonical?
                                                    * sicmutils.env/*
                                                    expt sicmutils.env/expt
                                                    atan sicmutils.env/atan
                                                    down sicmutils.env/down
                                                    linear-interpolants sicmutils.env/linear-interpolants
                                                    pushforward-vector sicmutils.env/pushforward-vector
                                                    Gamma-bar sicmutils.env/Gamma-bar
                                                    m:dimension sicmutils.env/m:dimension
                                                    symplectic-transform? sicmutils.env/symplectic-transform?
                                                    ->L-state sicmutils.env/->L-state
                                                    cos sicmutils.env/cos
                                                    tan sicmutils.env/tan
                                                    csc sicmutils.env/csc
                                                    S2-stereographic sicmutils.env/S2-stereographic
                                                    cot sicmutils.env/cot
                                                    time-independent-canonical? sicmutils.env/time-independent-canonical?
                                                    series sicmutils.env/series
                                                    state-advancer sicmutils.env/state-advancer
                                                    definite-integral sicmutils.env/definite-integral
                                                    velocity-tuple sicmutils.env/velocity-tuple
                                                    imag-part sicmutils.env/imag-part
                                                    column-matrix sicmutils.env/column-matrix
                                                    simplify sicmutils.env/simplify
                                                    tex$ sicmutils.env/tex$
                                                    sqrt sicmutils.env/sqrt
                                                    F->CT sicmutils.env/F->CT
                                                    ref sicmutils.env/ref
                                                    m:transpose sicmutils.env/m:transpose
                                                    R2-polar sicmutils.env/R2-polar
                                                    Rz sicmutils.env/Rz
                                                    Γ sicmutils.env/Γ
                                                    transpose sicmutils.env/transpose
                                                    Lagrangian-action sicmutils.env/Lagrangian-action
                                                    Hamilton-equations sicmutils.env/Hamilton-equations
                                                    complex sicmutils.env/complex
                                                    - sicmutils.env/-
                                                    R1-rect sicmutils.env/R1-rect
                                                    osculating-path sicmutils.env/osculating-path
                                                    matrix-by-rows sicmutils.env/matrix-by-rows
                                                    exp sicmutils.env/exp
                                                    alternate-angles sicmutils.env/alternate-angles
                                                    ->TeX sicmutils.env/->TeX
                                                    Lie-transform sicmutils.env/Lie-transform
                                                    components->vector-field sicmutils.env/components->vector-field
                                                    s->r sicmutils.env/s->r
                                                    vector-field->vector-field-over-map sicmutils.env/vector-field->vector-field-over-map
                                                    series:sum sicmutils.env/series:sum
                                                    zero? sicmutils.env/zero?
                                                    wedge sicmutils.env/wedge
                                                    determinant sicmutils.env/determinant
                                                    orientation sicmutils.env/orientation
                                                    D sicmutils.env/D
                                                    Poisson-bracket sicmutils.env/Poisson-bracket
                                                    momentum-tuple sicmutils.env/momentum-tuple
                                                    make-Christoffel sicmutils.env/make-Christoffel
                                                    SO3 sicmutils.env/SO3
                                                    evolve sicmutils.env/evolve
                                                    evolution sicmutils.env/evolution
                                                    component sicmutils.env/component
                                                    cross-product sicmutils.env/cross-product
                                                    coordinate-tuple sicmutils.env/coordinate-tuple
                                                    magnitude sicmutils.env/magnitude
                                                    print-expression sicmutils.env/print-expression
                                                    literal-oneform-field sicmutils.env/literal-oneform-field
                                                    / sicmutils.env//
                                                    angle sicmutils.env/angle
                                                    ->local sicmutils.env/->local
                                                    covariant-derivative sicmutils.env/covariant-derivative
                                                    vector-basis->dual sicmutils.env/vector-basis->dual
                                                    basis->oneform-basis sicmutils.env/basis->oneform-basis
                                                    ->JavaScript sicmutils.env/->JavaScript
                                                    Lagrange-equations sicmutils.env/Lagrange-equations
                                                    up sicmutils.env/up
                                                    basis->vector-basis sicmutils.env/basis->vector-basis
                                                    asin sicmutils.env/asin
                                                    compatible-shape sicmutils.env/compatible-shape
                                                    structure->vector sicmutils.env/structure->vector
                                                    chart sicmutils.env/chart
                                                    + sicmutils.env/+
                                                    Lagrange-equations-first-order sicmutils.env/Lagrange-equations-first-order
                                                    symplectic-unit sicmutils.env/symplectic-unit
                                                    abs sicmutils.env/abs
                                                    p->r sicmutils.env/p->r
                                                    vector->up sicmutils.env/vector->up
                                                    vector-field->components sicmutils.env/vector-field->components
                                                    Hamiltonian->state-derivative sicmutils.env/Hamiltonian->state-derivative
                                                    Euler-Lagrange-operator sicmutils.env/Euler-Lagrange-operator
                                                    qp-submatrix sicmutils.env/qp-submatrix
                                                    square sicmutils.env/square
                                                    Euler-angles sicmutils.env/Euler-angles
                                                    sin sicmutils.env/sin
                                                    momentum sicmutils.env/momentum
                                                    minimize sicmutils.env/minimize
                                                    interior-product sicmutils.env/interior-product
                                                    find-path sicmutils.env/find-path
                                                    ->H-state sicmutils.env/->H-state
                                                    sec sicmutils.env/sec
                                                    coordinate-system->basis sicmutils.env/coordinate-system->basis
                                                    coordinate sicmutils.env/coordinate
                                                    form-field->form-field-over-map sicmutils.env/form-field->form-field-over-map
                                                    Lagrange-interpolation-function sicmutils.env/Lagrange-interpolation-function
                                                    pullback sicmutils.env/pullback
                                                    Lagrangian->energy sicmutils.env/Lagrangian->energy
                                                    mapr sicmutils.env/mapr
                                                    cube sicmutils.env/cube
                                                    standard-map sicmutils.env/standard-map
                                                    Legendre-transform sicmutils.env/Legendre-transform
                                                    m->s sicmutils.env/m->s
                                                    d sicmutils.env/d
                                                    point sicmutils.env/point
                                                    #_#_macroexpand-n maria.eval/macroexpand-n}))
                  (doseq [form ['(in-ns cljs.spec.test.alpha$macros)
                                '(in-ns maria.user)]]
                    (eval-form* form))
                  (resolve))))))

(set! cljs.core/*eval* eval*)
