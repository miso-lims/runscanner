![CI](https://github.com/miso-lims/runscanner/actions/workflows/run_scanner_ci.yml/badge.svg)

# Run Scanner
This is a web service that monitors directories on the file system containing
the output from sequencing instruments and serves metadata for runs.

Full instructions for the setup and use of Run Scanner can be found in the [Run Scanner User Manual](https://miso-lims.readthedocs.io/projects/runscanner/en/latest).

## Prerequisites

* JDK 17
* Tomcat 8 or 9
* C++ build environment (see [runscanner-llumina](runscanner-illumina/README.md))
* [Maven 3.8](http://maven.apache.org/download.html) or later
* git

<a id="latest-release" />

## Downloading the latest release
Use the GitHub interface to download the [latest release](https://github.com/miso-lims/runscanner/releases/latest).
Extract the `.zip` or `.tar.gz` file.

<a id="setup" />

## Setting Up Run Scanner

Please refer to [Installation & Setup](https://miso-lims.readthedocs.io/projects/runscanner/en/latest/installation/) in the Run Scanner User Manual for installation instructions.

## Debugging

### Retrieving run output
For troublesome runs, you can see the output for a particular run directory using:

    java -cp $RUN_SCANNER_HOME/WEB-INF/classes:$RUN_SCANNER_HOME/WEB-INF/lib/'*' ca.on.oicr.gsi.runscanner.scanner.ProcessRun

It will display instructions on how to use it. You will have to set the `RUN_SCANNER_HOME` to the path containing an unpacked version of the WAR.

### List observable runs
To troubleshoot whether a processor can observe a run, you can see a list of all the runs a processor will accept from the filesystem using:

    java -cp $RUN_SCANNER_HOME/WEB-INF/classes:$RUN_SCANNER_HOME/WEB-INF/lib/'*' ca.on.oicr.gsi.runscanner.scanner.FindRuns
    
It will display usage instructions. You will have to set the `RUN_SCANNER_HOME` to the path containing an unpacked version of the WAR.

Please refer to [Troubleshooting](https://miso-lims.readthedocs.io/projects/runscanner/en/latest/troubleshooting/) in the Run Scanner User Manual for more information.
