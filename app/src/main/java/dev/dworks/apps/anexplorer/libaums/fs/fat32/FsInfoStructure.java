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

package dev.dworks.apps.anexplorer.libaums.fs.fat32;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import android.util.Log;

import dev.dworks.apps.anexplorer.libaums.driver.BlockDeviceDriver;

/**
 * This class holds information which shall support the {@link FAT}. For example
 * it has a method to get the last allocated cluster (
 * {@link #getLastAllocatedClusterHint()}). The FAT can use this to make
 * searching for free clusters more efficient because it does not have to search
 * the hole FAT.
 * 
 * @author mjahnen
 * 
 */
/* package */class FsInfoStructure {

	/* package */static int INVALID_VALUE = 0xFFFFFFFF;

	private static int LEAD_SIGNATURE_OFF = 0;
	private static int STRUCT_SIGNATURE_OFF = 484;
	private static int TRAIL_SIGNATURE_OFF = 508;
	private static int FREE_COUNT_OFF = 488;
	private static int NEXT_FREE_OFFSET = 492;

	private static int LEAD_SIGNATURE = 0x41615252;
	private static int STRUCT_SIGNATURE = 0x61417272;
	private static int TRAIL_SIGNATURE = 0xAA550000;

	private static final String TAG = FsInfoStructure.class.getSimpleName();

	private int offset;
	private BlockDeviceDriver blockDevice;
	private ByteBuffer buffer;

	/**
	 * Constructs a new info structure.
	 * 
	 * @param blockDevice
	 *            The device where the info structure is located.
	 * @param offset
	 *            The offset where the info structure starts.
	 * @throws IOException
	 *             If reading fails.
	 */
	private FsInfoStructure(BlockDeviceDriver blockDevice, int offset) throws IOException {
		this.blockDevice = blockDevice;
		this.offset = offset;
		buffer = ByteBuffer.allocate(512);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		blockDevice.read(offset, buffer);
		buffer.clear();

		if (buffer.getInt(LEAD_SIGNATURE_OFF) != LEAD_SIGNATURE
				|| buffer.getInt(STRUCT_SIGNATURE_OFF) != STRUCT_SIGNATURE
				|| buffer.getInt(TRAIL_SIGNATURE_OFF) != TRAIL_SIGNATURE) {
			throw new IOException("invalid fs info structure!");
		}
	}

	/**
	 * Reads the info structure from the device.
	 * 
	 * @param blockDevice
	 *            The device where the info structure is located.
	 * @param offset
	 *            The offset where the info structure starts.
	 * @return The newly created object.
	 * @throws IOException
	 *             If reading fails.
	 */
	/* package */static FsInfoStructure read(BlockDeviceDriver blockDevice, int offset)
			throws IOException {
		return new FsInfoStructure(blockDevice, offset);
	}

	/**
	 * Sets the cluster count to the new value. This change is not immediately
	 * written to the disk. If you want to write the change to disk, call
	 * {@link #write()}.
	 * 
	 * @param value
	 *            The new cluster count.
	 * @see #getFreeClusterCount()
	 * @see #decreaseClusterCount(long)
	 */
	/* package */void setFreeClusterCount(long value) {
		buffer.putInt(FREE_COUNT_OFF, (int) value);
	}

	/**
	 * 
	 * @return The free cluster count or {@link #INVALID_VALUE} if this hint is
	 *         not available.
	 */
	/* package */long getFreeClusterCount() {
		return buffer.getInt(FREE_COUNT_OFF);
	}

	/**
	 * Sets the last allocated cluster to the new value. This change is not
	 * immediately written to the disk. If you want to write the change to disk,
	 * call {@link #write()}.
	 * 
	 * @param value
	 *            The new last allocated cluster
	 * @see #getLastAllocatedClusterHint()
	 */
	/* package */void setLastAllocatedClusterHint(long value) {
		buffer.putInt(NEXT_FREE_OFFSET, (int) value);
	}

	/**
	 * 
	 * @return The last allocated cluster or {@link #INVALID_VALUE} if this hint
	 *         is not available.
	 */
	/* package */long getLastAllocatedClusterHint() {
		return buffer.getInt(NEXT_FREE_OFFSET);
	}

	/**
	 * Decreases the cluster count by the desired number of clusters. This is
	 * ignored {@link #getFreeClusterCount()} returns {@link #INVALID_VALUE},
	 * thus the free cluster count is unknown. The cluster count can also be
	 * increased by specifying a negative value!
	 * 
	 * @param numberOfClusters
	 *            Value, free cluster count shall be decreased by.
	 * @see #setFreeClusterCount(long)
	 * @see #getFreeClusterCount()
	 */
	/* package */void decreaseClusterCount(long numberOfClusters) {
		long freeClusterCount = getFreeClusterCount();
		if (freeClusterCount != FsInfoStructure.INVALID_VALUE) {
			setFreeClusterCount(freeClusterCount - numberOfClusters);
		}
	}

	/**
	 * Writes the info structure to the device. This does not happen
	 * automatically, if contents were changed so a call to this method is
	 * needed!
	 * 
	 * @throws IOException
	 *             If writing to device fails.
	 */
	/* package */void write() throws IOException {
		Log.d(TAG, "writing to device");
		blockDevice.write(offset, buffer);
		buffer.clear();
	}
}
