[![Build Status](https://travis-ci.org/oicr-gsi/runscanner.svg)](https://travis-ci.org/oicr-gsi/runscanner)[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=ca.on.oicr.gsi.runscanner%3Arunscanner&metric=alert_status)](https://sonarcloud.io/dashboard?id=ca.on.oicr.gsi.runscanner:runscanner)

# Run Scanner
This is a web service that monitors directories on the file system containing
the output from sequencing instruments and serves metadata for runs.

## Prerequisites

* JDK 11
* C++ build environment
* jsoncpp
* [Maven 3.0.5](http://maven.apache.org/download.html) or later
* git

<a id="latest-release" />

## Downloading the latest release
Use the GitHub interface to download the [latest release](https://github.com/oicr-gsi/runscanner/releases/latest).
Extract the `.zip` or `.tar.gz` file.

<a id="setup" />

## Setting Up Run Scanner

In `/etc/runscanner.json`, or another path of your choosing, put JSON data describing your instruments. You will need one record for each instrument:

    {
      "path": "/some/directory/where/sequencer/writes",
      "platformType": "ILLUMINA",
      "name": "default",
      "timeZone": "America/Toronto",
      "parameters": {}
    }

The JSON file then contains a list of instruments:

    [
      {
        "path": "/srv/sequencer/hiseq2500_1",
        "platformType": "ILLUMINA",
        "name": "default",
        "timeZone": "America/Toronto",
        "parameters": {}
      },
      {
        "path": "/srv/sequencer/hiseq2500_2",
        "platformType": "ILLUMINA",
        "name": "default",
        "timeZone": "America/Toronto",
        "parameters": {}
      }
    ]

The name/platform-type combination decide what scanner is used to interpret the sequencer's results. A list of supported scanners can be found on the status page or the debugging interface.

The parameters are set based on the processor.

- PACBIO/default requires `address` to be set to the URL of the PacBio machine.
- ILLUMINA/default optionally allows `checkOutput`. If true, the scanner will
  try to look for BCL files to verify a run is complete if no logs are present.
  If false, it will assume the run is complete if ambiguous. The default is true.
  This can be very slow on certain network file systems.

Create a directory `/srv/runscanner` for all the JARs. The build process for generating the JARs is below.

To start the server:

    java --module-path /srv/runscanner -m ca.on.oicr.gsi.runscanner.server/ca.on.oicr.gsi.runscanner.server.Server /etc/runscanner.json 8080
  
<a id="building" />

## Building Run Scanner

Navigate to `$RUNSCANNER_SRC`.
Build the application using:

	mvn clean install -pl '!oxfordnanopore'

This will build a version without support for Oxford Nanopore sequencers. See below for details.

The build artefacts can be copied to the installation directory:

    mkdir /srv/runscanner
    cp */target/*.jar /srv/runscanner
    mvn dependency:copy-dependencies -DoutputDirectory=/srv/runscanner 

<a id="illumina" />

## Enabling Illumina scanning

If you would like to scan for Illumina output, please see [runscanner-illumina/README.md](runscanner-illumina/README.md).

<a id="debugging" />

## Debugging

### Retrieving run output
For troublesome runs, you can see the output for a particular run directory using:

    java --module-path /srv/runscanner -m ca.on.oicr.gsi.runscanner.server/ca.on.oicr.gsi.runscanner.server.ProcessRun

It will display instructions on how to use it. You will have to set the `RUN_SCANNER_HOME` to the path containing an unpacked version of the WAR.

### List observable runs
To troubleshoot whether a processor can observe a run, you can see a list of all the runs a processor will accept from the filesystem using:

    java -cp /srv/runscanner -m ca.on.oicr.gsi.runscanner.server/ca.on.oicr.gsi.runscanner.server.FindRuns
    
It will display usage instructions.
