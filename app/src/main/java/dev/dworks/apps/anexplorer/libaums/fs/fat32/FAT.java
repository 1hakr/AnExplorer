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

import android.util.Log;

import dev.dworks.apps.anexplorer.libaums.driver.BlockDeviceDriver;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * This class represents the File Allocation Table (FAT) in a FAT32 file system.
 * The FAT is used to allocate the space of the disk to the different files and
 * directories.
 * <p>
 * The FAT distributes clusters with a specific cluster size
 * {@link dev.dworks.apps.anexplorer.libaums.fs.fat32.Fat32BootSector #getBytesPerCluster()}
 * . Every entry in the FAT is 32 bit. The FAT is a (linked) list where the
 * clusters can be followed until a cluster chain ends.
 * <p>
 * For more information you should refer to the official documentation of FAT32.
 * 
 * @author mjahnen
 * 
 */
public class FAT {

	private static final String TAG = FAT.class.getSimpleName();

	/**
	 * End of file / chain marker. This is used to determine when following a
	 * cluster chain should be stopped. (Last allocated cluster has been found.)
	 */
	private static final int FAT32_EOF_CLUSTER = 0x0FFFFFF8;

	private BlockDeviceDriver blockDevice;
	private long fatOffset[];
	private int fatNumbers[];
	private FsInfoStructure fsInfoStructure;

	/**
	 * Constructs a new FAT.
	 * 
	 * @param blockDevice
	 *            The block device where the FAT is located.
	 * @param bootSector
	 *            The corresponding boot sector of the FAT32 file system.
	 * @param fsInfoStructure
	 *            The info structure where the last allocated block and the free
	 *            clusters are saved.
	 */
	/* package */FAT(BlockDeviceDriver blockDevice, Fat32BootSector bootSector,
			FsInfoStructure fsInfoStructure) {
		this.blockDevice = blockDevice;
		this.fsInfoStructure = fsInfoStructure;
		if (!bootSector.isFatMirrored()) {
			int fatNumber = bootSector.getValidFat();
			fatNumbers = new int[] { fatNumber };
			Log.i(TAG, "fat is not mirrored, fat " + fatNumber + " is valid");
		} else {
			int fatCount = bootSector.getFatCount();
			fatNumbers = new int[fatCount];
			for (int i = 0; i < fatCount; i++) {
				fatNumbers[i] = i;
			}
			Log.i(TAG, "fat is mirrored, fat count: " + fatCount);
		}

		fatOffset = new long[fatNumbers.length];
		for (int i = 0; i < fatOffset.length; i++) {
			fatOffset[i] = bootSector.getFatOffset(fatNumbers[i]);
		}
	}

	/**
	 * This methods gets a chain by following the given start cluster to an end
	 * mark.
	 * 
	 * @param startCluster
	 *            The start cluster where the chain starts.
	 * @return The chain including the start cluster.
	 * @throws IOException
	 *             If reading from device fails.
	 */
	/* package */Long[] getChain(long startCluster) throws IOException {
		
		if(startCluster == 0) {
			// if the start cluster is 0, we have an empty file 
			return new Long[0];
		}
		
		final ArrayList<Long> result = new ArrayList<Long>();
		final int bufferSize = blockDevice.getBlockSize() * 2;
		// for performance reasons we always read or write two times the block
		// size
		// this is esp. good for long cluster chains because it reduces of read
		// or writes
		// and mostly cluster chains are located consecutively in the FAT
		final ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
		buffer.order(ByteOrder.LITTLE_ENDIAN);

		long currentCluster = startCluster;
		long offset;
		long offsetInBlock;
		long lastOffset = -1;

		do {
			result.add(currentCluster);
			offset = ((fatOffset[0] + currentCluster * 4) / bufferSize) * bufferSize;
			offsetInBlock = ((fatOffset[0] + currentCluster * 4) % bufferSize);

			// if we have a new offset we are forced to read again
			if (lastOffset != offset) {
				buffer.clear();
				blockDevice.read(offset, buffer);
				lastOffset = offset;
			}

			currentCluster = buffer.getInt((int) offsetInBlock);
		} while (currentCluster < FAT32_EOF_CLUSTER);

		return result.toArray(new Long[0]);
	}

	/**
	 * This methods searches for free clusters in the chain and then assigns it
	 * to the existing chain which is given at a parameter. The current chain
	 * given as parameter can also be empty so that a completely new chain (with
	 * a new start cluster) is created.
	 * 
	 * @param chain
	 *            The existing chain or an empty array to create a completely
	 *            new chain.
	 * @param numberOfClusters
	 *            The number of clusters which shall newly be allocated.
	 * @return The new chain including the old and the newly allocated clusters.
	 * @throws IOException
	 *             If reading or writing to the FAT fails.
	 */
	/* package */Long[] alloc(Long[] chain, int numberOfClusters) throws IOException {
		final ArrayList<Long> result = new ArrayList<Long>(chain.length + numberOfClusters);
		result.addAll(Arrays.asList(chain));
		// for performance reasons we always read or write two times the block
		// size
		// this is esp. good for long cluster chains because it reduces of read
		// or writes
		// and mostly cluster chains are located consecutively in the FAT
		final int bufferSize = blockDevice.getBlockSize() * 2;
		final ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
		buffer.order(ByteOrder.LITTLE_ENDIAN);

		final long cluster;
		if (chain.length != 0)
			cluster = chain[chain.length - 1];
		else
			cluster = -1;

		long lastAllocated = fsInfoStructure.getLastAllocatedClusterHint();
		if (lastAllocated == FsInfoStructure.INVALID_VALUE) {
			// we have to start from the beginning because there is no hint!
			lastAllocated = 2;
		}

		long currentCluster = lastAllocated;

		long offset;
		long offsetInBlock;
		long lastOffset = -1;

		// first we search all needed cluster and save them
		while (numberOfClusters > 0) {
			currentCluster++;
			offset = ((fatOffset[0] + currentCluster * 4) / bufferSize) * bufferSize;
			offsetInBlock = ((fatOffset[0] + currentCluster * 4) % bufferSize);

			// if we have a new offset we are forced to read again
			if (lastOffset != offset) {
				buffer.clear();
				blockDevice.read(offset, buffer);
				lastOffset = offset;
			}

			if (buffer.getInt((int) offsetInBlock) == 0) {
				result.add(currentCluster);
				numberOfClusters--;
			}
		}

		// TODO we should write in in all FATs when they are mirrored!
		if (cluster != -1) {
			// now it is time to write the partial cluster chain
			// start with the last cluster in the existing chain
			offset = ((fatOffset[0] + cluster * 4) / bufferSize) * bufferSize;
			offsetInBlock = ((fatOffset[0] + cluster * 4) % bufferSize);

			// if we have a new offset we are forced to read again
			if (lastOffset != offset) {
				buffer.clear();
				blockDevice.read(offset, buffer);
				lastOffset = offset;
			}
			buffer.putInt((int) offsetInBlock, (int) result.get(chain.length).longValue());
		}

		// write the new allocated clusters now
		for (int i = chain.length; i < result.size() - 1; i++) {
			currentCluster = result.get(i);
			offset = ((fatOffset[0] + currentCluster * 4) / bufferSize) * bufferSize;
			offsetInBlock = ((fatOffset[0] + currentCluster * 4) % bufferSize);

			// if we have a new offset we are forced to read again
			if (lastOffset != offset) {
				buffer.clear();
				blockDevice.write(lastOffset, buffer);
				buffer.clear();
				blockDevice.read(offset, buffer);
				lastOffset = offset;
			}

			buffer.putInt((int) offsetInBlock, (int) result.get(i + 1).longValue());
		}

		// write end mark to last newly allocated cluster now
		currentCluster = result.get(result.size() - 1);
		offset = ((fatOffset[0] + currentCluster * 4) / bufferSize) * bufferSize;
		offsetInBlock = ((fatOffset[0] + currentCluster * 4) % bufferSize);

		// if we have a new offset we are forced to read again
		if (lastOffset != offset) {
			buffer.clear();
			blockDevice.write(lastOffset, buffer);
			buffer.clear();
			blockDevice.read(offset, buffer);
			lastOffset = offset;
		}
		buffer.putInt((int) offsetInBlock, FAT32_EOF_CLUSTER);
		buffer.clear();
		blockDevice.write(offset, buffer);

		// refresh the info structure
		fsInfoStructure.setLastAllocatedClusterHint(currentCluster);
		fsInfoStructure.decreaseClusterCount(numberOfClusters);
		fsInfoStructure.write();

		Log.i(TAG, "allocating clusters finished");

		return result.toArray(new Long[0]);
	}

	/**
	 * This methods frees the desired number of clusters in the FAT and then
	 * sets the last remaining cluster to the end mark. If all clusters are
	 * requested to be freed the last step will be omitted.
	 * <p>
	 * This methods frees the clusters starting at the end of the existing
	 * cluster chain.
	 * 
	 * @param chain
	 *            The existing chain where the clusters shall be freed from.
	 * @param numberOfClusters
	 *            The amount of clusters which shall be freed.
	 * @return The new chain without the unneeded clusters.
	 * @throws IOException
	 *             If reading or writing to the FAT fails.
	 * @throws IllegalStateException
	 *             If more clusters are requested to be freed than currently
	 *             exist in the chain.
	 */
	/* package */Long[] free(Long[] chain, int numberOfClusters) throws IOException {
		final int offsetInChain = chain.length - numberOfClusters;
		// for performance reasons we always read or write two times the block
		// size
		// this is esp. good for long cluster chains because it reduces of read
		// or writes
		// and mostly cluster chains are located consecutively in the FAT
		final int bufferSize = blockDevice.getBlockSize() * 2;
		final ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
		buffer.order(ByteOrder.LITTLE_ENDIAN);

		if (offsetInChain < 0)
			throw new IllegalStateException(
					"trying to remove more clusters in chain than currently exist!");

		long currentCluster;

		long offset;
		long offsetInBlock;
		long lastOffset = -1;

		// free all unneeded clusters
		for (int i = offsetInChain; i < chain.length; i++) {
			currentCluster = chain[i];
			offset = ((fatOffset[0] + currentCluster * 4) / bufferSize) * bufferSize;
			offsetInBlock = ((fatOffset[0] + currentCluster * 4) % bufferSize);

			// if we have a new offset we are forced to read again
			if (lastOffset != offset) {
				if (lastOffset != -1) {
					buffer.clear();
					blockDevice.write(lastOffset, buffer);
				}

				buffer.clear();
				blockDevice.read(offset, buffer);
				lastOffset = offset;
			}

			buffer.putInt((int) offsetInBlock, 0);
		}

		// TODO we should write in in all FATs when they are mirrored!
		if (offsetInChain > 0) {
			// write the end mark to last cluster in the new chain
			currentCluster = chain[offsetInChain - 1];
			offset = ((fatOffset[0] + currentCluster * 4) / bufferSize) * bufferSize;
			offsetInBlock = ((fatOffset[0] + currentCluster * 4) % bufferSize);

			// if we have a new offset we are forced to read again
			if (lastOffset != offset) {
				buffer.clear();
				blockDevice.write(lastOffset, buffer);
				buffer.clear();
				blockDevice.read(offset, buffer);
				lastOffset = offset;
			}
			buffer.putInt((int) offsetInBlock, FAT32_EOF_CLUSTER);
			buffer.clear();
			blockDevice.write(offset, buffer);
		} else {
			// if we freed all clusters we have to write the last change of the
			// for loop above
			buffer.clear();
			blockDevice.write(lastOffset, buffer);
		}

		Log.i(TAG, "freed " + numberOfClusters + " clusters");

		// increase the free cluster count by decreasing with a negative value
		fsInfoStructure.decreaseClusterCount(-numberOfClusters);
		fsInfoStructure.write();

		return Arrays.copyOfRange(chain, 0, offsetInChain);
	}
}
