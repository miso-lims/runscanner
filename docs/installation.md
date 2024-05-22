### Prerequisites

* JDK 17
* Tomcat 8 or 9
* [Maven 3.8](http://maven.apache.org/download.html) or later
* git

For Illumina support:

* C++ build environment
* jsoncpp

Please ensure that your sequencer(s) are supported by Run Scanner.
Please refer to
[Appendix C: Sequencers Supported by Run Scanner](../appendices/#appendix-c-sequencers-supported-by-run-scanner)
 for more information.

### Downloading the latest release
<!-- This will change when we have docker images -->
Use the GitHub interface to download the
[latest release](https://github.com/miso-lims/runscanner/releases/latest).
Extract the `.zip` or `.tar.gz` file to a temporary location.

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
        "parameters": {},
        "ignoreSubdirectories": [
            "/run1"
        ]
      },
      {
        "path": "/srv/sequencer/promethion",
        "platformType": "OXFORDNANOPORE",
        "name": "promethion",
        "timeZone": "America/Toronto",
        "parameters": {},
        "ignoreSubdirectories": [
            "/test/run1",
            "/run2"
        ]
      }
    ]

The ignoreSubdirectories field is used to exclude subdirectories from processing. For example, a 
known bad run or a test folder holding temporary files like /test/mytestrun. This is optional 
and in cases where ignoreSubdirectories is not included in the configuration file, all 
subdirectories under that instrument path will be processed by Run Scanner. For some examples of 
how to use this feature, check out the tests inside `SchedulerTest`.

The name/platformType combination is used to define how to interpret the
sequencer's results. A full list of instrument options can be found in
[Appendix A: Processor Definitions](../appendices/#appendix-a-processor-definitions) and a
full list of supported sequencers can be found in
[Appendix C: Sequencers Supported by Run Scanner](../appendices/#appendix-c-sequencers-supported-by-run-scanner).

### Building Run Scanner

Navigate to `$RUNSCANNER_SRC`.
Build the application using:

	mvn clean package

There will be an important build artefact:
`scanner/target/scanner-$VERSION.war`

#### Enabling Illumina scanning

If you would like to scan for Illumina output, please follow
[Illumina Setup](../illuminasetup/) before deploying.

### Deploying

1. Stop Run Scanner's Tomcat.
1. Remove `${CATALINA_HOME}/webapps/${CONTEXT}` directory and `${CATALINA_HOME}/webapps/${CONTEXT}.war` file
   (See note about `${CONTEXT}` in [Setting Up Run Scanner](../installation/#setting-up-run-scanner) above).
1. Copy the `scanner-${VERSION}.war` from the build to `${CATALINA_HOME}/webapps/${CONTEXT}.war`.
1. Start Run Scanner's Tomcat.
