/*
 * (C) Copyright 2014 mjahnen <jahnen@in.tum.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package dev.dworks.apps.anexplorer.libaums.partition;

/**
 * Class which holds various information about the partitions located on a block
 * device.
 * 
 * @author mjahnen
 * 
 */
public class PartitionTableEntry {

	byte partitionType;
	int logicalBlockAddress;
	int totalNumberOfSectors;

	/**
	 * Construct a new PartitionTableEntry with the given information.
	 * 
	 * @param partitionType
	 *            The file system type of the partition (eg. FAT32).
	 * @param logicalBlockAddress
	 *            The logical block address on the device where this partition
	 *            starts.
	 * @param totalNumberOfSectors
	 *            The total numbers of sectors occupied by the partition.
	 */
	public PartitionTableEntry(byte partitionType, int logicalBlockAddress, int totalNumberOfSectors) {
		this.partitionType = partitionType;
		this.logicalBlockAddress = logicalBlockAddress;
		this.totalNumberOfSectors = totalNumberOfSectors;
	}

	/**
	 * 
	 * @return The file system type of the partition.
	 */
	public byte getPartitionType() {
		return partitionType;
	}

	/**
	 * 
	 * @return The logical block address where this partitions starts on the
	 *         device.
	 */
	public int getLogicalBlockAddress() {
		return logicalBlockAddress;
	}

	/**
	 * 
	 * @return The total numbers of sectors occupied by this partition. This
	 *         value is often unused because the same information is also stored
	 *         in the specific file system.
	 */
	public int getTotalNumberOfSectors() {
		return totalNumberOfSectors;
	}
}
