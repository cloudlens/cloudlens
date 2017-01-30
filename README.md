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


# Match and Process

Our first CloudLens script extracts the name of failed tests from `log.txt`.
These tests appear in the following format:

```
name > description FAILED
```

For instance, the log contains the line:

```
system.basic.WskBasicTests > Wsk Action CLI should reject delete of action that does not exist FAILED.

```

The CloudLens script and its output are shown below.

```
match("(?<failure>.*) > .* FAILED");
process(entry) when(entry.failure) { print("FAILED:", entry.failure); }
```

Output:

```
FAILED: system.basic.WskBasicTests
FAILED: packages.UtilsTests
```

This script has two sections. A `match` section parses entries using regular expressions possibly augmenting entry objects with new fields. An `process` section executes actions implemented in JavaScript for specific entries. In this example, the `match` section identifies the log entries in the log stream with a message matching the regular expression `(?<failure>.*) > .* FAILED`. It adds to each matched entry object a field failure that contains the name of the failed test. The `process` section prints the names of the failed tests detected by the `match` section.

The `(?<failure>.*) > .* FAILED` expression uses a feature of regular expressions called a named capture group that makes it possible to identify by name a fragment of the match. Concretely it matches the same messages as the simpler expression `.* > .* FAILED` but, in addition, the substring matching the parenthesized subexpression is given the name `failure`. In general, for each capture group `(?<ident>regex)`, the match section adds a field named ident to each entry object with a matching message and sets the field’s value to the substring of message matching regex. For instance, the `match` section in our example mutates the log entry
`{message: "system.basic.WskBasicTests > ... FAILED"}` to `{message: "system.basic.WskBasicTests > ... FAILED", failure: "system.basic.WskBasicTests"}`.

The `process` section applies a function to selected log entries in the log stream. The function is specified as a block of JavaScript code which takes the log entry as its single argument. The parameter name of the function is specified in parentheses after the `process` keyword and the activation condition is specified in parentheses after the `when` keyword. In this example, the log entry is bound to the name `entry` and the function is to be executed only on log entries for which the field `failure` is defined. In general, if no condition is specified, a rudimentary dependency analysis ensures that a `process(entry)` section is executed only when all the fields on which it depends are defined in the entry being processed. In this example, the condition could have been omitted and we could write:

```
process(entry) { print("FAILED:", entry.failure); }
```

The following shorthand are also legal:

```
when(entry.failure) { print("FAILED:", entry.failure); }
```

In this form, the parameter name of the function is inferred from the condition. A comma- separated list of conditions is interpreted as a conjunction of conditions.

While `process ... when` is a primitive construct of CloudLens, `match` is a built-in *lens*, i.e., a predefined CloudLens macro. We discuss lenses in Section 3.6.

# Variables

CloudLens is an imperative programming language. Scripts can mutate the log stream, like adding a failure field in the previous example, or declare and mutate variables. The `log.txt` file reports the beginning and the end of a test in the following format:

```
Starting test *description* at *date*
Finished test *description* at *date*
```

The following script identifies tests that lasted more than 12 seconds.

```
match("Starting test (?<desc>.*) at (?<start:Date[yyyy-MM-dd' 'HH:mm:ss.SSS]>.*)");
match("Finished test (?<desc>.*) at (?<end:Date[yyyy-MM-dd' 'HH:mm:ss.SSS]>.*)");
var start;
when(entry.start) { start = entry.start; }
when(entry.end) {
    entry.duration = entry.end - start;
    if(entry.duration > 12000) { print(entry.duration, "\t", entry.desc); }
}
```

Output:

```
13107  Actions CLI should error with a proper warning if the action exceeds its ...
14282  Wsk Activation Console should show an activation log message for hello world
15563  Wsk Activation Console should show repeated activations
31496  Util Actions should extract first n elements of an array of strings using...
```

This script leverages an extension of regular expressions to specify the expected type and format of a capture group `(?<ident:type[format]>regex)`. E.g., the field `start` has the type `Date` and the expected format in the log is `yyyy-MM-dd HH:mm:ss.SSS`.

The `start` variable is mutated every time the beginning of a test is detected, that is when the `start` field is defined. At the end of the test, when the end field is defined, we add a `duration` field to the log entry. If this duration is greater than 12 seconds (12000ms), we print the description of the test `entry.desc`.

# Order of Execution

CloudLens is a *data-flow* language. A CloudLens script describes a pipeline of stages that each log entry has to go through. One log entry flows through the entire script before processing starts on the next entry. For each entry, sections are executed in program order. In particular, concatenating the scripts of the previous sections combines their analyses into a single processing pass, interleaving their outputs:

```
13107    Actions CLI should error with a proper warning if the action exceeds its ...
FAILED: system.basic.WskBasicTests
14282    Wsk Activation Console should show an activation log message for hello world
15563    Wsk Activation Console should show repeated activations
FAILED: packages.UtilsTests
31496    Util Actions should extract first n elements of an array of strings using...
```

