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
import java.util.List;

/**
 * This class represents a long file name entry. The long file name can be
 * accessed via {@link #getName()}. This class delegates most actions to the
 * {@link #actualEntry}. It is responsible for parsing and serializing long file
 * names and the actual entry with the corresponding short name.
 * <p>
 * To understand the structure of long file name entries it is advantageous to
 * look at the official FAT32 specification.
 * 
 * @author mjahnen
 * 
 */
/* package */class FatLfnDirectoryEntry {

	/**
	 * The actual entry which holds information like the start cluster and the
	 * file size.
	 */
	private FatDirectoryEntry actualEntry;
	/**
	 * The long file name or null if the actual entry does not have a long file
	 * name.
	 */
	private String lfnName;

	private FatLfnDirectoryEntry() {

	}

	/**
	 * Creates a new {@link #FatLfnDirectoryEntry()} with the given information.
	 * 
	 * @param actualEntry
	 *            The actual entry in the FAT directory.
	 * @param lfnName
	 *            The long file name, can be null.
	 */
	private FatLfnDirectoryEntry(FatDirectoryEntry actualEntry, String lfnName) {
		this.actualEntry = actualEntry;
		this.lfnName = lfnName;
	}

	/**
	 * Creates a completely new {@link #FatLfnDirectoryEntry()}.
	 * 
	 * @param name
	 *            The long file name.
	 * @param shortName
	 *            The generated short name.
	 * @return The newly created entry.
	 */
	/* package */static FatLfnDirectoryEntry createNew(String name, ShortName shortName) {
		FatLfnDirectoryEntry result = new FatLfnDirectoryEntry();

		result.lfnName = name;
		result.actualEntry = FatDirectoryEntry.createNew();
		result.actualEntry.setShortName(shortName);

		return result;
	}

	/**
	 * Reads a {@link #FatLfnDirectoryEntry()} with the given information.
	 * 
	 * @param actualEntry
	 *            The actual entry.
	 * @param lfnParts
	 *            The entries where the long file name is stored in reverse
	 *            order.
	 * @return The newly created entry.
	 */
	/* package */static FatLfnDirectoryEntry read(FatDirectoryEntry actualEntry,
			List<FatDirectoryEntry> lfnParts) {
		StringBuilder builder = new StringBuilder(13 * lfnParts.size());

		if (lfnParts.size() > 0) {
			// stored in reverse order on the disk
			for (int i = lfnParts.size() - 1; i >= 0; i--) {
				lfnParts.get(i).extractLfnPart(builder);
			}

			return new FatLfnDirectoryEntry(actualEntry, builder.toString());
		}

		return new FatLfnDirectoryEntry(actualEntry, null);
	}

	/**
	 * Serializes the long file name and the actual entry in the order needed
	 * into the buffer. Updates the position of the buffer.
	 * 
	 * @param buffer
	 *            The buffer were the serialized data shall be stored.
	 */
	/* package */void serialize(ByteBuffer buffer) {
		if (lfnName != null) {
			byte checksum = actualEntry.getShortName().calculateCheckSum();
			int entrySize = getEntryCount();

			// long filename is stored in reverse order
			int index = entrySize - 2;
			// first write last entry
			FatDirectoryEntry entry = FatDirectoryEntry.createLfnPart(lfnName, index * 13,
					checksum, index + 1, true);
			entry.serialize(buffer);

			while ((index--) > 0) {
				entry = FatDirectoryEntry.createLfnPart(lfnName, index * 13, checksum, index + 1,
						false);
				entry.serialize(buffer);
			}
		}

		// finally write the actual entry
		actualEntry.serialize(buffer);
	}

	/**
	 * This method returns the entry count needed to store the long file name
	 * and the actual entry.
	 * 
	 * @return The amount of entries.
	 */
	/* package */int getEntryCount() {
		// we always have the actual entry
		int result = 1;

		// if long filename exists add needed entries
		if (lfnName != null) {
			int len = lfnName.length();
			result += len / 13;
			if (len % 13 != 0)
				result++;
		}

		return result;
	}

	/**
	 * Returns the name for this entry. If the long file name is not specified
	 * it returns the short name.
	 * 
	 * @return The name of the entry
	 */
	/* package */String getName() {
		if (lfnName != null)
			return lfnName;
		return actualEntry.getShortName().getString();
	}

	/**
	 * Sets a new long name and the corresponding short name.
	 * 
	 * @param newName
	 *            The new long name.
	 * @param shortName
	 *            The new short name.
	 */
	/* package */void setName(String newName, ShortName shortName) {
		lfnName = newName;
		actualEntry.setShortName(shortName);
	}

	/**
	 * Returns the file size for the actual entry.
	 * 
	 * @return The file size in bytes.
	 * @see #isDirectory()
	 * @see #setFileSize(long)
	 */
	/* package */long getFileSize() {
		return actualEntry.getFileSize();
	}

	/**
	 * Sets the file size in bytes for the actual entry.
	 * 
	 * @param newSize
	 *            The new size in bytes.
	 * @see #isDirectory()
	 * @see #getFileSize()
	 */
	/* package */void setFileSize(long newSize) {
		actualEntry.setFileSize(newSize);
	}

	/**
	 * Gets the start cluster for the actual entry.
	 * 
	 * @return The start cluster.
	 * @see #getStartCluster()
	 */
	/* package */long getStartCluster() {
		return actualEntry.getStartCluster();
	}

	/**
	 * Sets the start cluster for the actual entry.
	 * 
	 * @param newStartCluster
	 *            The new start cluster.
	 * @see #getStartCluster()
	 */
	/* package */void setStartCluster(long newStartCluster) {
		actualEntry.setStartCluster(newStartCluster);
	}

	/**
	 * Sets the last accessed time of the actual entry to now.
	 */
	/* package */void setLastAccessedTimeToNow() {
		actualEntry.setLastAccessedDateTime(System.currentTimeMillis());
	}

	/**
	 * Sets the last modified time of the actual entry to now.
	 */
	/* package */void setLastModifiedTimeToNow() {
		actualEntry.setLastModifiedDateTime(System.currentTimeMillis());
	}

	/**
	 * 
	 * @return True if this entry denotes a directory.
	 */
	/* package */boolean isDirectory() {
		return actualEntry.isDirectory();
	}

	/**
	 * Sets this entry to indicate a directory.
	 */
	/* package */void setDirectory() {
		actualEntry.setDirectory();
	}

	/**
	 * Returns the actual entry which holds the information like start cluster
	 * or file size.
	 * 
	 * @return The actual entry.
	 */
	/* package */FatDirectoryEntry getActualEntry() {
		return actualEntry;
	}

	/**
	 * Copies created, last accessed and last modified date and time fields from
	 * one entry to another.
	 * 
	 * @param from
	 *            The source.
	 * @param to
	 *            The destination.
	 */
	/* package */static void copyDateTime(FatLfnDirectoryEntry from, FatLfnDirectoryEntry to) {
		FatDirectoryEntry actualFrom = from.getActualEntry();
		FatDirectoryEntry actualTo = from.getActualEntry();
		actualTo.setCreatedDateTime(actualFrom.getCreatedDateTime());
		actualTo.setLastAccessedDateTime(actualFrom.getLastAccessedDateTime());
		actualTo.setLastModifiedDateTime(actualFrom.getLastModifiedDateTime());
	}

	@Override
	public String toString() {
		return "[FatLfnDirectoryEntry getName()=" + getName() + "]";
	}
}
