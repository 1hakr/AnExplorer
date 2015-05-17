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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * This class represents the FAT32 boot sector which is always located at the
 * beginning of every FAT32 file system. It holds important information about
 * the file system such as the cluster size and the start cluster of the root
 * directory.
 * 
 * @author mjahnen
 * 
 */
/* package */class Fat32BootSector {
	private static final int BYTES_PER_SECTOR_OFF = 11;
	private static final int SECTORS_PER_CLUSTER_OFF = 13;
	private static final int RESERVED_COUNT_OFF = 14;
	private static final int FAT_COUNT_OFF = 16;
	private static final int TOTAL_SECTORS_OFF = 32;
	private static final int SECTORS_PER_FAT_OFF = 36;
	private static final int FLAGS_OFF = 40;
	private static final int ROOT_DIR_CLUSTER_OFF = 44;
	private static final int FS_INFO_SECTOR_OFF = 48;
	private static final int VOLUME_LABEL_OFF = 48;

	private short bytesPerSector;
	private byte sectorsPerCluster;
	private short reservedSectors;
	private byte fatCount;
	private long totalNumberOfSectors;
	private long sectorsPerFat;
	private long rootDirStartCluster;
	private short fsInfoStartSector;
	private boolean fatMirrored;
	private byte validFat;
	private String volumeLabel;

	private Fat32BootSector() {

	}

	/**
	 * Reads a FAT32 boot sector from the given buffer. The buffer has to be 512
	 * (the size of a boot sector) bytes.
	 * 
	 * @param buffer
	 *            The data where the boot sector is located.
	 * @return A newly created boot sector.
	 */
	/* package */static Fat32BootSector read(ByteBuffer buffer) {
		Fat32BootSector result = new Fat32BootSector();
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		result.bytesPerSector = buffer.getShort(BYTES_PER_SECTOR_OFF);
		result.sectorsPerCluster = buffer.get(SECTORS_PER_CLUSTER_OFF);
		result.reservedSectors = buffer.getShort(RESERVED_COUNT_OFF);
		result.fatCount = buffer.get(FAT_COUNT_OFF);
		result.totalNumberOfSectors = buffer.getInt(TOTAL_SECTORS_OFF) & 0xffffffffl;
		result.sectorsPerFat = buffer.getInt(SECTORS_PER_FAT_OFF) & 0xffffffffl;
		result.rootDirStartCluster = buffer.getInt(ROOT_DIR_CLUSTER_OFF) & 0xffffffffl;
		result.fsInfoStartSector = buffer.getShort(FS_INFO_SECTOR_OFF);
		short flag = buffer.getShort(FLAGS_OFF);
		result.fatMirrored = ((byte) flag & 0x80) == 0;
		result.validFat = (byte) ((byte) flag & 0x7);

		StringBuilder builder = new StringBuilder();

		for (int i = 0; i < 11; i++) {
			byte b = buffer.get(VOLUME_LABEL_OFF + i);
			if (b == 0)
				break;
			builder.append((char) b);
		}

		result.volumeLabel = builder.toString();

		return result;
	}

	/**
	 * Returns the number of bytes in one single sector of a FAT32 file system.
	 * 
	 * @return Number of bytes.
	 */
	/* package */short getBytesPerSector() {
		return bytesPerSector;
	}

	/**
	 * Returns the number of sectors in one single cluster of a FAT32 file
	 * system.
	 * 
	 * @return Number of bytes.
	 */
	/* package */byte getSectorsPerCluster() {
		return sectorsPerCluster;
	}

	/**
	 * Returns the number of reserved sectors at the beginning of the FAT32 file
	 * system. This includes one sector for the boot sector.
	 * 
	 * @return Number of sectors.
	 */
	/* package */short getReservedSectors() {
		return reservedSectors;
	}

	/**
	 * Returns the number of the FATs in the FAT32 file system. This is mostly
	 * 2.
	 * 
	 * @return Number of FATs.
	 */
	/* package */byte getFatCount() {
		return fatCount;
	}

	/**
	 * Returns the total number of sectors in the file system.
	 * 
	 * @return Total number of sectors.
	 */
	/* package */long getTotalNumberOfSectors() {
		return totalNumberOfSectors;
	}

	/**
	 * Returns the total number of sectors in one file allocation table. The
	 * FATs have a fixed size.
	 * 
	 * @return Number of sectors in one FAT.
	 */
	/* package */long getSectorsPerFat() {
		return sectorsPerFat;
	}

	/**
	 * Returns the start cluster of the root directory in the FAT32 file system.
	 * 
	 * @return Root directory start cluster.
	 */
	/* package */long getRootDirStartCluster() {
		return rootDirStartCluster;
	}

	/**
	 * Returns the start sector of the file system info structure.
	 * 
	 * @return FSInfo Structure start sector.
	 */
	/* package */short getFsInfoStartSector() {
		return fsInfoStartSector;
	}

	/**
	 * Returns if the different FATs in the file system are mirrored, ie. all of
	 * them are holding the same data. This is used for backup purposes.
	 * 
	 * @return True if the FAT is mirrored.
	 * @see #getValidFat()
	 * @see #fatCount()
	 */
	/* package */boolean isFatMirrored() {
		return fatMirrored;
	}

	/**
	 * Returns the valid FATs which shall be used if the FATs are not mirrored.
	 * 
	 * @return Number of the valid FAT.
	 * @see #isFatMirrored()
	 * @see #fatCount()
	 */
	/* package */byte getValidFat() {
		return validFat;
	}

	/**
	 * Returns the amount in bytes in one cluster.
	 * 
	 * @return Amount of bytes.
	 */
	/* package */int getBytesPerCluster() {
		return sectorsPerCluster * bytesPerSector;
	}

	/**
	 * Returns the FAT offset in bytes from the beginning of the file system for
	 * the given FAT number.
	 * 
	 * @param fatNumber
	 *            The number of the FAT.
	 * @return Offset in bytes.
	 * @see #isFatMirrored()
	 * @see #fatCount()
	 * @see #getValidFat()
	 */
	/* package */long getFatOffset(int fatNumber) {
		return getBytesPerSector() * (getReservedSectors() + fatNumber * getSectorsPerFat());
	}

	/**
	 * Returns the offset in bytes from the beginning of the file system of the
	 * data area. The data area is the area where the contents of directories
	 * and files are saved.
	 * 
	 * @return Offset in bytes.
	 */
	/* package */long getDataAreaOffset() {
		return getFatOffset(0) + getFatCount() * getSectorsPerFat() * getBytesPerSector();
	}

	/**
	 * This returns the volume label stored in the boot sector. This is mostly
	 * not used and you should instead use {@link FatDirectory#getVolumeLabel()}
	 * of the root directory.
	 * 
	 * @return The volume label.
	 */
	/* package */String getVolumeLabel() {
		return volumeLabel;
	}
}
