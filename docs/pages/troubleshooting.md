---
layout: default
title: Troubleshooting
categories: ['User Manual']
order: 10
---
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
### Sequencer 'Valid?' reads 'No'
<!-- TODO: Write explanation -->
