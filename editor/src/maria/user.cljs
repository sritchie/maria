(ns maria.user
  (:refer-clojure :exclude [+ - * / zero? partial]
                  :rename {})
  (:require chia.view.hiccup
            maria.friendly.messages
            [maria.friendly.kinds :refer [what-is]]
            goog.net.jsloader
            goog.crypt                                      ;; solely for `stringToUtf8ByteArray` in Shannon's Entropy lesson -- feel free to remove once we switch to a simple story for grabbing a single external dependency
            maria.user.loaders
            maria.repl-specials
            [cells.cell :as cell :refer [defcell
                                         cell
                                         with-view]]
            [cells.lib :refer [interval
                               timeout
                               fetch
                               geo-location]]
            [shapes.core :as shapes :rename {square square*}
             :refer [listen
                     circle ellipse rectangle triangle polygon polyline text image
                     position opacity rotate scale
                     colorize stroke stroke-width no-stroke fill no-fill
                     color-names colors-named rgb hsl rescale
                     layer beside above value-to-cell!
                     #_gfish
                     ;; are these internal only? -jar
                     ;;assure-shape-seq shape-bounds bounds shape->vector
                     ]]
            [cljs.spec.alpha :include-macros true]
            [cljs.spec.test.alpha :include-macros true]
            [chia.view :include-macros true]
            [applied-science.js-interop :include-macros true]
            [sicmutils.env :as e
             :refer [->infix
                     elliptic-f
                     Hamiltonian
                     integrate-state-derivative
                     Rx
                     principal-value
                     structure?
                     basis->basis-over-map
                     R3-cyl
                     Ry
                     differential
                     Gamma
                     negate
                     R2-rect
                     real-part
                     Christoffel->Cartan
                     literal-vector-field
                     log
                     R3-rect
                     up?
                     acos
                     coordinate-system->vector-basis
                     Cartan-transform
                     vector->down
                     state->t
                     velocity
                     literal-manifold-function
                     Jacobian
                     tex$$
                     coordinate-system->oneform-basis
                     invert
                     literal-manifold-map
                     compose
                     coordinatize
                     S2-Riemann
                     F->C
                     conjugate
                     Lie-derivative
                     pi
                     components->oneform-field
                     Lagrangian->state-derivative
                     polar-canonical
                     S2-spherical
                     s->m
                     commutator
                     Lagrangian->Hamiltonian
                     iterated-map
                     compositional-canonical?
                     *
                     expt
                     atan
                     down
                     linear-interpolants
                     pushforward-vector
                     Gamma-bar
                     m:dimension
                     symplectic-transform?
                     ->L-state
                     cos
                     tan
                     csc
                     S2-stereographic
                     cot
                     time-independent-canonical?
                     series
                     state-advancer
                     definite-integral
                     velocity-tuple
                     imag-part
                     column-matrix
                     simplify
                     tex$
                     sqrt
                     F->CT
                     ref
                     m:transpose
                     R2-polar
                     Rz
                     Γ
                     transpose
                     Lagrangian-action
                     Hamilton-equations
                     complex
                     -
                     R1-rect
                     osculating-path
                     matrix-by-rows
                     exp
                     alternate-angles
                     ->TeX
                     Lie-transform
                     components->vector-field
                     s->r
                     vector-field->vector-field-over-map
                     series:sum
                     zero?
                     wedge
                     determinant
                     orientation
                     D
                     Poisson-bracket
                     momentum-tuple
                     make-Christoffel
                     SO3
                     evolve
                     evolution
                     component
                     cross-product
                     coordinate-tuple
                     magnitude
                     print-expression
                     literal-oneform-field
                     /
                     angle
                     ->local
                     covariant-derivative
                     vector-basis->dual
                     basis->oneform-basis
                     ->JavaScript
                     Lagrange-equations
                     up
                     basis->vector-basis
                     asin
                     compatible-shape
                     structure->vector
                     chart
                     partial
                     +
                     Lagrange-equations-first-order
                     symplectic-unit
                     abs
                     p->r
                     vector->up
                     vector-field->components
                     Hamiltonian->state-derivative
                     Euler-Lagrange-operator
                     qp-submatrix
                     square
                     Euler-angles
                     sin
                     momentum
                     minimize
                     interior-product
                     find-path
                     ->H-state
                     sec
                     coordinate-system->basis
                     coordinate
                     form-field->form-field-over-map
                     Lagrange-interpolation-function
                     pullback
                     Lagrangian->energy
                     mapr
                     cube
                     standard-map
                     Legendre-transform
                     m->s
                     d
                     point]
             :refer-macros [literal-function with-literal-functions]]))
