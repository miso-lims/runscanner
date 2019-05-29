---
layout: default
title: Internal Operation
order: 9.5
categories: ['User Manual']
---
This page outlines how Run Scanner operates, and uses the information provided by the configuration JSON. For further information, please refer to the Developer Documentation. <!-- TODO: this does not exist yet -->

<a id="scheduling" />
### Scheduler
Upon startup, Run Scanner starts a service called the Scheduler, which periodically begins Run Scanner's scanning processes. <!-- TODO: Finish -->

<a id="configurations" />
### Configurations
Run Scanner reads each JSON object in the configuration JSON and maps the fields within to Configuration Objects. These objects are held in memory by Run Scanner and updated every 15 minutes by the <a href="#scheduling">Scheduler</a>. Each Configuration holds a reference to a <a href="#processors">Processor</a>, which is selected based on the name, platformType, and parameters specified in JSON object corresponding to the Configuration.

<a id="processors" />
### Processors
<!-- TODO: Write this -->
