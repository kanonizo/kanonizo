[![Build Status](https://travis-ci.org/kanonizo/kanonizo.svg)](https://travis-ci.org/kanonizo/kanonizo)


# Kanonizo
Kanonizo (meaning "arrange" in Greek) is an open-source framework for Test Case Prioritization. Test Case Priorization aims to re-order test cases such that
test cases that are most likely to detect faults placed prominently.

## Dependencies
Kanonizo is built in Java and is built using Maven.
- Java >= 8u172 - it is important to note that Kanonizo uses certain JavaFX packages to create a Graphical User Interface for
prioritizing test cases, so it is required that Java 8u172 or higher is installed. It is also important to note that the OpenJDK
linux package does not include the required JavaFX classes.
- Maven >= 3.0.0 - in order to build Kanonizo Maven 3 is used
- Scythe (https://github.com/thomasdeanwhite/scythe) is used for instrumenting classes on the fly to determine coverage for some
techniques
  ```
  git clone https://github.com/thomasdeanwhite/Scythe.git /path/to/scythe/dir
  cd /path/to/working/dir
  mvn clean package install
  ```
  
## Installing Kanonizo
Once the dependencies are installed, Kanonizo can be built from source
- ```
     git clone https://github.com/kanonizo/kanonizo.git /path/to/kanonizo/dir
     cd /path/to/kanonizo/dir
     mvn clean package install -DskipTests=true
  ```
- The built jar file will be available at `/path/to/kanonizo/dir/client/target/kanonizo.jar` or inside your local .m2 repository

## Using Kanonizo
Kanonizo is built inside a runnable jar file and can be executed using the following information:

Option | Required | Explanation
------ | -------- | -----------
-s <src_folder> | True | Source folder containing compiled .class java files. Kanonizo will automatically recurse through subdirectories to find all compatible files representing source code. Source code is used in Kanonizo to calculate coverage of test cases
-t <test_folder>| True | Test folder containing compiled .class test cases. Depending on the configuration Kanonizo will execute these test cases at runtime to calculate coverage or use a coverage file to work out the statements covered by test cases
-a <algorithm> | True | Algorithm to use for prioritizing test cases. These can be seen using the -listAlgorithms runtime option
-l <library1> | False | Library folder containing dependency jars. This option can be specified multiple times. Kanonizo will find any jar files in the target directory
-r <proj_root> | False | Root folder of the project. This may be needed in certain cases such as the `Schwa` algorithm, which uses file paths that are relative to the project root
-g | False | Enables a GUI for prioritizing test cases. Note that the GUI removes the need for -s, -t and -a specifications since these are handled through the GUI
-h | False | Prints help about using Kanonizo
-Dkey=value | False | Enables the use of many runtime parameters. These can be seen using the -listParameters runtime option
-listAlgorithms | False | Lists the available algorithms in Kanonizo
-listParameters | False | Lists the runtime parameters in Kanonizo

Example usage (in bash terminal):
```
kanonizo_jar=/path/to/kanonizo/dir/client/target/kanonizo.jar
cd /path/to/code/project
java -jar $kanonizo_jar -s build/classes -t build.tests -a greedy [-l lib]
```
## Kanonizo Output

During its execution, Kanonizo writes a number of log/data files that represent both the output from the program and various statistics about the runtime execution data

```
├── algorithm_time.dat       # contains the total runtime in ms of the search algorithm
├── application.log          # contains all output from the run of kanonizo
├── fitness 
│   └── <DATE_TIME>.csv      # represents the fitness values of algorithms such as Genetic Algorithm
├── ordering
│   └── <DATE_TIME>.csv      # represents the order of test cases produced by the search algorithm
├── statistics
│   └── <DATE_TIME>.csv      # contains various statistics about the algorithm execution
└── timings
    └── <DATE_TIME>.csv      # contains a bunch of timing data about various stages of the algorithm that are used for profiling
```

In most cases, the user will only be interested in the ordering file, since this is the primary reason for running the tool.
Both the directory in which data files are stored and the name of the data files can be specified on the command line using -Dlog_dir and -Dlog_filename respectively
