<!-- TODO: image -->
The Scanned page of Run Scanner can be accessed by clicking the 'Scanned'
link on the header bar of the Run Scanner interface. This page outlines the
runs detected and successfully processed by Run Scanner.

### Scanned Runs List
Each run which has been successfully scanned and processed by Run Scanner
will be listed on the Scanned page. Each item in the list contains the Run
Alias, which is a link to the Run JSON (see below), and the path at
which Run Scanner detected the run.

### Run JSON
Clicking the Run Alias will load the Run
[JSON](https://en.wikipedia.org/wiki/JSON) for the selected run.
This file is a summary of all of the information Run Scanner has retrieved
from the sequencing run output.

This file contains the same information which will be returned when accessed
via the REST API.
<!-- right? --> <!-- For more information, please refer to [API Docs](/api/). -->

The fields within this file vary based on the model of sequencer which has
produced the run. (For more information, please refer to
 [Appendix B: Run JSON Fields](/appendices/#appendix-b-run-json-fields))

All major web browsers should support viewing JSON files interactively,
<!-- right? --> however these files can also be opened in a text editor or
a dedicated JSON editor application.
