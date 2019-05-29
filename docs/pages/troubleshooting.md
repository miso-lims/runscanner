---
layout: default
title: Troubleshooting
categories: ['User Manual']
order: 10
---
<!-- TODO: Installation issues? -->

<a id="ProcessRun" />
### Retrieving run output
For troublesome runs, you can see the output for a particular run directory using:

    java -cp $RUN_SCANNER_HOME/WEB-INF/classes:$RUN_SCANNER_HOME/WEB-INF/lib/'*' ca.on.oicr.gsi.runscanner.scanner.ProcessRun

It will display instructions on how to use it. You will have to set the `RUN_SCANNER_HOME` to the path containing an unpacked version of the WAR.

<a id="FindRuns" />
### List observable runs
To troubleshoot whether a processor can observe a run, you can see a list of all the runs a processor will accept from the filesystem using:

    java -cp $RUN_SCANNER_HOME/WEB-INF/classes:$RUN_SCANNER_HOME/WEB-INF/lib/'*' ca.on.oicr.gsi.runscanner.scanner.FindRuns
    
It will display usage instructions. You will have to set the `RUN_SCANNER_HOME` to the path containing an unpacked version of the WAR.

<a id="SequencerInvalid" />
### Sequencing Output Directory 'Valid?' reads 'No'
To access a sequencing output directory, Run Scanner needs to be provided a valid absolute path to the directory, and the tomcat user needs to have read and execute permissions on said directory. The processor in the configuration JSON must also be specified correctly, and be provided a valid time zone.

For more information on the configuration JSON file, please refer to <a href="installation.html#setup">Setting Up Run Scanner</a>.

<!-- TODO: This messages really could be more user-friendly in Run Scanner itself -->
#### 'Path is null!'
This message appears if Run Scanner is not provided a valid absolute path to a sequencing output directory. This message will also appear if no path is specified. Check your configuration JSON for a valid absolute path to the sequencing output directory.

#### 'Path is not a directory!'
This message appears if Run Scanner does not detect that the path specified is a directory. This may happen if the path leads to a file, or if Run Scanner does not have permission to access the directory. Check that the path in the configuration JSON leads to a directory for which the tomcat user has permission to read and execute permissions. Grant those permissions if necessary.

#### 'Path cannot be read!'
This message appears if the tomcat user does not have permission to read the sequencing output directory. Ensure read and execute permissions are granted to the tomcat user on the sequencing output directory and its contents.

#### 'Path cannot be executed!'
This message appears if the tomcat user does not have execute permission for the sequencing output directory. Ensure read and execute permissions are granted to the tomcat user on the sequencing output directory and its contents.

#### 'Processor is null!'
This message appears if Run Scanner cannot construct a Processor given the information specified in the configuration JSON. Ensure that the 'name', 'platformType', and 'parameters' fields are filled out appropriately as per <a href="installation.html#setup">Setting Up Run Scanner</a> and <a href="appendices.html#A">Appendix A: Processor Definitions</a>.

#### 'TimeZone is null!'
This message appears if the 'timeZone' field in the configuration JSON is blank or invalid. This field uses TZ Database names. Please refer to <a href="https://en.wikipedia.org/wiki/List_of_tz_database_time_zones#List">List of TZ Database names on Wikipedia</a>.

<a id="Unreadable" />
### Runs appear in Unreadable list
#### Check that directory contains run data
Directories will appear in the Unreadable list if they do not contain files which Run Scanner recognizes as run data. <!-- Vague --> Ensure that the directory contains run data.

#### Verify format of run data
Run Scanner expects run data to be in certain formats per sequencer. Please refer to <a href="appendices.html#C">Appendix C: Software Versions Supported by Run Scanner</a> for more information.

It is also possible that the run data contains invalid information. Try inspecting the run files with a tool appropriate for the file type (such as _h5dump_ for Oxford Nanopore output) to ensure all data is of the appropriate data type.

#### Ensure Run Scanner has read and execute permission on run data
The tomcat user associated with the tomcat instance on which Run Scanner is being hosted must have read and execute permission for the run data in the sequencing output directory. Grant these permissions if necessary.

#### Ensure runscanner-illumina is installed
If runs from an Illumina sequencer are universally failing, ensure that runscanner-illumina is installed correctly, as outlined in <a href="illuminasetup.html">Illumina Setup</a>. Illumina runs cannot be read without this interoperability application.
