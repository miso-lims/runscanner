---
layout: default
title: Appendices
categories: ['User Manual']
order: 11
---
<a id="A" />
### Appendix A: Processor Definitions
<table>
	<tr>
		<th>JSON Name</th>
		<th>JSON Value(s)</th>
	</tr>
	<tr>
		<td>path</td>
		<td>Absolute path to sequencer output</td>
	</tr>
	<tr>
		<td>platformType</td>
		<td>One of the following:
		<ul>
			<li>ILLUMINA</li>
			<li>PACBIO</li>
			<li>OXFORDNANOPORE</li>
		</ul></td>
	</tr>
	<tr>
		<td>name</td>
		<td>Illumina/PacBio:
		<ul>
			<li>"default"</li>
			<li>"testing"</li>
		</ul>
		Oxford Nanopore:
		<ul>
			<li>"promethion"</li>
			<li>"minion"</li>
			<li>"testing"</li>
		</ul></td>
	</tr>
	<tr>
		<td>timeZone</td>
		<td>Valid String ID for Java TimeZone, which is a TZ Database name. <a href="https://en.wikipedia.org/wiki/List_of_tz_database_time_zones#List">List of TZ Database names on Wikipedia</a></td>
	</tr>
	<tr>
		<td>parameters</td>
		<td>Nested JSON Object:
		<table>
			<tr>
				<th>JSON Name</th>
				<th>JSON Value(s)</th>
			</tr>
			<tr colspan="2">
				<th>ILLUMINA</th>
			</tr>
			<tr>
				<td>checkOutput (optional)</td>
				<td><ul>
					<li>true: Scanner will search for BCL files to verify run is complete if no logs are present. This can negatively affect processing speed over certain network filesystems. Default value.</li>
					<li>false: Scanner will assume run is complete if no logs are present.</li>
				</ul></td>
			</tr>
			<tr colspan="2">
				<th>PACBIO</th>
			</tr>
			<tr>
				<td>address (required)</td>
				<td>URL of PacBio Sequencer</td>
			</tr>
			<tr colspan="2">
				<th>OXFORDNANOPORE</th>
			</tr>
			<tr>
				<td>name (required)</td>
				<td>Unique name for Oxford Nanopore sequencer to be reported as `sequencerName` in run information</td>
			</tr>
		</table></td>
	</tr>
</table>


<a id="B" />
### Appendix B: Run JSON Fields
<table>
	<tr>
		<th>Field Name</th>
		<th>PacBio</th>
		<th>Illumina</th>
		<th>Oxford Nanopore</th>
	</tr>
	<tr>
		<td>platform</td>
		<td></td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>runAlias</td>
		<td></td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>sequencerFolderPath</td>
		<td></td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>sequencerName</td>
		<td></td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>sequencerPosition</td>
		<td></td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>containerSerialNumber</td>
		<td></td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>containerModel</td>
		<td></td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>laneCount</td>
		<td></td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>healthType</td>
		<td></td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>startDate</td>
		<td></td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>completedDate</td>
		<td></td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>pairedEndRun</td>
		<td></td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>software</td>
		<td></td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>metrics</td>
		<td></td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>poolNames</td>
		<td></td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>runBasesMask</td>
		<td></td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>bclCount</td>
		<td></td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>callCycle</td>
		<td></td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>chemistry</td>
		<td></td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>imgCycle</td>
		<td></td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>indexLengths</td>
		<td></td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>numCycles</td>
		<td></td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>numReads</td>
		<td></td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>poolNames</td>
		<td></td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>readLength</td>
		<td></td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>scoreCycle</td>
		<td></td>
		<td></td>
		<td></td>
	</tr>
	<tr>
		<td>workflowType</td>
		<td></td>
		<td></td>
		<td></td>
	</tr>
</table>




<a id="C" />
### Appendix C: Software Versions Supported by Run Scanner
<!-- TODO: oh god huger table -->
