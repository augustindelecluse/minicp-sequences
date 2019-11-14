*****************************************************************
Part 8: Search
*****************************************************************

*We ask you not to publish your solutions on a public repository.
The instructors interested to get the source-code of
the solutions can contact us.*

Slides
======

* `Search Heuristics <https://www.icloud.com/keynote/0yqTbzWk8Qg7SJDNe9JLM8eug#08-black-box-search>`_

Theoretical questions
=====================

* `search <https://inginious.org/course/minicp/search>`_



Discrepancy Limited Search (optional)
=================================================================

Implement ``LimitedDiscrepancyBranching``, a branching that can wrap any branching
to limit the discrepancy of the branching.

Test your implementation in `LimitedDiscrepancyBranchingTest.java. <https://bitbucket.org/minicp/minicp/src/HEAD/src/test/java/minicp/search/LimitedDiscrepancyBranchingTest.java?at=master>`_


Conflict based search strategy
=================================================================


Implement Conflict Ordering Search [COS2015]_ and Last Conflict [LC2009]_ heuristics

Test your implementation in `LastConflictSearchTest.java <https://bitbucket.org/minicp/minicp/src/HEAD/src/test/java/minicp/search/LastConflictSearchTest.java?at=master>`_
and `ConflictOrderingSearchTest.java. <https://bitbucket.org/minicp/minicp/src/HEAD/src/test/java/minicp/search/ConflictOrderingSearchTest.java?at=master>`_ .

.. [LC2009] Lecoutre, C., Saïs, L., Tabary, S., & Vidal, V. (2009). Reasoning from last conflict (s) in constraint programming. Artificial Intelligence, 173(18), 1592-1614.

.. [COS2015] Gay, S., Hartert, R., Lecoutre, C., & Schaus, P. (2015). Conflict ordering search for scheduling problems. In International conference on principles and practice of constraint programming (pp. 140-148). Springer.


Bound Impact Value Selector
=================================================================


Implement the bound impact value selector [BIVS2017]_  to discover good solutions quickly.
Test it on the TSP and compare it with random value selection.


Implement the bound-impact value selector in `TSPBoundImpact.java <https://bitbucket.org/minicp/minicp/src/HEAD/src/main/java/minicp/examplesTSPBoundImpact.java?at=master>`_
Verify experimentally that the first solution found is smaller than with the default min value heuristic.
You can also use it in combination with your conflict ordered search implementation.


.. [BIVS2009] Fages, J. G., & Prud'Homme, C. Making the first solution good! In 2017 IEEE 29th International Conference on Tools with Artificial Intelligence (ICTAI). IEEE.




