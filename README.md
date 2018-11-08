[![Build Status](https://travis-ci.org/oicr-gsi/runscanner.svg)](https://travis-ci.org/Ooicr-gsi/runscanner)https://sonarcloud.io/api/project_badges/measure?project=ca.on.oicr.gsi.runscanner%3Arunscanner&metric=alert_status

# Run Scanner
This is a web service that monitors directories on the file system containing
the output from sequencing instruments and provides them to MISO.

## Prerequisites

* JDK 8
* Tomcat 8
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

Create a file called `ROOT.xml` in `$CATALINA_HOME/conf/Catalina/localhost`, creating the directory if necessary, and populate it with the following information:

    <Context>
       <Parameter name="runscanner.configFile" value="/etc/runscanner.json" override="false"/>
    </Context>

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
  
<a id="building" />

## Building Run Scanner

Navigate to `$RUNSCANNER_SRC`.
Build the application using:

	mvn clean package
	
There will be an important build artefact: `scanner/target/scanner-$VERSION.war`

<a id="illumina" />

## Enabling Illumina scanning

If you would like to scan for Illumina output, please see [runscanner-illumina/README.md](runscanner-illumina/README.md).

<a id="release" />

## Releasing 

1. Stop Run Scanner's Tomcat.
1. Remove `$CATALINA_HOME/webapps/ROOT` directory and `$CATALINA_HOME/webapps/ROOT.war` file.
1. Copy the `scanner-$VERSION.war` from the build to `$CATALINA_HOME/webapps/ROOT.war`.
1. Start Run Scanner's Tomcat.

<a id="debugging" />

## Debugging
For troublesome runs, you can see the output for a particular run directory using:

    java -cp $RUN_SCANNER_HOME/WEB-INF/classes:$RUN_SCANNER_HOME/WEB-INF/lib/'*' ca.on.oicr.gsi.runscanner.scanner.Main

It will display instructions on how to use it. You will have to set the `RUN_SCANNER_HOME` to the path containing an unpacked version of the WAR.







