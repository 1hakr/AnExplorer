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

import java.io.IOException;
import java.nio.ByteBuffer;

import dev.dworks.apps.anexplorer.libaums.driver.BlockDeviceDriver;
import dev.dworks.apps.anexplorer.libaums.partition.mbr.MasterBootRecord;

/**
 * Helper class to create different supported {@link PartitionTable}s.
 * 
 * @author mjahnen
 * 
 */
public class PartitionTableFactory {
	/**
	 * Creates a {@link PartitionTable} suitable for the given block device. The
	 * partition table should be located at the logical block address zero of
	 * the device.
	 * 
	 * @param blockDevice
	 *            The block device where the partition table is located.
	 * @return The newly created {@link PartitionTable}.
	 * @throws IOException
	 *             If reading from the device fails.
	 */
	public static PartitionTable createPartitionTable(BlockDeviceDriver blockDevice)
			throws IOException {
		// we currently only support mbr
		ByteBuffer buffer = ByteBuffer.allocate(512);
		blockDevice.read(0, buffer);
		return MasterBootRecord.read(buffer);
	}
}
