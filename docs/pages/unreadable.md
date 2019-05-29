---
layout: default
title: Unreadable Page
categories: ['User Manual']
order: 6
---

<!-- Update this as you update what the Unreadables page looks like -->

The Unreadable page of Run Scanner can be accessed by clicking the 'Unreadable' link on the header bar of the Run Scanner interface. This page lists the directories Run Scanner has identified as runs but have failed in some way to process. 

These runs may have failed due to:
* the directory not containing any valid run data files
* a run data file containing data of a format Run Scanner cannot process
* Run Scanner does not have the appropriate permissions to access the run data files
* any other interruption to Run Scanner's processing

Runs will **not** appear in the Unreadable list if they are within a sequencing output directory for which Run Scanner lacks permission to access entirely. The <a href="main.html#sequencers">Sequencing Output Directories section of the Main Page</a> will indicate when this is the case.

For more information on fixing runs which have failed to read, please refer to <a href="troubleshooting.html">Troubleshooting</a>.
For more information on processing, please refer to <a href="internal.html">Internal Operation</a>.
