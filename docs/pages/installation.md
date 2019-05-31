---
layout: default
title: Installation & Setup
categories: ['User Manual']
order: 1
---
<a id="prerequisites" />
### Prerequisites

* JDK 8
* Tomcat 8
* [Maven 3.0.5](http://maven.apache.org/download.html) or later
* git

For Illumina support:

* C++ build environment
* jsoncpp

Please ensure that your sequencer(s) are supported by Run Scanner. 
Please refer to <a href="appendices.html#C">Appendix C: Sequencers Supported 
by Run Scanner</a> for more information.

<a id="latest-release" />

### Downloading the latest release
<!-- This will change when we have docker images -->
Use the GitHub interface to download the 
[latest release](https://github.com/miso-lims/runscanner/releases/latest).
Extract the `.zip` or `.tar.gz` file to a temporary location.

<a id="setup" />

### Setting Up Run Scanner

In the following instructions:
* Let `${CATALINA_HOME}` be the directory where Tomcat is installed.
* Let `${CONTEXT}` be the context URL you wish to use for Run Scanner. 
  * To deploy to the root context (e.g., https://www.myrunscanner.org), let `${CONTEXT}` be `ROOT`
  * To deploy to a subdirectory (e.g., https://www.myserver.org/runscanner/), let `${CONTEXT}` be the name of the subdirectory (e.g., `runscanner`)

Create a file called `${CONTEXT}.xml` in 
`${CATALINA_HOME}/conf/Catalina/localhost`, creating the directory if 
necessary, and populate it with the following information:

    <Context>
       <Parameter name="runscanner.configFile" value="/etc/runscanner.json" override="false"/>
    </Context>

`/etc/runscanner.json` is the default location of the instrument 
descriptions, however this can be changed if necessary. Create this file, 
and within, write JSON data describing your instruments. Run Scanner 
requires one record per instrument, therefore the file will contain a 
list of instruments:

    [
      {
        "path": "/srv/sequencer/hiseq2500_1",
        "platformType": "ILLUMINA",
        "name": "default",
        "timeZone": "America/Toronto",
        "parameters": {}
      },
      {
        "path": "/srv/sequencer/promethion",
        "platformType": "OXFORDNANOPORE",
        "name": "promethion",
        "timeZone": "America/Toronto",
        "parameters": {"name": "promethion_001"}
      }
    ]

The name/platformType combination is used to define how to interpret the 
sequencer's results. A full list of instrument options can be found in 
<a href="appendices.html#A">Appendix A: Processor Definitions</a> and a 
full list of supported sequencers can be found in 
<a href="appendices.html#C">Appendix C: Sequencers Supported by Run Scanner</a>. 
 
<a id="building" />

### Building Run Scanner

Navigate to `$RUNSCANNER_SRC`.
Build the application using:

	mvn clean package
	
There will be an important build artefact: 
`scanner/target/scanner-$VERSION.war`

<a id="illumina" />

#### Enabling Illumina scanning

If you would like to scan for Illumina output, please follow 
<a href="illuminasetup.html">Illumina Setup</a> before deploying.

<a id="deploying" />

### Deploying

1. Stop Run Scanner's Tomcat.
1. Remove `${CATALINA_HOME}/webapps/${CONTEXT}` directory and `${CATALINA_HOME}/webapps/${CONTEXT}.war` file
   (See note about `${CONTEXT}` in "<a href="#setup">Setting Up Run Scanner</a>" above).
1. Copy the `scanner-${VERSION}.war` from the build to `${CATALINA_HOME}/webapps/${CONTEXT}.war`.
1. Start Run Scanner's Tomcat.
