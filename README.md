
[ ![Codeship Status for pschaus/minicp](https://app.codeship.com/projects/c5b42a30-bb10-0134-c1e5-0a15df6d3688/status?branch=master)](https://app.codeship.com/projects/195547)

[![codecov](https://codecov.io/bb/pschaus/minicp/branch/master/graph/badge.svg?token=zAUOtKaB64)](https://codecov.io/bb/pschaus/minicp)


# README #

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
./src/main/java/                   # the implementation of mini-cp
./src/main/java/minicp/examples/   # model examples
./src/test/java/                   # the test suite
./data/                            # input instances
```




