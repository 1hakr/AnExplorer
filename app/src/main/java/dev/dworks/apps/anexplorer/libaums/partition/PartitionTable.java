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

import java.util.Collection;

/**
 * This interface represents a partition table.
 * <p>
 * Normally a block device has a partition table at the beginning of the device
 * which says something about the partitions on the mass storage device. For
 * example where they start and end and which file system a specific partition
 * has.
 * 
 * @author mjahnen
 * 
 */
public interface PartitionTable {
	/**
	 * 
	 * @return The size in bytes the partition table occupies.
	 */
	public int getSize();

	/**
	 * 
	 * @return A collection of {@link PartitionTableEntry}s located on the block
	 *         device.
	 */
	public Collection<PartitionTableEntry> getPartitionTableEntries();
}
