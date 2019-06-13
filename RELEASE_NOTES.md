# Unreleased

Changes:

  * Added support for MinION output
  * Add runscanner-mystery-files.log 

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
