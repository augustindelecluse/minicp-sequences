.. _minicp:


*************
Learn Mini-CP
*************

This tutorial is based on the course "LINGI2365 Constraint Programming" given at UCLouvain by Pierre Schaus.

.. toctree::
        :maxdepth: 2

        learning_minicp/part_1
        learning_minicp/part_2
        learning_minicp/part_3
        learning_minicp/part_4
        learning_minicp/part_5
        learning_minicp/part_6
        learning_minicp/part_7
        learning_minicp/part_8
        learning_minicp/part_9
        learning_minicp/part_10
        learning_minicp/part_11

Outcomes
========

Learning outcomes by studying Mini-CP:

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


Technical Report
================

The complete architecture of Mini-CP is described this `document <_static/mini-cp.pdf>`_.


Mini-CP XCSP3 Mini Solver
=========================

We provide under the form of a student project the possibility to participate to the XCSP3 MiniSolver Competition with Mini-CP.
All the interfacing with XCSP3 tools and the parsing of XCSP3 format is done for you.
You can focus on the only interesting part: make your solver as efficient as possible.

* `XCSP3 Website <http://xcsp3.org/competition>`_



