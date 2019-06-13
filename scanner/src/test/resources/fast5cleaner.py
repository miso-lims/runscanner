## A Utility for cleaning fast5s of data that we don't need for our test cases
import sys
import h5py

# Fail early if wrong number of arguments
# Length is 2 because sys.argv is always at least 1
if len(sys.argv) != 2:
    print("Please specify the fast5 file you wish to clean.")
    print("eg. 'python fast5cleaner test_run.fast5'")
    sys.exit(1)

# Open the specified file with h5py, read-only
oldFile = h5py.File(sys.argv[1], 'r')

# Create a new fast5 file and open it with h5py
newFile = h5py.File(sys.argv[1].replace(".fast5", "_clean.fast5"), 'a')

# Select the first read Group in the specified file 
if "UniqueGlobalKey" in oldFile.keys():
    readToKeep = oldFile["UniqueGlobalKey"]
else:
    readToKeep = oldFile[oldFile.keys()[0]]

# Create a single group in the new file under the read's name
theGroup = newFile.create_group(readToKeep.name)

# We only want to keep the metadata subgroups
groupsToKeep = {'tracking_id', 'context_tags'}
for subGroupName in groupsToKeep:
    # Add these new subgroups to the group in the new file
    newSubGroup = theGroup.create_group(subGroupName)

    # Copy all attributes within the subgroup to the new subgroup
    # (Attributes are (name, value))
    for oldFileAttr in readToKeep[subGroupName].attrs.items():
        newSubGroup.attrs.create(oldFileAttr[0], oldFileAttr[1]) 

