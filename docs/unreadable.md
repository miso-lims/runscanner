The Unreadable page of Run Scanner can be accessed by clicking the
'Unreadable' link on the header bar of the Run Scanner interface. This page
lists the directories Run Scanner has identified as runs but have failed in
some way to process.

These runs may have failed due to:
* the directory not containing any valid run data files
* a run data file containing data of a format Run Scanner cannot process
* Run Scanner does not have the appropriate permissions to access the run data files
* any other interruption to Run Scanner's processing

Runs will **not** appear in the Unreadable list if they are within a
sequencing output directory for which Run Scanner lacks permission to
access entirely. The 
[Sequencing Output Directories section of the Main Page](../main/#sequencing-output-directories)
will indicate when this is the
case.

For more information on fixing runs which have failed to read, please
refer to [Troubleshooting unreadable runs](../troubleshooting/#runs-appear-in-unreadable-list).
