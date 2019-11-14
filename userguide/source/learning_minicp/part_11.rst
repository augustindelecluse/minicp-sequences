*****************************************************************
Part 11: Modeling
*****************************************************************

*We ask you not to publish your solutions on a public repository.
The instructors interested to get the source-code of
the solutions can contact us.*

Slides
======

* `Modeling <https://www.icloud.com/keynote/0bduxg7nHWOfdqcedJH7dNTdA#11-modeling-bin-packing>`_

Theoretical questions
=====================

* `Modeling <https://inginious.org/course/minicp/modeling>`_



The logical or constraint and watched literals
=======================================================


* Implement the constraint `Or.java <https://bitbucket.org/minicp/minicp/src/HEAD/src/main/java/minicp/engine/constraints/Or.java?at=master>`_
  for modeling the logical clause constraint: `(x[0] or x[1] or x[2] ... x[n-1])`.
* Test your implementation in `OrTest.java. <https://bitbucket.org/minicp/minicp/src/HEAD/src/test/java/minicp/engine/constraints/OrTest.java?at=master>`_
* The implementation should use the watched literals technique.


A reminder about the watched literals technique:

*  The constraint should only listen to the changes of two unbound variables with `propagateOnBind(this)`
  and dynamically listen to other ones whenever of these two become bound. Keep in mind that
  any call to `x[i].propagateOnBind(this)` has a reversible effect on backtrack.
* Why two ? Because as long as there is one unbound one, the constraint is still satisfiable and nothing need to be propagated
  and whenever it is detected that only one is unbound and all the other ones are set to false,
  the last one must be set to true (this is called unit propagation in sat-solvers).
* The two unbound variables
  should be at indexes `wL` (watched left) and `wR` (watched right).
  As depicted below `wL` (`wR`) is the left (right) most unbound variable.
* Those indices are store in `ReversibleInt` such that they can only increase during search (incrementality).
* When `propagate` is called, it means that one of the two watched variable is bound (`x[wL] or x[wR]`) and
  consequently the two pointers must be updated.
* If during the update a variable bound to `true` is detected, the constraint can be deactivated since it will always be satisfied.


.. image:: ../_static/watched-literals.svg
    :scale: 50
    :width: 600
    :alt: watched literals


The logical reified or constraint
=======================================================


* Implement the constraint `IsOr.java <https://bitbucket.org/minicp/minicp/src/HEAD/src/main/java/minicp/engine/constraints/IsOr.java?at=master>`_
  for modeling the logical clause constraint: `b iff (x[0] or x[1] or x[2] ... x[n-1])`.
* Test your implementation in `IsOrTest.java. <https://bitbucket.org/minicp/minicp/src/HEAD/src/test/java/minicp/engine/constraints/IsOrTest.java?at=master>`_
* In case `b` is true, you can post your previous `Or` constraint
(create it once and forall and post it when needed to avoid creating objects during search that would trigger Garbage Collection).


Steel Mill Slab Problem: Modeling, redundant constraints and symmetry breaking
======================================================================================

A number of TODO must be completed in `Steel.java <https://bitbucket.org/minicp/minicp/src/HEAD/src/main/java/minicp/examples/Steel.java?at=master>`_
that will gradually improve the performance for solving this problem optimally.

1. Model the objective function computing the total loss to be minimized. You should use element constraints to compute the loss
   in each slab. The precomputed array `loss` gives for each load (index) the loss
   that would be induced. It is precomputed as the difference between the smallest capacity that can accommodate
   the load and the load value. A sum constraint constraint can then be used to compute the total loss.

2. Model a boolean variable reflecting the presence or not of each color in each slab.
   The color is present if at least one order with such color is present.
   The `IsOr` constraint previously implemented can be used for that.
3. Restrict the number of colors present in slab j to be <= 2.
   Your model can now be run, although it will not be able to solve optimally yet the easiest instance `data/steel/bench_20_0`.
4. Add a redundant constraint for the bin-packing stating that sum of the loads is equal to the sum of elements.
   Do you observe an improvement in the solving complexity ?
5. Add static symmetry breaking constraint. Two possibilities: the load of slabs must be decreasing or the losses must be decreasing.
   Do you observe an improvement in the solving complexity ?
6. Implement a dynamic symmetry breaking during search. Select an order `x` representing the slab where this order is placed.
   Assume that the maximum index of a slab containing an order is m.
   Then create m+1 branches with x=0,x=1,...,x=m,x=m+1 since all the decisions x=m+2,x=m+3 ... would subproblems symmetrical with x=m+1.
   You should now be able to solve optimally the instance 'data/steel/bench_20_0' reaching a zero loss solution.


