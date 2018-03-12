.. _minicp:


******************************
Exercises
******************************

We propose a set of exercises to extend mini-cp with useful features.
By doing these exercises you will gradually progress in your understanding of CP.
For each exercise, we ask you to implement JUnit tests to make sure that
your implementation works as expected.
If you don't test each feature independently you take the risk to
loose a lot of time finding very difficult bugs.


Learning Outcomes
=======================================

Be able to

* Understand reversible data structures
* Understand a domain
* Implement global constraints
* Implement custom search
* Model CP easy problems
* Use LNS
* Write unit-tests for constraints and models
* Debug constraints, models, etc




Implement the less or equal reified constraint
=================================


The file is ``*``
Check that your implementation pass the tests.



AllDifferent Forward Checking
=================================

Implement a dedicated algorithm for the all-different.
Whenever a variable is bound to a value, this value is removed from the domain of other variables.


Conflict based search branching
=================================================================



Implement a binary first fail search strategy.
You can use the ``branch`` helper method to create the array of alternatives.
You should select the unbound variable with minimum domain first.
On the left this variable is assigned to the value given by the heuristic.
On the right this same value is removed.
Capture a call to ``Store::add(new EqualVal(x,v))`` on the left branch,
and ``Store::add(new DifferentVal(x,v))`` on the right branch.


Last Conflict [LC2009]_
Conflict Ordering Search [LC2009]_


.. [LC2009] Lecoutre, C., Saïs, L., Tabary, S., & Vidal, V. (2009). Reasoning from last conflict (s) in constraint programming. Artificial Intelligence, 173(18), 1592-1614.




Discrepancy Search
=================================================================

Implement ``DiscrepancyBranching``, a branching that can wrap any branching
to limit the discrepancy of the branching.

Depth First Search
=================================================================

Replace the recursive DFS search by a non recursive implementation using an explicit stack.



Circuit Constraint
========================


Circuit Filtering [TSP1998]_

.. [TSP1998] Pesant, G., Gendreau, M., Potvin, J. Y., & Rousseau, J. M. (1998). An exact constraint logic programming algorithm for the traveling salesman problem with time windows. Transportation Science, 32(1), 12-29.


Element Constraint
========================

TODO


Cumulative Constraint
========================

Cumulative and Time-Table Filtering [TT2015]_

.. [TT2015] Gay, S., Hartert, R., & Schaus, P. (2015, August). Simple and scalable time-table filtering for the cumulative constraint. In International Conference on Principles and Practice of Constraint Programming (pp. 149-157). Springer.

Table Constraint
========================

Compact Table [CT2016]_

.. [CT2016] Demeulenaere, J., Hartert, R., Lecoutre, C., Perez, G., Perron, L., Régin, J. C., & Schaus, P. (2016, September). Compact-table: Efficiently filtering table constraints with reversible sparse bit-sets. In International Conference on Principles and Practice of Constraint Programming (pp. 207-223). Springer.

Restarts
========================

TODO

Watched Literals: The or (clause) constraint
========================

TODO


Implement a domain iterator
============================

TODO

.. Implement: The closure must be deactivated when filtering is lost
x.whenValueLost(filtering, () -> {});














  
     


