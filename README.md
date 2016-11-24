
[ ![Codeship Status for pschaus/minicp](https://app.codeship.com/projects/c5b42a30-bb10-0134-c1e5-0a15df6d3688/status?branch=master)](https://app.codeship.com/projects/195547)

[![codecov](https://codecov.io/bb/pschaus/minicp/branch/master/graph/badge.svg?token=zAUOtKaB64)](https://codecov.io/bb/pschaus/minicp)


# README #

* This repository contains the source code from Augustin Delecluse, Pierre Schaus, and Pascal Van Hentenryck. Sequence variables for routing problems.
  *In 28th International Conference on Principles and Practice of Constraint Programming (CP
  2022)*, 2022.
* The jar executable can be found in the `out/artifacts` folder
* All jar executables have been built using java 17, but can be rebuilt using java 8.
* The code in this repository has been built from MiniCP
* MiniCP technical documentation, exercises etc 
can be found at www.minicp.org
* MiniCP is a Java project build with maven https://maven.apache.org



System Requirements
-------------------

* JDK:
 1.8 or above (this is to execute Maven - it still allows you to build against 1.3
 and prior JDK's).
* Memory:
 No minimum requirement.
* Disk:
 Approximately 10MB is required for the Maven installation itself. In addition to
 that, additional disk space will be used for your local Maven repository. The size
 of your local repository will vary depending on usage but expect at least 500MB.
* Operating System:
 - Windows: Windows 2000 or above.
 - Unix based systems (Linux, Solaris and Mac OS X) and others: No minimum requirement.

Installing Maven
----------------

1. Unpack the archive where you would like to store the binaries, e.g.:
 - Unix-based operating systems (Linux, Solaris and Mac OS X)
   ```
   tar zxvf apache-maven-3.x.y.tar.gz 
   ```  
 - Windows
   ```
   unzip apache-maven-3.x.y.zip
   ```
2. A directory called `apache-maven-3.x.y` will be created.
3. Add the bin directory to your PATH, e.g.:
 - Unix-based operating systems (Linux, Solaris and Mac OS X)
   ```
   export PATH=/usr/local/apache-maven-3.x.y/bin:$PATH
   ```
 - Windows
   ```
   set PATH="v:\program files\apache-maven-3.x.y\bin";%PATH%
   ```
4. Make sure `JAVA_HOME` is set to the location of your JDK
5. Run `mvn --version` to verify that it is correctly installed.


For complete documentation, see https://maven.apache.org/download.html#Installation

CMD to run the TSPTW model
--------------------------

Solve a TSPTW instance when providing an initial solution (node ordering, omitting node `0`)
```
java -jar out/artifacts/tsptw_jar/minicp.jar -f INSTANCE_FILE -s INITIAL_SOL -t TIMEOUT
```
The timeout option is optional and default to 600s. With an example of 10s:
```
java -jar out/artifacts/tsptw_jar/minicp.jar -f data/TSPTW/instances/AFG/rbg010a.tw -s "2 3 1 4 6 5 7 8 9 10" -t 10
```

CMD to run the DARP model
--------------------------

```
java -jar out/artifacts/darp_jar/minicp.jar INSTANCE_FILE MAX_RUN_TIME VERBOSITY
```
`MAX_RUN_TIME` and `VERBOSITY` can be omitted (and default to 300 seconds and printing only the objective value)

Example of usage for an instance with 10 seconds of timeout and printing the objective value and ordering:
```
java -jar out/artifacts/darp_jar/minicp.jar data/darp/Cordeau2003/a13-144.txt 10 2 
```

CMD to run the PTP model
--------------------------

```
java -jar out/artifacts/ptp_jar/minicp.jar INSTANCE_FILE MAX_RUN_TIME
```
`MAX_RUN_TIME` can be omitted (and default to 300 seconds)

Example of usage for an instance with 10 seconds of timeout:
```
java -jar out/artifacts/ptp_jar/minicp.jar data/ptp/easy/PTP-RAND-1_36_14_144.json 10
```


CMD to Execute a model and running the test suite
--------------------------------------------------

```
 cd minicp/
 mvn compile                                              # compile all the project
 mvn exec:java -Dexec.mainClass="minicp.examples.NQueens" # execute the n-queens model
 mvn test                                                 # run all the test suite
```

Using an IntelliJ idea editor
--------------------------------------------------

We recommend IntelliJ idea https://www.jetbrains.com/idea/download

Simply do > `File | Open Project (Alt + F + O)` and specify the path to `pom.xml`
as explained here
https://blog.jetbrains.com/idea/2008/03/opening-maven-projects-is-easy-as-pie/

Content
-------------

```
./src/main/java/                        # the implementation of mini-cp
./src/main/java/minicp/engine/constraints/sequence/   # constraints on sequences
./src/main/java/minicp/engine/core/     # variables implementations (including SequenceVar)
./src/main/java/minicp/examples/darp/   # DARP models
./src/main/java/minicp/examples/ptp/    # PTP model
./src/main/java/minicp/examples/tsptw/  # TSPTW model
./src/test/java/                        # the test suite
./data/                                 # input instances
```

