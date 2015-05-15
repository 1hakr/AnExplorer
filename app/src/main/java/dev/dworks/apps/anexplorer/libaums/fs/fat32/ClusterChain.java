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

import android.util.Log;

import dev.dworks.apps.anexplorer.libaums.driver.BlockDeviceDriver;

/**
 * This class represents a cluster chain which can be followed in the FAT of a
 * FAT32 file system. You can {@link #read(long, ByteBuffer) read} from or
 * {@link #write(long, ByteBuffer) write} to it easily without having to worry
 * about the specific clusters.
 * 
 * @author mjahnen
 * 
 */
public class ClusterChain {

	private static final String TAG = ClusterChain.class.getSimpleName();

	private BlockDeviceDriver blockDevice;
	private FAT fat;
	private Long[] chain;
	private long clusterSize;
	private long dataAreaOffset;

	/**
	 * Constructs a new ClusterChain with the given information.
	 * 
	 * @param startCluster
	 *            The start cluster which shall be followed in the FAT.
	 * @param blockDevice
	 *            The block device where the fat fs is located.
	 * @param fat
	 *            The file allocation table.
	 * @param bootSector
	 *            The boot sector of the FAT32 fs.
	 * @throws IOException
	 */
	/* package */ClusterChain(long startCluster, BlockDeviceDriver blockDevice, FAT fat,
			Fat32BootSector bootSector) throws IOException {
		this.fat = fat;
		this.blockDevice = blockDevice;
		chain = fat.getChain(startCluster);
		clusterSize = bootSector.getBytesPerCluster();
		dataAreaOffset = bootSector.getDataAreaOffset();
	}

	/**
	 * Reads from the cluster chain at the given offset into the given buffer.
	 * This method automatically searches for following clusters in the chain
	 * and reads from them appropriately.
	 * 
	 * @param offset
	 *            The offset in bytes where reading shall start.
	 * @param dest
	 *            The destination buffer the contents of the chain shall be
	 *            copied to.
	 * @throws IOException
	 *             If reading fails.
	 */
	/* package */void read(long offset, ByteBuffer dest) throws IOException {
		int length = dest.remaining();

		int chainIndex = (int) (offset / clusterSize);
		// if the offset is not a multiple of the cluster size we have to start
		// reading
		// directly in the cluster
		if (offset % clusterSize != 0) {
			// offset in the cluster
			int clusterOffset = (int) (offset % clusterSize);
			int size = Math.min(length, (int) (clusterSize - clusterOffset));
			dest.limit(dest.position() + size);

			blockDevice.read(getFileSystemOffset(chain[chainIndex], clusterOffset), dest);

			// round up to next cluster in the chain
			chainIndex++;
			// make length now a multiple of the cluster size
			length -= size;
		}

		// now we can proceed reading the clusters without an offset in the
		// cluster
		while (length > 0) {
			// we always read one cluster at a time, or if remaining size is
			// less than the cluster size, only "size" bytes
			int size = (int) Math.min(clusterSize, length);
			dest.limit(dest.position() + size);

			blockDevice.read(getFileSystemOffset(chain[chainIndex], 0), dest);

			chainIndex++;
			length -= size;
		}
	}

	/**
	 * Writes to the cluster chain at the given offset from the given buffer.
	 * This method automatically searches for following clusters in the chain
	 * and reads from them appropriately.
	 * 
	 * @param offset
	 *            The offset in bytes where writing shall start.
	 * @param source
	 *            The buffer which holds the contents which shall be transferred
	 *            into the cluster chain.
	 * @throws IOException
	 *             If writing fails.
	 */
	/* package */void write(long offset, ByteBuffer source) throws IOException {
		int length = source.remaining();

		int chainIndex = (int) (offset / clusterSize);
		// if the offset is not a multiple of the cluster size we have to start
		// reading
		// directly in the cluster
		if (offset % clusterSize != 0) {
			int clusterOffset = (int) (offset % clusterSize);
			int size = Math.min(length, (int) (clusterSize - clusterOffset));
			source.limit(source.position() + size);

			blockDevice.write(getFileSystemOffset(chain[chainIndex], clusterOffset), source);

			// round up to next cluster in the chain
			chainIndex++;
			// make length now a multiple of the cluster size
			length -= size;
		}

		// now we can proceed reading the clusters without an offset in the
		// cluster
		while (length > 0) {
			// we always write one cluster at a time, or if remaining size is
			// less than the cluster size, only "size" bytes
			int size = (int) Math.min(clusterSize, length);
			source.limit(source.position() + size);

			blockDevice.write(getFileSystemOffset(chain[chainIndex], 0), source);

			chainIndex++;
			length -= size;
		}
	}

	/**
	 * Returns the offset of a cluster from the beginning of the FAT32 file
	 * system in bytes.
	 * 
	 * @param cluster
	 *            The desired cluster.
	 * @param clusterOffset
	 *            The desired offset in bytes in the cluster.
	 * @return Offset in bytes from the beginning of the disk (FAT32 file
	 *         system).
	 */
	private long getFileSystemOffset(long cluster, int clusterOffset) {
		return dataAreaOffset + clusterOffset + (cluster - 2) * clusterSize;
	}

	/**
	 * Sets a new cluster size for the cluster chain. This method allocates or
	 * frees clusters in the FAT if the number of the new clusters is bigger or
	 * lower than the current number of allocated clusters.
	 * 
	 * @param newNumberOfClusters
	 *            The new number of clusters.
	 * @throws IOException
	 *             If growing or allocating the chain fails.
	 * @see #getClusters()
	 * @see #setLength(long)
	 */
	/* package */void setClusters(int newNumberOfClusters) throws IOException {
		int oldNumberOfClusters = getClusters();
		if (newNumberOfClusters == oldNumberOfClusters)
			return;

		if (newNumberOfClusters > oldNumberOfClusters) {
			Log.d(TAG, "grow chain");
			chain = fat.alloc(chain, newNumberOfClusters - oldNumberOfClusters);
		} else {
			Log.d(TAG, "shrink chain");
			chain = fat.free(chain, oldNumberOfClusters - newNumberOfClusters);
		}
	}

	/**
	 * Gets the current allocated clusters for this chain.
	 * 
	 * @return The number of clusters.
	 * @see #setClusters(int)
	 * @see #getLength()
	 */
	/* package */int getClusters() {
		return chain.length;
	}

	/**
	 * Sets the new length in bytes of this chain. This method allocates or
	 * frees new space on the disk depending on the new length.
	 * 
	 * @param newLength
	 *            The new length.
	 * @throws IOException
	 *             If growing or allocating the chain fails.
	 * @see #getLength()
	 * @see #setClusters(int)
	 */
	/* package */void setLength(long newLength) throws IOException {
		final long newNumberOfClusters = ((newLength + clusterSize - 1) / clusterSize);
		setClusters((int) newNumberOfClusters);
	}

	/**
	 * Returns the size in bytes the chain currently occupies on the disk.
	 * 
	 * @return The size / length in bytes of the chain.
	 * @see #setLength(long)
	 * @see #getClusters()
	 */
	/* package */long getLength() {
		return chain.length * clusterSize;
	}
}
