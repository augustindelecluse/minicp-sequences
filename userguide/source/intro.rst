.. _intro:



*******
Preface
*******

This document is made for anyone who wants to learn
constraint programming using using Mini-CP as a support.

This tutorial will continuously evolve.
Don't hesitate to give us feedback or suggestion for improvement.
You are also welcome to report any mistake or bug.


What is Mini-CP
===============
The success the MiniSAT solver has largely contributed to the dissemination of (CDCL) SAT solvers.
The MiniSAT solver has a neat and minimalist architecture that is well documented.
We believe the CP community is currently missing such a solver that would permit new-comers to demystify the internals of CP technology. 
We introduce Mini-CP a white-box bottom-up teaching framework for CP implemented in Java. 
Mini-CP is voluntarily missing many features that you would find in a commercial or complete open-source solver. 
The implementation, although inspired by state-of-the-art solvers is not focused on efficiency but rather on readability to convey the concepts as clearly as possible.
Mini-CP is small and well tested.


Javadoc
=======

The `Javadoc API <https://minicp.bitbucket.io/apidocs/>`_.


.. _install:

Install Mini-CP
===============

.. raw:: html

    <iframe width="560" height="315" src="https://www.youtube.com/embed/VF_vkCnOp88?rel=0" frameborder="0" allow="autoplay; encrypted-media" allowfullscreen></iframe>


Mini-CP source-code is available from bitbucket_.

**Using an IDE**

We recommend using IntelliJ_ or Eclipse_.

From IntelliJ_ you can simply import the project.

.. code-block:: none

    Open > (select pom.xml in the minicp directory and open as new project)


From Eclipse_ you can simply import the project.

.. code-block:: none

    Import > Maven > Existing Maven Projects (select minicp directory)


**From the command line**

Using maven_ command line you can do you can do:


.. code-block:: none

    $mvn compile # compile all the project
    $mvn test    # run all the test suite

Some other useful commands

.. code-block:: none

    $mvn checkstyle:checktyle   # generates a report in target/site/checkstyle.html
    $mvn findbugs:gui           # opens a gui with potential source of bugs in your code
    $mvn jacoco:report          # creates a cover report in target/site/jacoco/index.html
    $mvn javadoc:javadoc        # creates javadoc in target/site/apidocs/index.html


.. _bitbucket: https://bitbucket.org/minicp/minicp
.. _IntelliJ: https://www.jetbrains.com/idea/
.. _Eclipse: https://www.eclipse.org
.. _maven: https://maven.apache.org


Getting Help with Mini-CP
=========================

You'll get greatest chance of getting answers to your questions using the Mini-CP usergroup_.

.. _usergroup: https://groups.google.com/d/forum/mini-cp


Who uses Mini-CP
================

If you use it for teaching or for research, please let-us know and we will add you in this list.

* UCLouvain, `INGI2365 <https://uclouvain.be/cours-2017-LINGI2365>`_ Teacher: Pierre Schaus.
* ACP, `Summer School <http://school.a4cp.org/summer2017/>`_ 2017, Porquerolles, France, Teacher: Pierre Schaus.
* Université de Nice `Master in CS <http://unice.fr/formation/formation-initiale/sminf1212>`_  Teacher: Arnaud Malapert and Marie Pelleau 


Citing Mini-CP
==============

If you use find Mini-CP useful for your research or teaching you can cite:

.. code-block:: latex
	
	@Misc{minicp,
	  author = "{Laurent Michel, Pierre Schaus, Pascal Van Hentenryck}",
	  title = "{MiniCP}: A Lightweight Solver for Constraint Programming",
	  year = {2018},
	  note = {Available from \texttt{https://minicp.bitbucket.io}},
	}




