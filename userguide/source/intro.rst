.. _intro:



************
Preface
************


This document is made for anyone who wants to learn
constraint programming using using mini-cp as a support.

This tutorial will continuously evolve.
Don't hesitate to give us feedback or suggestion for improvement.
You are also welcome to report any mistake or bug.


What is mini-cp
=======================================
The success minisat solver has largely contributed to the dissemination of (CDCL) SAT solvers. 
Minisat has a neat and minimalist architecture that is well documented. 
We believe the CP community is currently missing such a solver that would permit new-comers to demystify the internals of CP technology. 
We introduce Mini-CP a white-box bottom-up teaching framework for CP implemented in Java. 
Mini-CP is voluntarily missing many features that you would find in a commercial or complete open-source solver. 
The implementation, although inspired by state-of-the-art solvers is not focused on efficiency but rather on readability to convey the concepts as clearly as possible.
Mini-CP is small (<1500 LOC excluding tests) and well tested.

 
Install mini-cp
=======================================

Minicp is available from bitbucket_.
The `Javavadoc API <_static/index.html>`_.

**Using an IDE**

We recommend using IntelliJ_ or Eclipse_.

From IntelliJ_ you can simply import the project.

.. code-block:: none

    Open > (select pom.xml in the minicp directory)


From Eclipse_ you can simply import the project.

.. code-block:: none

    Import > Maven > Existing Maven Projects (select minicp directory)


**From the command line**

Using maven_ command line you can do you can do:


.. code-block:: none

    $mvn compile
    $mvn test



.. _bitbucket: https://bitbucket.org/pschaus/minicp
.. _IntelliJ: https://www.jetbrains.com/idea/
.. _Eclipse: https://www.eclipse.org
.. _maven: https://maven.apache.org


Getting Help with mini-cp
=======================================

You'll get greatest chance of getting answers to your questions using the mini-cp usergroup_.

.. _usergroup: https://groups.google.com/d/forum/mini-cp

     


Who uses mini-cp
==============

If you use it for teaching or for research, please let-us know and we will add you in this list.

* UCLouvain, `INGI2365 <https://uclouvain.be/cours-2017-LINGI2365>`_ Teacher: Pierre Schaus.
* ACP, `Summer School <http://school.a4cp.org/summer2017/>`_ 2017, Porquerolles, France, Teacher: Pierre Schaus.
* Université de Nice `Master in CS <http://unice.fr/formation/formation-initiale/sminf1212>`_  Teacher: Arnaud Malapert and Marie Pelleau 


Citing mini-cp
==============

If you use find mini-cp useful for your research or teaching you can cite:

.. code-block:: latex
	
	@Misc{minicp,
	  author = "{Laurent Michel, Pierre Schaus, Pascal Van Hentenryck}",
	  title = "{Mini-CP: A Minimalist Open-Source Solver to teach Constraint Programming",
	  year = {2017},
	  note = {Available from \texttt{www.info.ucl.ac.be/~pschaus/minicp}},
	}