# Finite Streams

CloudLens is designed to handle both finite and infinite streams. Live logs often embed finite sublogs that are key to application monitoring, e.g., transaction logs in a database log. The `after` section makes it possible to execute actions at the end of the log stream. For instance, we can count the number of failed tests as we detect them and report the final count.

```
var failed = 0;
when(entry.failure) { failed++; }
after { print(failed, "failed tests"); }
```

Output:

```
2 failed tests
```

Obviously the `after` section is never executed if the log is infinite. Like `process` sections, `after` sections are executed in program order. Conceptually, a special *EndOfStream* entry follows the last log entry, triggering `after` sections instead of `process` sections.

# Return

By default, a section does not add, remove, or replace entries in the stream. It is possible to do so using `return` statements: `return e;` replaces the current entry with `e` whereas `return;` removes the current entry. A section may also insert multiple entries into the stream by returning an array. For instance, the following script prunes entries with empty messages, i.e., blank log lines, and appends a couple of log entries at the end of the log.

```
when(entry.message) { if(entry.message.length == 0) return; }
after { return [{message: failed + " failed tests"}, {message: "THE END"}]; }
```

If no `return` statement gets executed, the entry being processed remains in the log, hence there is no need for an `else` branch in the if statement above.

# Lenses

CloudLens scripts can encapsulate processing into *lenses*. Lenses are declared using the `lens` keyword followed by the name of the lens, the parameter list within parentheses, and the body within curly braces. The body of a lens has the same structure as a CloudLens script. For instance, the following lens makes it possible to implement multi-pass analyses by first buffering then replaying the (finite) stream.

```
lens rewind() {
    var stream = [];
    when(entry) { stream.push(entry); return; } // store entry, suppress from the log
    after { return stream; } // replay the log
}
```

We can use the `rewind` lens to print the description of tests that took more than 10% of the total time.

```
var totalTime = 0;
when(entry.duration) { totalTime += entry.duration; }
after { print("Total Time:", totalTime/1000, "seconds"); }
rewind();
when(entry.duration) {
    entry.prop = entry.duration * 100 / totalTime;
    if(entry.prop > 10) { print(entry.prop.toFixed(2) + "%", entry.desc); }
}
```

Output:

```
Total Time: 226.309 seconds
13.92% Util Actions should extract first n elements of an array of strings using ...
```

In a first traversal of the log, we compute the total test time `totalTime`. We then traverse the log again to compute the proportion of time spent by each test.

Lenses can be viewed as macros but a variable declared inside of a lens is scoped to the lens, e.g., in this example the stream variable is not visible outside of the `rewind` lens, and multiple instantiations of the lens get their own copy of the variable.

# Structured Logs

So far we have been processing `log.txt` one line at a time. But often logs have some structure. Modern logging frameworks produce structured log entries instead of simple text. Log messages may be split across multiple lines, etc. Navigating these structures is easy with CloudLens.

In our example log, Travis includes a stack trace for each failed test. Stack traces range over many lines but they logically belong to the most recent error message. Stack traces are recognizable by their non-zero indentation. In order to rebuild the logical structure of the log, we define a `group` lens that appends a log entry to the previous one if it matches a specific regular expression. More precisely, it combines the log entries matching the regular expression into a `group` array that gets embedded into most recent unmatched entry.

```
lens group(regex) {
    var current = null;
    match("(?<partOfGroup>)" + regex);
    when(entry.partOfGroup) { // entry is part of group
        delete entry.partOfGroup; // remove helper tag
        if(current !== null) { current.group.push(entry); }
        return; // suppress entry
    }
    when(entry) { // entry is not part of group
        var last = current;
        current = entry;
        current.group = [];
        return last;
    }
    after {
      if(current !== null) return current;
    }
}
```

Using the `group` lens, we can look for indented log lines and build structured log entries.

```
group("^\s");
```

The log entries of interest are now of the form:

```
{message: "error message", group: [{message: " stack trace element"}, {message: " stack trace element"}, ...]}
```

Theses traces however have a lot of noise. Suppose we wish to filter the traces to retain only the method calls associated with the OpenWhisk source code. We can define a filter lens and invoke it on the group array. For each failed test, the following CloudLens script prints a filtered stack trace.

```
lens filter() {
    match("at .*\((?<whisk>Wsk.*)\)");
    when(line.whisk) { print('    at', line.whisk); }
}
when(entry.group, entry.failure) {
    print("FAILED", entry.failure);
    filter(entry.group);
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

In general, lenses can be invoked (1) at the toplevel or (2) inside of a process or after section. In the first case as illustrated with the `group` invocation, the lens is implicitly applied to the log stream. In the second, the array argument must be specified explicitly, as shown with the `filter` example. In both cases, the lens gets to process a stream of entries, either the log stream or the streamed array content.
