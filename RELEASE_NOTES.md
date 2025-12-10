# Unreleased

# 2.3.0

Changes:

  * Added consumables with LOT numbers to Illumina runs.

# 2.2.0

Changes:

  * Renamed PacBio "revio" processor to "v2"
  * Adapted PacBio v2 processor to support Vega runs
  * Fixed bad URLs in Swagger docs

# 2.1.1

Changes:

  * Added Sample PacBio Revio test directory
  * Fixed Runs not displaying for PacBio Revio Processors
  * Added custom error page

# 2.1.0

Changes:

  * Added support for parsing and serving Illumina DRAGEN BCLConvert data                            
  * Added RevioPacBioProcessor to scan PacBio Revio runs (experimental; still under development)
  * Update clang in CI from 12 to 14 
  * Fixed Run Scanner's interactive Swagger docs generating incorrect URLs in some configurations
  * Detect new RunErrored status on Novaseq X

# 2.0.1

Changes:

  * Fixed error loading Scheduled, Processing, Unreadable, and File System Error pages
  * Fixed missing Run Scanner metrics

# 2.0.0

Changes:

  * Add invalidation endpoint for individual runs
  * Tomcat 10 is now required

Known Issues:

  * Error loading Scheduled, Processing, Unreadable, and File System Error pages
  * Missing Run Scanner metrics

# 1.16.3

Changes:

  * Added back metrics for Illumina Tests
  * Added test data for NovaSeq X
  * Update tests to include sequencing kits
  * Changed Illumina metrics number format punctuation for consistency
  * Security patches

# 1.16.2

Changes:

  * Update Installation documentation to include ignore directories change
  * Added ability to exclude runs from processing using ignoreSubdirectories field in configuration file
  * Added metric to track good (readable) runs

# 1.16.1

Changes:

  * Fix build parameters for readthedocs config
  * Add metric for run directory readability

# 1.16.0

Changes:

  * NovaSeq X support - improved detection of startDate, completionDate, and containerModel

# 1.15.1

Changes:

  * Fixed detecting wrong indexSequencing for NextSeq 2000 runs

# 1.15.0

Changes:

  * Fixed detection of startDate, containerModel, and indexSequencing for NextSeq 2000 runs
  * Update illumina-interop to 1.2.0

# 1.14.0

Changes:

  * Upgrade to Java 17

# 1.13.3

Changes:

  * Added metric to track last scan start time

# 1.13.2

Changes:
  
  * Removed guava dependency
  * Update spring-beans to 5.3.18
  * Update jackson to 2.13.2

# 1.13.1

Changes:

  * Updated to log4j 2.17.1

# 1.13.0

Changes:

  * Updated to Java 11. Now requires JDK 11 to build. Tomcat also must be using Java 11.

# 1.12.5

Changes:

  * Update dependencies

# 1.12.4

Changes:

  * Fix detecting Oxford Nanopore flow cell model

# 1.12.3

Changes:

  * Detect NovaSeq index sequencing method

# 1.12.2

Changes:

  * Documentation fixes
  * Bugfixes for using ProcessRun on Nanopore runs

# 1.12.1

Changes:

  * Nanopore processor will report null if attribute is empty string

# 1.12.0

  * Log Illumina library exceptions

# 1.11.0

Changes:

  * Attempt to find ONT run status and completion time in final_summary.txt

# 1.10.1

Changes:

  * Fix Illumina scanner reporting sample IDs as pool names

# 1.10.0

Changes:

  * Fix MinION sequencing container models (was previously reporting the sequencing kit instead)
  * Added sequencing kit field for Oxford Nanopore runs
  * Show in red when configuration is bad

# 1.9.0

Changes:

  * Fix version incompatibility which made PromethION runs Unreadable
  * Get Oxford Nanopore sequencer name from files instead of configuration

# 1.8.0

Changes:

  * Export length of all reads in Illumina runs

# 1.7.0

Changes:

  * Capture HiSeq clustering type as the workflow type
  * Convert documentation to readthedocs

# 1.6.2

Changes:

 * Fixed /metrics endpoints
 * All times reported by Run Scanner are now standard datetime strings rather than arrays of integer values

# 1.6.0

Changes:

  * Added user manual and API documentation
  * Added support for MinION output
  * Add runscanner-mystery-files.log
  * Oxford Nanopore runs now contain all directory names between user-specified directory and fast5 location in run alias 
  * Fixed %\>Q30 column labels which were previously labelled as error rates, and added density units to Illumina metrics
  * Removed the Instruments page

# 1.5.0

Changes:

  * Split Oxford Nanopore MinKNOW and Protocol versions into separate fields

# 1.4.5

Changes:

  * Detects PromethION runs under directory format used at PromethION version 19.01.1: in directory structure project/sample/run/[fast5_pass|fast5_fail|etc], 'run' will be used as Run Alias
  * Introduces src/test/resources/fast5cleaner.py, which creates a new fast5 containing only the metadata needed for a test, reducing file size dramatically and protecting sequenced data. Use this to create new 'goldens' for ONT runs.
  * Renames Main to ProcessRuns for clarity

# 1.4.4

Changes:

  * Display info about why configuration is invalid

# 1.4.3

Changes:

  * Updated Oxford Nanopore DTOs
  * Added instructions for deploying to non-root context

# 1.4.2

Changes:

  * Fixed error loading Scanned runs list page

# 1.4.1

Changes:

  * Fixed deserialization of NanoporeNotificationDto
  * Use run alias instead of file name in Scanned list

# 1.4.0

Changes:

  * Add basic processor for MinION
  * Read numbers are non-indexed reads
  * Upgrade interop version
  * Improve PromethION directory scanning performance

# 1.3.0

Changes:

  * Add basic processor for PromethION
  * Change in scheduling policy to avoid spikes in runs metrics

# 1.2.4

Changes:

  * Fix detection of Illumina MiSeq chemistry
  * Add favicon
  * Link run names to DTOs and rename/reorder headers

# 1.2.3

Changes:

  * Expose NovaSeq WorkflowType
  * Return 404 (Not Found) when run does not exist
  * Export number of runs in a failed state

# 1.2.2

Changes:

  * Accept new NovaSeq output pattern
  * Handle latin1 characters in samplesheet

# 1.2.1

Changes:

  * Hide non-standard NovaSeq 'Side' values
 
# 1.2.0

Changes:

  * Report HiSeq and NovaSeq run position

# 1.1.0

Changes:

 * Update NovaSeq flowcell models
 * Add badges to README

# 1.0.1

Changes:

  * Add chemistry for NovaSeq
  * Fix README

# 1.0.0

Changes:

  * Run Scanner becomes a standalone project from MISO
