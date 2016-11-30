# The CloudLens programming language

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

