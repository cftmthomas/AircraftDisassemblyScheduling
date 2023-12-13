# Aircraft Disassembly Scheduling

This repository contains the code, data and experiments results presented in the paper "A Constraint Programming Approach for Aircraft Disassembly Scheduling" submitted at the conference CPAIOR 2024.

## Content

The repository is organized as such:

- The **data** folder contains the anonymized instance used for the experiments. See the **Instances** section for more details about the instance files structure.
- The experimental results are contained in the **results** folder. See the corresponding section for more details.
- The **src** folder contains the source code. It is organized as such:
	- The **cpoptimizer** package contains the CP Optimizer model as well as its relaxed version and utility classes.
	- The **datamodel** package contains all the data objects that are manipulated in the model.
	- The **tools** package contains several utility classes.
	- The **visu** package contains all the visualisation classes.

## Instances

Instances are encoded in json files. An instance file contains the following fields:

- `id` contains a unique identifier string unique to the instance.
- `name` contains the name of the instance.
- `version` contains a version number for the instance format.
- `maxTime` contains an integer indicating the global time horizon H.
- `balanceAF` and `balanceLR`contain integers corresponding to the maximum difference of mass allowed on the two balance axis
- `resources` contains an ordered list of resources available for the tasks. Each resource has the following fields:
	- `id` is a unique integer id corresponding to the index of the resource.
	- `name` contains the name of the resource.
	- `category`corresponds to the category of the resource encoded as a String.
	- `unavailable` is a list of time windows corresponding to the periods when the resource is unavailable. Each time window is encoded as a tuple of two integers corresponding to the start and end of the unavailablity period.
	- `cost` is an integer that encodes the cost per time period of the resource.
- `locations` contains an ordered list of locations. Each location contains the following fields:
	- `id` a numerical unique id corresponding to the index of the location.
	- `name` the name of the location.
	- `zone` the zone in which this location is situated. For locations that are not situated in one of the 4 balance zones, this field may either contain the value "CENTER" or be empty.
	- `capacity` the capacity of this location that corresponds to the maximum number of technicians that can work there at the same time.
- `operations` contains an ordered list of operations. Each operation has the following fields:
	- `id` a unique numerical id corresponding to the index of the operation in the list.
	- `name` the name of the operation
	- `card` an ATA jobcard number encoded as a string. For anonymous instances, it is replaced by the id.
	- `duration` the duration of the operation.
	- `location` a location id that refers to the location where the operation take place.
	- `occupancy` the occupancy which corresponds to the total number of technicians that are needed for the operation.
	- `mass` the mass removed during the operation.
	- `resources` a list of requirements that are needed for the operation. Each requirement consists in two fields:
		- `category`which contains a list of categories that are allowed for this requirement.
		- `quantity` a quantity of resources needed for this requirement.
	- `precedences` contains a list of operation ids correponding to the preceeding operations that must be finished before starting this operation.

There are 19 instances. The instance **737-600-Full-Anon** is the full instance that contains the whole set of operations. All the other instances contain a subset of operations.

## Results

Experiments results are separated into two folders:

- **LexicographicalSearch** contains the results of the lexicographical search on the whole set of instances. It is further divided into two folders:
	-  **Auto** which contains the results for the lexicographical search on the makespan objective first.
	-  **ILEX-AUTO** which contains the results for the inverted lexicographical search on the cost objective first.
- **Relaxed** contains the results of the runs with the relaxed model.

For all folders, the results are separated into two kind of files: `Solutions` and `Logs`. Solution files contain the final best solution found at the end of the search. Log files contain the search log that records the evolution of the search. These files are also json files. They have the following format:

- `Solution` files contain the following fields:
	- `instance` contains the whole instance that the solution solves.
	- `activities` is a list indicating how each operation is scheduled. Each entry in the list correspond to an operation. It contains the following fields:
		- `operation` which is an operation id.
		- `start` which indicates the time at which the operation starts.
		- `end` which indicates the time at which the operation ends.
	- `assignments` is a list of all the assignations during the planing period. Each assignation is characterised by the following fields:
		- `resource` the resource which is assigned.
		- `operation` the operation at which the resource is assigned.
		- `requirement` the requirement of the operation that the assignation satisfys.
		- `start` the start of the assignation.
		- `end` the end of the assignation.
- `Log` files contain the following fields:
	- `instance` a string that containts the name of the instance on which the run was done.
	- `makespanBound` contains the lower bound found for the makespan objective encoded as an integer.
	- `costBound` contains the lower bound found for the cost objective encoded as an integer.
	- `log` is an ordered list that contains the history of the search in terms of solutions found. Each entry in the list correspond to a new solution during the search. Entries have the following fields:
		- `time` which indicates the search time at which the solution has been found.
		- `makespan` which contains the makespan of the solution.
		- `cost` which contains its cost.
		- `optimal` which is a boolean indicating if the solver has proven the solution optimal.

