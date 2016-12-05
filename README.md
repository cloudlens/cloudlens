# The CloudLens Programming Language

* [Installation](#installation)
* [Use](#use)
  * [Table](#table-processing) 
  * [Stream](#stream-processing)
  * [Working with JSON Input Files](#working-with-json-input-files)
  * [Zeppelin Notebook for CloudLens](#zeppelin-notebook-for-cloudlens)
* [Tutorial](#tutorial)
  * [A CloudLens Script](#a-cloudlens-script)
  * [State Variables](#state-variables)
  * [Blocks and Multiple Traversals](#blocks-and-multiple-traversals)
  * [Hierarchical Structures and Lenses](#hierarchical-structures-and-lenses)
  * [Lens Execution](#lens-execution)
  
  
# Installation

CloudLens requires Apache Maven (tested with version 3.3.1 and above)
and Java 8.

Before building, export JAVA\_HOME to point to a Java 8 JDK.

```
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_101.jdk/Contents/Home
```

To build, execute the following command in the `cloudlens` folder.

```
./build-notebook.sh
```

This operation may take a few minutes.

There is no need to rebuild the notebook after each repository update.
If you already have a working version of the notebook, to update
CloudLens, just execute the following command in the `cloudlens`
folder.

```
./build.sh
```

# Use

There are two execution modes: [table](#table-processing) and
[stream](#stream-processing) processing.

## Table Processing

This is the default execution mode.  CloudLens stores the entire log
history. It is thus possible to re-stream from the begining of the log
an arbitrary number of time.

This mode can be used for troubleshooting, for instance, to determine
the causes of a crash from a particular log file.

To run a lens file `file.lens` on the log file `file.log`, use the
executable `cloudlens`:

```
./cloudlens -run file.lens -log file.log
```

## Stream Processing

In streaming mode, CloudLens does not store the log history.  Hence,
executing a script requires much less memory, but it is impossible to
re-stream from the begining of the log.

This mode can be used for monitoring running systems (with a
potentially infinite log stream), to report alerting states. It can
also be useful for analysing huge log files without loading the entire
file.

If no log file is provided, CloudLens uses the standard input. It can thus
be plugged to a running system with a simple unix pipe.  For example,
to run CloudLens on system.log:

```
tail -f /var/log/system.log | ./cloudlens -run file.lens
```

To activate the streaming mode while providing a log file, use the
`-stream` option:

```
 ./cloudlens -stream -run file.lens -log file.log
```

In both modes, it is also possible to load multiple log files and
multiple lens files:

```
./cloudlens -run lens1.lens lens2.lens lens3.lens -log log1.log log2.log
```

Log files are concatenated, and lens files are loaded in
sequence.

## Working with JSON Input Files

CloudLens can handle JSON objects as input. This makes it convenient to read logs
that have been stored in popular log processing systems.

It is possible to load a JSON array instead of a file of row data.
Each element of the array become one log entry.
To load a JSON array, on the Command Line:

```
./cloudlens -format json -run file.lens -log file.json
```

It is also possible to load a JSON object and specify a path to an
array inside this object. On the Command Line:

```
./cloudlens -format json -jsonpath path.to.array -run file.lens -log file.log
```

## Javascript Librairies

On the command line, option `-js` loads a JavaScript file before executing the script.
```
./cloudlens -js file.js -run file.lens -log file.log.
```

## Zeppelin Notebook for CloudLens

CloudLens provides an extension of the Zeppelin notebook as its IDE.
To start or restart the notebook, execute the following command in the `cloudlens` folder:

```
./start-notebook.sh
```

Connect to [http://localhost:8080](http://localhost:8080) in a web browser to use the notebook.

To terminate the web server, execute the following command in the `cloudlens` folder:

```
./stop-notebook.sh
```
To load a log file into the notebook, execute the following command:
```
source("file:///path/to/file.log")
```

It is also possible to load JSON arrays (in case the log is stored as JSON) with the `json` option. 
```
source(“file:///path/to/file.log”, json)
```
To specify a path to an array inside a JSON object:
```
source(“file:///path/to/file.log”, json, path.to.array)
```

To load JavaScript libraries in the notebook, use the function CL.loadjs inside a JavaScript block:
```
 { CL.loadjs("file:///js/file.js") }
```

Any of the CloudLens scripts below can be cut and pasted, and executed in the notebook.




# Tutorial

CloudLens is a *streaming* programming language. Programs are executed on streams of JSON object constructed from an input file.
Each line of an input line becomes a JSON object with a unique field `message` that contains the line's text.
We call these objects the *entries* of the log.

We now illustrate the different functionalities of CloudLens with a log file produced by Travis.
When a user modifies the source code of a project, Travis automatically starts a test suite to make sure that the new modifications do not cause a regression in the application that's being developed.
We use the file  `log.txt`, a report generated by Travis for the OpenWhisk (http://openWhisk.org) project. 
The entire log file is located at the following address:
https://s3.amazonaws.com/archive.travis-ci.org/jobs/144778470/log.txt


## A CloudLens Script

Our first CloudLens script extracts the name of failed tests from `log.txt`.
These tests appear in the following format:
```
name > description FAILED
system.basic.WskBasicTests > Wsk Action CLI should reject
  delete of action that does not exist FAILED
```

The CloudLens script that can extract this information from `log.txt` and its result
are shown below.

```
match { "(?<failure>.*) > .* FAILED" }
stream (entry) when(entry.failure) {
   print("FAILED:", entry.failure);
}
```
Output:
```
FAILED: system.basic.WskBasicTests
FAILED: packages.UtilsTests
```

This script has two parts. For each log entry: a `match` section extract information,
and a `stream` section executed some arbitrary JavaScript code.

The `match` section adds fields to entries by using groups in Java regular expressions.
A capture group  `(?<ident>regex)` defines new fields
`ident` that contain the text corresponding to the regular expression
`regex` for each log entry.
In the previous script, the regular expression adds a field
`failure` that contains the name of the test that has failed.

The `stream` section contains arbitrary JavaScript code in which
variable `entry` contains the current log entry being processed.
The condition `when (entry.failure)` ensures this code is executed
only for entries for which `failure` is defined.
If no condition is specified, a rudimentary dependency analysis ensures
that a `stream` section is executed only when all the fields on which
it depends are defined in the entry being processed.


## State Variables

It is possible to share state variables between the different sections, like the field
`failure` in the previous example. It is also possible to share state between
different iterations of the same section.

In the  `log.txt` file the starts and ends of tests are specified in the following format:
```
Starting test *description* at *date*
Finished test *description* at *date*
```
The following script prints the description of tests that lasted more than 12 seconds.
```
var dateFormat = "# date:Date[yyyy-MM-dd' 'HH:mm:ss.SSS]";
match {
    "(?<start>Starting) test (?<desc>.*) at (?<date>.*)" + dateFormat;
    "(?<end>Finished) test (?<desc>.*) at (?<date>.*)" + dateFormat
}
var start;
stream (entry) when (entry.start) {
    start = entry.date;
}
stream (entry) when (entry.end) {
    entry.duration = entry.date - start;
    if (entry.duration > 12000) {
        print(entry.duration, "\t", entry.desc);
    }
}
```
Output:
```
13107    Actions CLI should error with a proper warning if the action exceeds its ...
14282    Wsk Activation Console should show an activation log message for hello world
15563    Wsk Activation Console should show repeated activations
31496    Util Actions should extract first n elements of an array of strings using...
```

The variable `dateFormat` specifies the shape of field `date` defined
by the two regular expressions in the `match` section.
This format expresses a complete date in milliseconds.

The `start` variable, shared between the two `stream` sections, is updated
every time the start of a test is detected, that is when the `start` field is defined.
At the end of the test, when the `end` field is defines, we add a field
 `duration` containing the the time duration of the test.
If this time is greater than 12 seconds (12000ms), we print the description of the test
`entry.desc`.


## Blocks and Multiple Traversals

Blocks allow executing an arbitrary JavaScript code only once at the beginning or at the end of the
stream.
This construct is useful for e.g., initializing a set of variables, or printing the results of an execution.
The following script counts the number of tests that have failed.

```
var failed = 0;
match { "(?<failure>.*) > .* FAILED" }
stream (entry) when (entry.failure) {
    failed++;
}
{ print(failed, "failed tests"); }
```
Output:
```
2 failed tests
```

As a convention, we rewind the stream after a block.
It is therefore possible to iterate one or more times on the same stream.
The following script prints the description of tests that took more than 10%
of the total time of testing.
```
var dateFormat = "# date:Date[yyyy-MM-dd' 'HH:mm:ss.SSS]";
match {
    "(?<start>Starting) test (?<desc>.*) at (?<date>.*)" + dateFormat;
    "(?<end>Finished) test (?<desc>.*) at (?<date>.*)" + dateFormat
}
var start;
stream (entry) when (entry.start) {
    start = entry.date;
}
stream (entry) when (entry.end) {
    entry.duration = entry.date - start;
}

var totalTime = 0;
stream (entry) when (entry.duration) {
    totalTime += entry.duration;
}
{
  print("Total Time:", totalTime/1000, "seconds");
}
stream (entry) when (entry.duration) {
    entry.prop = entry.duration*100 / totalTime;
    if (entry.prop > 10) {
        print(entry.prop.toFixed(2)+"%", entry.desc)
    }
}
```
Output:
```
Total Time: 226.309 seconds
13.92% Util Actions should extract first n elements of an array of strings using ...
```
```
Warning: time.lens line 29, implicit restart of stream log!
```
When a test has ended, we add the duration of this test `entry.duration` to the total test time
`totalTime`.
At the end of the stream,  the block prints the total test time.
We can then traverse the stream again to compute the proportion of time spent by each test:
`entry.duration / totalTime`.

CloudLens warns the user that the stream has been rewound.
This signals to the user that all of the processed entries have been kept in memory during
the execution of the program to enable rewinding and traversing the stream again.
Moreover, this warns the user that the CloudLens script cannot be used for monitoring (analyzing infinite logs).

To deactivate this warning, it is possible to rewind explicitly the stream by using the contruct
`restart log` right after the block.
It is also possible to use this instruction in the absence of a block.

## Hierarchical Structures and Lenses


It is sometimes useful to structure entries hierarchically. For
example, when a test fails, Travis prints the stack trace.  Even if
the stack trace appears on multiple consecutive lines, each line is
only one fragment of information.  The entire trace can therefore be
grouped into a single entry.  More generally, entries can span over
multiple lines.  In our example, we assume that a line starting with a
space is a fragment of the preceding entry.

```
group in messages {
    "^[^ ]"
}
```
A `group` section regroups several consecutive entries into a single one
containing an array (named `messages` in the example).
We thereby transform the stream of JSON objects containing one log line each
into a stream of objects containing an array of objects.
A new entry is initialized each time the regular expression associated with
the  `group` section is detected.
In this example, we start a new entry when we encounter a line that does not start with a space.
Each entry therefore contains all the fragments associated with it, that is,
all the following lines that start with a space.

For each failed test, we thereby construct an array containing the entire stack trace.
This trace contains a lot of information that is not useful (test libraries, external API, etc...).
Suppose we wish to filter these traces to retain only the calls associated with the source code
of OpenWhisk.

The following script defines a *lens*, meaning a function that executes a CloudLens script.
```
lens stackCleanup() {
    match {
        "at .*\((?<whisk>Wsk.*)\)";
    }
    stream (line) when (line.whisk) {
        print('    at', line.whisk)
    }
}
```
When excecuted over an entire stack trace, the `stackCleanup` lens only prints the lines
that contain the string `Wsk`, which is included in all OpenWhisk class names.

For each failed test, the following CloudLens script prints a filtered stack trace.
```
match { "(?<failure>.*) > .* FAILED" }
group in messages {
    "^[^ ]"
}
lens stackCleanup() {
    match {
        "at .*\((?<whisk>Wsk.*)\)";
    }
    stream (line) when (line.whisk) {
        print('    at', line.whisk)
    }
}
stream (entry) {
    if (entry.messages[0].failure !== undefined) {
        print("FAILED", entry.messages[0].failure);
        stackCleanup(entry.messages)
    }
}
```
Output:
```
FAILED system.basic.WskBasicTests
    at WskBasicTests.scala:295
    at WskBasicTests.scala:295
    at WskBasicTests.scala:295
    at WskBasicTests.scala:50
    at WskBasicTests.scala:50
FAILED packages.UtilsTests
    at WskTestHelpers.scala:111
    at WskTestHelpers.scala:65
```
If a test has failed, the field `failure` of the first entry of the array `entry.messages` 
is defined.
When a lens is called, an optional argument can be used to specify the stream of entries to process.
By default, lenses are executed on the current stream but this could also be an arbitrary JavaScript array.
In this example, we start by printing the name of the test, and then execute the lens 
`stackCleanup` on the array `entry.messages`.
We therefore print only the lines of the stack trace that correspond to OpenWhisk classes.


###Lens Execution

There exist two ways to execute a lens.
To illustrate these two options, let's define two lenses 
`testFailed` and `testTime` based on examples above.
The first option, which is used in the previous example, is to call the corresponding functions in a
JavaScript block.
```
{ testFailed(); testTime(); }
```
Output:
```
FAILED: system.basic.WskBasicTests
FAILED: packages.UtilsTests
13107    Actions CLI should error with a proper warning if the action exceeds its ...
14282    Wsk Activation Console should show an activation log message for hello world
15563    Wsk Activation Console should show repeated activations
31496    Util Actions should extract first n elements of an array of strings using...
```
The two lenses are then executed in sequence.
We start by streaming over the entire log to extract the names of failed tests.
Then we restream the entire log to print the description of tests that lasted more than 12 seconds.

The second option allows executing these two analyses concurrently while streaming over the log only once.
```
run testFailed()
run testTime()
```
Output:
```
13107    Actions CLI should error with a proper warning if the action exceeds its ...
FAILED: system.basic.WskBasicTests
14282    Wsk Activation Console should show an activation log message for hello world
15563    Wsk Activation Console should show repeated activations
FAILED: packages.UtilsTests
31496    Util Actions should extract first n elements of an array of strings using...
```
For each entry, we execute first `testFailed` and then `testTime`.
Compared to the previous example the results are interleaved.

