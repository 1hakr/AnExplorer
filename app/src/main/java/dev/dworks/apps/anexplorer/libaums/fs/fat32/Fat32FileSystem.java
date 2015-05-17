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

import dev.dworks.apps.anexplorer.libaums.driver.BlockDeviceDriver;
import dev.dworks.apps.anexplorer.libaums.fs.FileSystem;
import dev.dworks.apps.anexplorer.libaums.fs.UsbFile;

/**
 * This class represents the FAT32 file system and is responsible for setting
 * the FAT32 file system up and extracting the volume label and the root
 * directory.
 * 
 * @author mjahnen
 * 
 */
public class Fat32FileSystem implements FileSystem {

	private Fat32BootSector bootSector;
	private FAT fat;
	private FsInfoStructure fsInfoStructure;
	private FatDirectory rootDirectory;

	/**
	 * This method constructs a FAT32 file system for the given block device.
	 * There are no further checks that the block device actually represents a
	 * valid FAT32 file system. That means it must be ensured that the device
	 * actually holds a FAT32 file system in advance!
	 * 
	 * @param blockDevice
	 *            The block device the FAT32 file system is located.
	 * @throws IOException
	 *             If reading from the device fails.
	 */
	private Fat32FileSystem(BlockDeviceDriver blockDevice) throws IOException {
		ByteBuffer buffer = ByteBuffer.allocate(512);
		blockDevice.read(0, buffer);
		bootSector = Fat32BootSector.read(buffer);
		fsInfoStructure = FsInfoStructure.read(blockDevice, bootSector.getFsInfoStartSector()
				* bootSector.getBytesPerSector());
		fat = new FAT(blockDevice, bootSector, fsInfoStructure);
		rootDirectory = FatDirectory.readRoot(blockDevice, fat, bootSector);
	}

	/**
	 * This method constructs a FAT32 file system for the given block device.
	 * There are no further checks if the block device actually represents a
	 * valid FAT32 file system. That means it must be ensured that the device
	 * actually holds a FAT32 file system in advance!
	 * 
	 * @param blockDevice
	 *            The block device the FAT32 file system is located.
	 * @throws IOException
	 *             If reading from the device fails.
	 */
	public static Fat32FileSystem read(BlockDeviceDriver blockDevice) throws IOException {
		return new Fat32FileSystem(blockDevice);
	}

	@Override
	public UsbFile getRootDirectory() {
		return rootDirectory;
	}

	@Override
	public String getVolumeLabel() {
		String volumeLabel = rootDirectory.getVolumeLabel();
		if (volumeLabel == null) {
			volumeLabel = bootSector.getVolumeLabel();
		}
		return volumeLabel;
	}
}
