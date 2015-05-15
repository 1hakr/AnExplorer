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

import android.util.Log;

import dev.dworks.apps.anexplorer.libaums.driver.BlockDeviceDriver;
import dev.dworks.apps.anexplorer.libaums.fs.FileSystem;
import dev.dworks.apps.anexplorer.libaums.fs.FileSystemFactory;

/**
 * This class represents a partition on an mass storage device. A partition has
 * a certain file system which can be accessed via {@link #getFileSystem()}.
 * This file system is needed to to access the files and directories of a
 * partition.
 * <p>
 * The method {@link #getVolumeLabel()} returns the volume label for the
 * partition. Calling the method is equivalent to calling
 * {@link FileSystem#getVolumeLabel()}.
 * 
 * @author mjahnen
 * 
 */
public class Partition implements BlockDeviceDriver {

	private static final String TAG = Partition.class.getSimpleName();

	// private PartitionTableEntry partitionTableEntry;
	private BlockDeviceDriver blockDevice;
	/**
	 * The logical block address where on the device this partition starts.
	 */
	private int logicalBlockAddress;
	private int blockSize;
	private FileSystem fileSystem;

	private Partition() {

	}

	/**
	 * Creates a new partition with the information given.
	 * 
	 * @param entry
	 *            The entry the partition shall represent.
	 * @param blockDevice
	 *            The underlying block device.
	 * @return The newly created Partition.
	 * @throws IOException
	 *             If reading from the device fails.
	 */
	public static Partition createPartition(PartitionTableEntry entry, BlockDeviceDriver blockDevice)
			throws IOException {
		Partition partition = null;

		// we currently only support fat32
		int partitionType = entry.getPartitionType();
		if (partitionType == 0x0b || partitionType == 0x0c) {
			partition = new Partition();
			// partition.partitionTableEntry = entry;
			partition.logicalBlockAddress = entry.getLogicalBlockAddress();
			partition.blockDevice = blockDevice;
			partition.blockSize = blockDevice.getBlockSize();
			partition.fileSystem = FileSystemFactory.createFileSystem(entry, partition);
		} else {
			Log.w(TAG, "unsupported partition type: " + entry.getPartitionType());
		}

		return partition;
	}

	/**
	 * 
	 * @return the file system on the partition which can be used to access
	 *         files and directories.
	 */
	public FileSystem getFileSystem() {
		return fileSystem;
	}

	/**
	 * This method returns the volume label of the file system / partition.
	 * Calling this method is equivalent to calling
	 * {@link FileSystem#getVolumeLabel()}.
	 * 
	 * @return Returns the volume label of this partition.
	 */
	public String getVolumeLabel() {
		return fileSystem.getVolumeLabel();
	}

	@Override
	public void init() {

	}

	@Override
	public void read(long offset, ByteBuffer dest) throws IOException {
		long devOffset = offset / blockSize + logicalBlockAddress;
		// TODO try to make this more efficient by for example making tmp buffer
		// global
		if (offset % blockSize != 0) {
			Log.w(TAG, "device offset not a multiple of block size");
			ByteBuffer tmp = ByteBuffer.allocate(blockSize);

			blockDevice.read(devOffset, tmp);
			tmp.clear();
			tmp.position((int) (offset % blockSize));
			dest.put(tmp);

			devOffset++;
		}

		if (dest.remaining() > 0)
			blockDevice.read(devOffset, dest);
	}

	@Override
	public void write(long offset, ByteBuffer src) throws IOException {
		long devOffset = offset / blockSize + logicalBlockAddress;
		// TODO try to make this more efficient by for example making tmp buffer
		// global
		if (offset % blockSize != 0) {
			Log.w(TAG, "device offset not a multiple of block size");
			ByteBuffer tmp = ByteBuffer.allocate(blockSize);

			blockDevice.read(devOffset, tmp);
			tmp.clear();
			tmp.position((int) (offset % blockSize));
			int remaining = Math.min(tmp.remaining(), src.remaining());
			tmp.put(src.array(), src.position(), remaining);
			src.position(src.position() + remaining);
			tmp.clear();
			blockDevice.write(devOffset, tmp);

			devOffset++;
		}

		if (src.remaining() > 0)
			blockDevice.write(devOffset, src);
	}

	@Override
	public int getBlockSize() {
		return blockDevice.getBlockSize();
	}
}