In addition to the solution and log files for each instances, both results folders contain a series of csv files that contain general statistics computed based on the solution and log files.

## How to run

Before compiling or running the code, you must install [CP Optimizer 22.1.1](https://www.ibm.com/docs/en/icos/22.1.1?topic=cp-optimizer) or a subsequent version. Follow the installation instructions bundled with the download to set up correctly the program on your machine. Do not forget to set up the correct environmental variables.

The project uses [maven](https://maven.apache.org/) to manage the libraries needed for the model. Make sure that maven is installed on your machine and that your local maven repository is correctly configured into the `settings.xml` file which should be located in `USER_HOME/.m2`. This should look like this:

```xml
<localRepository>
	path/to/your/local/repository
</localRepository>
```

By default the local repository is `${user.home}/.m2/repository`. If needed, you can find more information on repositories and how to configure them in maven [here](https://maven.apache.org/guides/introduction/introduction-to-repositories.html) and [here](https://maven.apache.org/configure.html).

In order to make the CP Optimizer libraries available for maven, you need to import the CP Optimizer jar into your local maven repository. To do so, use the [maven install plugin](https://maven.apache.org/guides/mini/guide-3rd-party-jars-local.html) to import the CP Optimizer jar which is situated at `CPLEX_INSTAll_DIRECTORY/cpoptimizer/lib/ILOG.CP.jar` by running the following command:

```bash
mvn install:install-file -Dfile=<path-to-jar> -DgroupId=cplex -DartifactId=cpoptimizer -Dversion=22.1.1 -Dpackaging=jar
```

You may change the groupId, artifacId or version values but in this case, you will need to edit the `pom.xml` file of the project to point to the correct artifact.

Once these steps are done, you can compile the project and run the model either in your favourite IDE or via maven by using the commands `mvn compile` and `mvn exec:java Dexec.args="arguments to the program"`. Depending on your system or if you are using an IDE, you may have to configure environment variables, add elements to the path or pass additional arguments to make the CP Optimizer library properly recognized at runtime. Refer to the CP Optimizer documentation or your IDE documentation to do so.

There are two classes that can be used to launch the model. The `Launcher.java` class simply launches to model while the `visu/runnable/Visu.java` class launches the model then displays a visualisation of the last found solution at the end of the search. Both classes accept the same format of arguments:

```bash
<path/to/instance> <model> [options]
```

Possible options are:

- `-st` a flag that indicates that a solution file must be used as starting point for the search. In this case, the `<path/to/instance>` argument must point to a solution file instead of an instance file.
- `-t <time-limit>` sets the time limit (in seconds) of the search (first phase in case of lexicographical search).
- `-t2 <time-limit>` is used to set a time limit for the second phase of a lexicographical search.
- `-f <fail-limit>` sets the fail limit of the search.
- `-s <search>` allows to set a specific kind of search for the model. Accepted values are:

	- **LEX-DF** lexicographical search with a depth first search.
	- **LEX-FD** lexicographical search with a failure directed search.
	- **ILEX-AUTO** inverted lexicographical search (on cost objective first) with cp optimizer's auto search.
	- **ILEX-DF** inverted lexicographical search with a depth first search.
	- **ILEX-FD** inverted lexicographical search with a failure directed search.
	- **MK-AUTO** search on the makespan only with CP Optimizer's auto search.
	- **MK-DF** search on the makespan only with depth first search.
	- **MK-FD** search on the makespan only with a failure directed search.
	- **CST-AUTO** search on the cost only with CP Optimizer's auto search.
	- **CST-DF** search on the cost only with a depth first search.
	- **CST-FD** search on the cost only with a failure directed search.
	
	The search set by default is a lexicographical search with CP Optimizer's auto search.
- `-n <n-workers>` sets the number of workers that are used in parralel for the CP Optimizer search. The default value is 1.
- `-out <output/path>` sets the output path which correponds to the folder where the log and solution files will be written at the end of the search.

The `<model>` argument is mandatory and indicates which model will be run. Its possible values are:

- **CPOOptInterModel** the CP Optimizer model.
- **CPOOptInterModelRelax** the relaxed model without the balance, capacity and certification constraints.
- **DisplaySol** (for the Visu class only) displays the visualisation for a given solution file. In this case the `<path/to/instance>` argument must point to a solution file instead of an instance file.