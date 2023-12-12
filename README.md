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