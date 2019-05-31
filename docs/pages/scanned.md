---
layout: default
title: Scanned Page
categories: ['User Manual']
order: 3
---
<!-- TODO: image -->
The Scanned page of Run Scanner can be accessed by clicking the 'Scanned' link on the header bar of the Run Scanner interface. This page outlines the runs detected and successfully processed by Run Scanner.

<a id="scanned-list" />
### Scanned Runs List
Each run which has been successfully scanned and processed by Run Scanner will be listed on the Scanned page. Each item in the list contains the Run Alias, which is a link to the <a href="#json">Run JSON</a>, and the path at which Run Scanner detected the run. 

<a id="json" />
### Run JSON
Clicking the Run Alias will load the Run <a href="https://en.wikipedia.org/wiki/JSON">JSON</a> for the selected run. This file is a summary of all of the information Run Scanner has retrieved from the sequencing run output. 

This file contains the same information which will be returned when accessed via the REST API.
<!-- right? --> <!-- For more information, please refer to <a href="api.html">API Docs</a>. -->

The fields within this file vary based on the model of sequencer which has produced the run. (For more information, please refer to <a href="appendices.html#B">Appendix B: Run JSON Fields</a>)

All major web browsers should support viewing JSON files interactively, <!-- right? --> however these files can also be opened in a text editor or a dedicated JSON editor application.
