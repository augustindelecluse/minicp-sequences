.. _minicp:


************************************************************
Learn Mini-CP
************************************************************

Technical Report
=================================
The complete architecture of mini-cp is described this `document <_static/mini-cp.pdf>`_.

Slides and teaching material
=================================

Slides 

* `Tutorial <https://www.icloud.com/keynote/0E4e7B5uJ0FLuubA0pUqVp_0Q#minicp>`_ given at CPAIOR17

Slides from the course INGI2365 given by Pierre Schaus  @UCLouvain: 

* `Variables, Domains, Trail and search <https://www.icloud.com/keynote/0QTIjJ1gIxzkr0Eig7pDlYvIA#02-variables-domains-trail-search>`_
* `Views, Optimization, Heuristics and Large Neighborhood Search <https://www.icloud.com/keynote/0ua695DWVE6DqpjPvqR-St7WQ#03-views-optimization-heuristics-lns-restarts>`_
* `Domain and Bound Consistencies, Sum, Element(2D) and Circuit constraints, Incremental aspect of filtering algorithms <https://www.icloud.com/keynote/0-kSpTi0bzBdLi8kMJzLt4g2A#04-element-constraints>`_
* `Table Constraints (STR2 and Compact Table), Regular and Application to Eternity Puzzle <https://www.icloud.com/keynote/0Nr2LcZGY2xQop312SgMGs37Q#05-table-constraints>`_
* `Logical Constraints, Domain Consistent Element, Stable Mariage <https://www.icloud.com/keynote/0pRiKg20XCtBpT3prOIHuYVlw#06-stable-mariage-element-var>`_
* `Table constraints with short-tuples and negative table constraints <https://www.icloud.com/keynote/0xQmFtdqhzCFK61lit0t2a1Zw#07-short-negative-table-constraints>`_
* `Black-Box searches <https://www.icloud.com/keynote/0yqTbzWk8Qg7SJDNe9JLM8eug#08-black-box-search>`_
* `Scheduling and Cumulative Constraint <https://www.icloud.com/keynote/0I01PANBy68haEqhFDRIcvK0Q#09-cumulative-scheduling>`_
* `Scheduling and Disjunctive Constraint <https://www.icloud.com/keynote/0jR5krj0fNao6euSqBNODWPmQ#10-disjunctive-scheduling>`_
* `Modeling, Redundant Constraint, Symmetry Breaking <https://www.icloud.com/keynote/0bduxg7nHWOfdqcedJH7dNTdA#11-modeling-bin-packing>`_
* `Régin's Algorithms: Flow-based Domain Consistency for the AllDifferent <https://www.icloud.com/keynote/0hAlR-h7MWDv4LzVtrctvkBpQ#12-flow-based-constraints>`_

Learning outcomes by studying mini-cp:

From a state and inference prospective, specific learning outcomes include:

* Trailing and state reversion
* Domain and variable implementation – Propagation queue
* Arithmetic Constraints
* Logical Constraints
* Reified Constraints
* Global Constraints (including for scheduling)
* Views


From a search prospective, the outcomes include:

* Backtracking algorithms and depth first search
* Branch and Bound for Constraint Optimization
* Incremental Computation
* Variable and Value Heuristics implementation
* Searching with phases
* Large Neighborhood Search

While, from a modeling perspective, the outcomes include:

* Redundant constraints
* Bad smells and good smells: model preferably with element constraints instead of 0/1 variables
* Breaking symmetries
* Scheduling: producer consumer problems, etc.
* Design problem specific heuristics and search


******************************
Mini-CP XCSP3 Mini Solver
******************************

We provide under the form of a student project the possibility to participate to the XCSP3 MiniSolver Competition with Mini-CP.
All the interfacing with XCSP3 tools and the parsing of XCSP3 format is done for you.
You can focus on the only interesting part: make your solver as efficient as possible.

* `XCSP3 Website <http://xcsp3.org/competition>`_



