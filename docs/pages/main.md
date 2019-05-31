---
layout: default
title: Main Page
categories: ['User Manual']
order: 2
---

<!-- TODO: Add image without any identifiable info in it -->

The Main Page of Run Scanner is accessed at the root of Run Scanner's URL 
and can be accessed from anywhere in the Run Scanner interface by clicking 
the bold 'Run Scanner' link on the menu bar at the top of the screen.

<a id="core" />
### Core
This section provides information about the current state of Run Scanner, 
including uptime, whether Run Scanner's current configuration is valid, and 
whether Run Scanner is currently in the process of scanning for new run 
information. 

Upon first starting Run Scanner, the 'Time Since Last Scan' item will read 
'Starting up...'. Run Scanner may take some time to start up before scanning. 
This wait time is a function of the number of sequencing output directories
 defined in the configuration JSON, whether certain parameters are enabled, 
and whether the sequencing output directories must be accessed over a network.
 <!-- Is this accurate? -->

Run Scanner will scan for new runs every 15 minutes. 'Currently Scanning' 
may read 'No' while 'Waiting Runs' is still greater than zero. This is normal,
 and due to the fact that 'Scanning' and 'Processing' are considered separate 
tasks by Run Scanner. Run Scanner will continue processing runs after scanning
 for new runs has completed. 

<a id="sequencers" />
### Sequencing Output Directories
Underneath the <a href="#core">Core</a> section, Run Scanner displays one 
section per sequencing output directory, as defined in the configuration 
JSON (for more information, please refer to 
<a href="installation.html#setup">Setting Up Run Scanner</a>). Each section 
is headed by the directory path, and outlines the platform, processor type, 
and time zone defined in the JSON file. 

The 'Valid?' line in each section details whether Run Scanner can access the 
directory. If the directory has been defined correctly in the JSON, this line 
will read 'Yes'. If it does not, Run Scanner will outline the issues it is 
facing in accessing the directory. The issue may be that the target of the 
provided path does not exist, or that Run Scanner does not have the necessary
 permissions to read or write to the directory. For more information, please 
refer to <a href="troubleshooting.html#SequencerInvalid">Sequencing Output 
Directory 'Valid?' reads 'No'</a>.

<a id="processors" />
### Processors
The final section of the Main Page is a list of available name and 
platformType combinations. This section can be considered a reference for 
creating the configuration JSON (as outlined in 
<a href="installation.html#setup">Setting Up Run Scanner</a>). 
