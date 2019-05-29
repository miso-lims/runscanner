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

<a id="C" />
### Appendix C: Software Versions Supported by Run Scanner
