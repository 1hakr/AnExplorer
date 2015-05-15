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
import java.nio.charset.Charset;
import java.util.Calendar;

/**
 * This class holds the information of an 32 byte FAT32 directory entry. It
 * holds information such as if an entry is a directory, read only or hidden.
 * <p>
 * There are three cases a {@link #FatDirectoryEntry()} is used:
 * <ul>
 * <li>To store information about a directory or a file.
 * <li>To store a part of a long filename. Every entry can store up to 13 bytes
 * of a long file name.
 * <li>To store a volume label which can occur in the root directory of a FAT32
 * file system.
 * </ul>
 * <p>
 * To determine if the entry denotes a volume label entry use
 * {@link #isVolumeLabel()}. If this method returns true only the method
 * {@link #getVolumeLabel()} makes sense to call. Calling any other method the
 * results will be undefined.
 * <p>
 * To determine if an entry is an entry for a long file name use
 * {@link #isLfnEntry()}. If this method returns true only the method
 * {@link #extractLfnPart(StringBuilder)} makes sense to call. Calling any other
 * method the results will be undefined.
 * <p>
 * In all other cases the entry is either a file or a directory which can be
 * determined by {@link #isDirectory()}. Further information of the file or
 * directory give for example {@link #getCreatedDateTime()} or
 * {@link #getStartCluster()} to access the contents.
 * 
 * @author mjahnen
 * 
 */
/* package */class FatDirectoryEntry {

	/* package */final static int SIZE = 32;

	private static final int ATTR_OFF = 0x0b;
	private static final int FILE_SIZE_OFF = 0x1c;
	private static final int MSB_CLUSTER_OFF = 0x14;
	private static final int LSB_CLUSTER_OFF = 0x1a;
	private static final int CREATED_DATE_OFF = 0x10;
	private static final int CREATED_TIME_OFF = 0x0e;
	private static final int LAST_WRITE_DATE_OFF = 0x18;
	private static final int LAST_WRITE_TIME_OFF = 0x16;
	private static final int LAST_ACCESSED_DATE_OFF = 0x12;

	private static final int FLAG_READONLY = 0x01;
	private static final int FLAG_HIDDEN = 0x02;
	private static final int FLAG_SYSTEM = 0x04;
	private static final int FLAG_VOLUME_ID = 0x08;
	private static final int FLAG_DIRECTORY = 0x10;
	private static final int FLAG_ARCHIVE = 0x20;

	/* package */static final int ENTRY_DELETED = 0xe5;

	/**
	 * Holds the data like it would be stored on the disk.
	 */
	private ByteBuffer data;
	/**
	 * The 8.3 short name of the entry, when entry represents a directory or
	 * file.
	 */
	private ShortName shortName;

	private FatDirectoryEntry() {

	}

	/**
	 * Constructs a new {@link #FatDirectoryEntry()} with the given data.
	 * 
	 * @param data
	 *            The buffer where entry is located.
	 */
	private FatDirectoryEntry(ByteBuffer data) {
		this.data = data;
		data.order(ByteOrder.LITTLE_ENDIAN);
		shortName = ShortName.parse(data);
		// clear buffer because short name took 13 bytes
		data.clear();
	}

	/**
	 * Creates a completely new {@link #FatDirectoryEntry()}. Do not forget to
	 * set the start cluster! The time fields ({@link #setCreatedDateTime(long)}
	 * , {@link #setLastAccessedDateTime(long)},
	 * {@link #setLastModifiedDateTime(long)}) are all set to the current time.
	 * 
	 * @return The newly constructed entry.
	 * @see #setStartCluster(long)
	 * @see #setDirectory()
	 */
	/* package */static FatDirectoryEntry createNew() {
		FatDirectoryEntry result = new FatDirectoryEntry();
		result.data = ByteBuffer.allocate(SIZE);

		long now = System.currentTimeMillis();
		result.setCreatedDateTime(now);
		result.setLastAccessedDateTime(now);
		result.setLastModifiedDateTime(now);

		return result;
	}

	/**
	 * Reads a directory {@link #FatDirectoryEntry()} from the given buffer and
	 * updates the position of the buffer if successful! Returns
	 * <code>null</code> if there are no further entries.
	 * 
	 * @param data
	 *            The buffer where the entries are located.
	 * @return Newly constructed entry.
	 */
	/* package */static FatDirectoryEntry read(ByteBuffer data) {
		byte[] buffer = new byte[SIZE];

		if (data.get(data.position()) == 0)
			return null;

		data.get(buffer);

		return new FatDirectoryEntry(ByteBuffer.wrap(buffer));
	}

	/**
	 * Serializes this {@link #FatDirectoryEntry()} so that it can be written to
	 * disk. Updates the position of the given buffer.
	 * 
	 * @param buffer
	 *            The buffer data shall be written to.
	 */
	/* package */void serialize(ByteBuffer buffer) {
		buffer.put(data.array());
	}

	/**
	 * Returns the flags in the {@link #FatDirectoryEntry()}.
	 * 
	 * @return The flag variable.
	 * @see #setFlag(int)
	 * @see #FLAG_ARCHIVE
	 * @see #FLAG_DIRECTORY
	 * @see #FLAG_HIDDEN
	 * @see #FLAG_SYSTEM
	 * @see #FLAG_READONLY
	 * @see #FLAG_VOLUME_ID
	 */
	private int getFlags() {
		return data.get(ATTR_OFF);
	}

	/**
	 * Sets a specific flag.
	 * 
	 * @param flag
	 *            The flag to set.
	 * @see #getFlags()
	 * @see #FLAG_ARCHIVE
	 * @see #FLAG_DIRECTORY
	 * @see #FLAG_HIDDEN
	 * @see #FLAG_SYSTEM
	 * @see #FLAG_READONLY
	 * @see #FLAG_VOLUME_ID
	 */
	private void setFlag(int flag) {
		int flags = getFlags();
		data.put(ATTR_OFF, (byte) (flag | flags));
	}

	/**
	 * Returns true if a specific flag is currently set.
	 * 
	 * @param flag
	 *            The flag to be checked.
	 * @return True if the flag is set.
	 * @see #getFlags()
	 * @see #setFlag(int)
	 * @see #FLAG_ARCHIVE
	 * @see #FLAG_DIRECTORY
	 * @see #FLAG_HIDDEN
	 * @see #FLAG_SYSTEM
	 * @see #FLAG_READONLY
	 * @see #FLAG_VOLUME_ID
	 */
	private boolean isFlagSet(int flag) {
		return (getFlags() & flag) != 0;
	}

	/**
	 * Returns true if the current {@link #FatDirectoryEntry()} is an long
	 * filename entry.
	 * 
	 * @return True if the entry is a long filename entry.
	 * @see #extractLfnPart(StringBuilder)
	 * @see #createLfnPart(String, int, byte, int, boolean)
	 */
	/* package */boolean isLfnEntry() {
		return isHidden() && isVolume() && isReadOnly() && isSystem();
	}

	/**
	 * Returns true if the current {@link #FatDirectoryEntry()} denotes a
	 * directory.
	 * 
	 * @return True if entry is a directory.
	 */
	/* package */boolean isDirectory() {
		return ((getFlags() & (FLAG_DIRECTORY | FLAG_VOLUME_ID)) == FLAG_DIRECTORY);
	}

	/**
	 * Sets a mark that indicates that this {@link #FatDirectoryEntry()} shall
	 * denote a directory.
	 */
	/* package */void setDirectory() {
		setFlag(FLAG_DIRECTORY);
	}

	/**
	 * Returns true if the current {@link #FatDirectoryEntry()} denotes a volume
	 * label.
	 * 
	 * @return True if entry is a volume label.
	 * @see #getVolumeLabel()
	 * @see #createVolumeLabel(String)
	 */
	/* package */boolean isVolumeLabel() {
		if (isLfnEntry())
			return false;
		else
			return ((getFlags() & (FLAG_DIRECTORY | FLAG_VOLUME_ID)) == FLAG_VOLUME_ID);
	}

	/**
	 * Returns true if the current {@link #FatDirectoryEntry()} is a system file
	 * or directory. Normally a user shall not see this item!
	 * 
	 * @return True if entry is a system item.
	 */
	/* package */boolean isSystem() {
		return isFlagSet(FLAG_SYSTEM);
	}

	/**
	 * Returns true if the current {@link #FatDirectoryEntry()} is hidden. This
	 * entry should only be accessible by the user if he explicitly asks for it!
	 * 
	 * @return True if entry is hidden.
	 */
	/* package */boolean isHidden() {
		return isFlagSet(FLAG_HIDDEN);
	}

	/**
	 * Returns true if the current {@link #FatDirectoryEntry()} is an archive.
	 * This is used by backup tools.
	 * 
	 * @return True if entry is an archive.
	 */
	/* package */boolean isArchive() {
		return isFlagSet(FLAG_ARCHIVE);
	}

	/**
	 * Returns true if the current {@link #FatDirectoryEntry()} is a read only
	 * file or directory. Normally a user shall not be able to write or to alter
	 * this item!
	 * 
	 * @return True if entry is read only.
	 */
	/* package */boolean isReadOnly() {
		return isFlagSet(FLAG_READONLY);
	}

	/**
	 * Returns true if the volume id flag is set.
	 * 
	 * @return True if volume id set.
	 * @see #FLAG_VOLUME_ID
	 */
	/* package */boolean isVolume() {
		return isFlagSet(FLAG_VOLUME_ID);
	}

	/**
	 * Returns true if the {@link #FatDirectoryEntry()} was deleted and shall
	 * not show up in the file tree.
	 * 
	 * @return True if entry was deleted.
	 */
	/* package */boolean isDeleted() {
		return getUnsignedInt8(0) == ENTRY_DELETED;
	}

	/**
	 * Returns the time this {@link #FatDirectoryEntry()} was created.
	 * 
	 * @return Time in milliseconds since January 1 00:00:00, 1970 UTC
	 */
	/* package */long getCreatedDateTime() {
		return decodeDateTime(getUnsignedInt16(CREATED_DATE_OFF),
				getUnsignedInt16(CREATED_TIME_OFF));
	}

	/**
	 * Sets the time this {@link #FatDirectoryEntry()} was created.
	 * 
	 * @param dateTime
	 *            Time in milliseconds since January 1 00:00:00, 1970 UTC
	 */
	/* package */void setCreatedDateTime(long dateTime) {
		// TODO entry has also field which holds 10th seconds created
		setUnsignedInt16(CREATED_DATE_OFF, encodeDate(dateTime));
		setUnsignedInt16(CREATED_TIME_OFF, encodeTime(dateTime));
	}

	/**
	 * Returns the time this {@link #FatDirectoryEntry()} was accessed the last
	 * time.
	 * 
	 * @return Time in milliseconds since January 1 00:00:00, 1970 UTC
	 */
	/* package */long getLastAccessedDateTime() {
		return decodeDateTime(getUnsignedInt16(LAST_WRITE_DATE_OFF),
				getUnsignedInt16(LAST_WRITE_TIME_OFF));
	}

	/**
	 * Sets the time this {@link #FatDirectoryEntry()} was accessed the last
	 * time.
	 * 
	 * @param dateTime
	 *            Time in milliseconds since January 1 00:00:00, 1970 UTC
	 */
	/* package */void setLastAccessedDateTime(long dateTime) {
		setUnsignedInt16(LAST_WRITE_DATE_OFF, encodeDate(dateTime));
		setUnsignedInt16(LAST_WRITE_TIME_OFF, encodeTime(dateTime));
	}

	/**
	 * Returns the time this {@link #FatDirectoryEntry()} was modified the last
	 * time.
	 * 
	 * @return Time in milliseconds since January 1 00:00:00, 1970 UTC
	 */
	/* package */long getLastModifiedDateTime() {
		return decodeDateTime(getUnsignedInt16(LAST_ACCESSED_DATE_OFF), 0);
	}

	/**
	 * Sets the time this {@link #FatDirectoryEntry()} was modified the last
	 * time.
	 * 
	 * @param dateTime
	 *            Time in milliseconds since January 1 00:00:00, 1970 UTC.
	 */
	/* package */void setLastModifiedDateTime(long dateTime) {
		setUnsignedInt16(LAST_ACCESSED_DATE_OFF, encodeDate(dateTime));
	}

	/**
	 * Returns the short name of the {@link #FatDirectoryEntry()}.
	 * 
	 * @return Newly constructed short name.
	 * @see #setShortName(ShortName)
	 */
	/* package */ShortName getShortName() {
		if (data.get(0) == 0)
			return null;
		else {
			return shortName;
		}
	}

	/**
	 * Sets the short name for this {@link #FatDirectoryEntry()} and writes it
	 * to the data array.
	 * 
	 * @param shortName
	 *            The new short name.
	 * @see #getShortName()
	 */
	/* package */void setShortName(ShortName shortName) {
		this.shortName = shortName;
		shortName.serialize(data);
		// clear buffer because short name put 13 bytes
		data.clear();
	}

	/**
	 * Creates a new {@link #FatDirectoryEntry()} to hold the volume directory
	 * in the root directory of a FAT32 file system.
	 * 
	 * @param volumeLabel
	 *            The volume label.
	 * @return Newly constructed entry for the volume label.
	 * @see #getVolumeLabel()
	 * @see #isVolumeLabel()
	 */
	/* package */static FatDirectoryEntry createVolumeLabel(String volumeLabel) {
		FatDirectoryEntry result = new FatDirectoryEntry();
		ByteBuffer buffer = ByteBuffer.allocate(SIZE);
		buffer.order(ByteOrder.LITTLE_ENDIAN);

		System.arraycopy(volumeLabel.getBytes(Charset.forName("ASCII")), 0, buffer.array(), 0,
				volumeLabel.length());

		result.data = buffer;
		result.setFlag(FLAG_VOLUME_ID);

		return result;
	}

	/**
	 * Returns the volume label which can occur in the root directory of a FAT32
	 * file system.
	 * 
	 * @return The volume label.
	 * @see #createVolumeLabel(String)
	 * @see #isVolumeLabel()
	 */
	/* package */String getVolumeLabel() {
		StringBuilder builder = new StringBuilder();

		for (int i = 0; i < 11; i++) {
			byte b = data.get(i);
			if (b == 0)
				break;
			builder.append((char) b);
		}

		return builder.toString();
	}

	/**
	 * Returns the start cluster for this {@link #FatDirectoryEntry()}. The
	 * start cluster denotes where a file or a directory start in the fs.
	 * 
	 * @return The start cluster.
	 * @see #setStartCluster(long)
	 */
	/* package */long getStartCluster() {
		final int msb = getUnsignedInt16(MSB_CLUSTER_OFF);
		final int lsb = getUnsignedInt16(LSB_CLUSTER_OFF);
		return (msb << 16) | lsb;
	}

	/**
	 * Sets the start cluster for this {@link #FatDirectoryEntry()}. The start
	 * cluster denotes where a file or a directory start in the fs.
	 * 
	 * @param newStartCluster
	 *            The start cluster
	 * @see #getStartCluster()
	 */
	/* package */void setStartCluster(long newStartCluster) {
		setUnsignedInt16(MSB_CLUSTER_OFF, (int) ((newStartCluster >> 16) & 0xffff));
		setUnsignedInt16(LSB_CLUSTER_OFF, (int) (newStartCluster & 0xffff));
	}

	/**
	 * Returns the size of a file in bytes. For directories this value should
	 * always be zero.
	 * 
	 * @return The file size in bytes.
	 * @see #setFileSize(long)
	 */
	/* package */long getFileSize() {
		return getUnsignedInt32(FILE_SIZE_OFF);
	}

	/**
	 * Sets the size of a file in bytes. For directories this value should
	 * always be zero.
	 * 
	 * @param newSize
	 *            The file size in bytes.
	 * @see #getFileSize()
	 */
	/* package */void setFileSize(long newSize) {
		setUnsignedInt32(FILE_SIZE_OFF, newSize);
	}

	/**
	 * This method creates an {@link #FatDirectoryEntry()} for a long file name.
	 * Every entry can store up to 13 bytes for the long file name, thus the
	 * name has sometimes to be split in more parts.
	 * 
	 * @param unicode
	 *            The complete unicode String denoting the long filename.
	 * @param offset
	 *            The offset where the part shall begin.
	 * @param checksum
	 *            The checksum of the short name (
	 *            {@link ShortName#calculateCheckSum()}).
	 * @param index
	 *            The index of this entry, starting at one.
	 * @param isLast
	 *            True if this is the last entry.
	 * @return The newly constructed entry holding the lfn part.
	 * @see #extractLfnPart(StringBuilder)
	 * @see #isLfnEntry()
	 */
	/* package */static FatDirectoryEntry createLfnPart(String unicode, int offset, byte checksum,
			int index, boolean isLast) {
		FatDirectoryEntry result = new FatDirectoryEntry();

		if (isLast) {
			int diff = unicode.length() - offset;
			if (diff < 13) {
				StringBuilder builder = new StringBuilder(13);
				builder.append(unicode, offset, unicode.length());
				// end mark
				builder.append('\0');

				// fill with 0xffff
				for (int i = 0; i < 13 - diff; i++) {
					builder.append((char) 0xffff);
				}

				offset = 0;
				unicode = builder.toString();
			}
		}

		ByteBuffer buffer = ByteBuffer.allocate(SIZE);
		buffer.order(ByteOrder.LITTLE_ENDIAN);

		buffer.put(0, (byte) (isLast ? index + (1 << 6) : index));
		buffer.putShort(1, (short) unicode.charAt(offset));
		buffer.putShort(3, (short) unicode.charAt(offset + 1));
		buffer.putShort(5, (short) unicode.charAt(offset + 2));
		buffer.putShort(7, (short) unicode.charAt(offset + 3));
		buffer.putShort(9, (short) unicode.charAt(offset + 4));
		// Special mark for lfn entry
		buffer.put(11, (byte) (FLAG_HIDDEN | FLAG_VOLUME_ID | FLAG_READONLY | FLAG_SYSTEM));
		// unused
		buffer.put(12, (byte) 0);
		buffer.put(13, checksum);
		buffer.putShort(14, (short) unicode.charAt(offset + 5));
		buffer.putShort(16, (short) unicode.charAt(offset + 6));
		buffer.putShort(18, (short) unicode.charAt(offset + 7));
		buffer.putShort(20, (short) unicode.charAt(offset + 8));
		buffer.putShort(22, (short) unicode.charAt(offset + 9));
		buffer.putShort(24, (short) unicode.charAt(offset + 10));
		// unused
		buffer.putShort(26, (short) 0);
		buffer.putShort(28, (short) unicode.charAt(offset + 11));
		buffer.putShort(30, (short) unicode.charAt(offset + 12));

		result.data = buffer;

		return result;
	}

	/**
	 * This method extracts the long filename part of the
	 * {@link #FatDirectoryEntry()}. It appends the long filename part to the
	 * StringBuilder given.
	 * 
	 * @param builder
	 *            The builder where the long filename part shall be appended.
	 * @see #createLfnPart(String, int, byte, int, boolean)
	 * @see #isLfnEntry()
	 */
	/* package */void extractLfnPart(StringBuilder builder) {
		final char[] name = new char[13];
		name[0] = (char) data.getShort(1);
		name[1] = (char) data.getShort(3);
		name[2] = (char) data.getShort(5);
		name[3] = (char) data.getShort(7);
		name[4] = (char) data.getShort(9);
		name[5] = (char) data.getShort(14);
		name[6] = (char) data.getShort(16);
		name[7] = (char) data.getShort(18);
		name[8] = (char) data.getShort(20);
		name[9] = (char) data.getShort(22);
		name[10] = (char) data.getShort(24);
		name[11] = (char) data.getShort(28);
		name[12] = (char) data.getShort(30);

		int len = 0;
		while (len < 13 && name[len] != '\0')
			len++;

		builder.append(name, 0, len);
	}

	private int getUnsignedInt8(int offset) {
		return data.get(offset) & 0xff;
	}

	private int getUnsignedInt16(int offset) {
		final int i1 = data.get(offset) & 0xff;
		final int i2 = data.get(offset + 1) & 0xff;
		return (i2 << 8) | i1;
	}

	private int getUnsignedInt32(int offset) {
		final int i1 = data.get(offset) & 0xff;
		final int i2 = data.get(offset + 1) & 0xff;
		final int i3 = data.get(offset + 2) & 0xff;
		final int i4 = data.get(offset + 3) & 0xff;
		return (i4 << 24) | (i3 << 16) | (i2 << 8) | i1;
	}

	private void setUnsignedInt16(int offset, int value) {
		data.put(offset, (byte) (value & 0xff));
		data.put(offset + 1, (byte) ((value >>> 8) & 0xff));
	}

	private void setUnsignedInt32(int offset, long value) {
		data.put(offset, (byte) (value & 0xff));
		data.put(offset + 1, (byte) ((value >>> 8) & 0xff));
		data.put(offset + 2, (byte) ((value >>> 16) & 0xff));
		data.put(offset + 3, (byte) ((value >>> 24) & 0xff));
	}

	/**
	 * This method decodes a timestamp from an {@link #FatDirectoryEntry()}.
	 * 
	 * @param date
	 *            The data of the entry.
	 * @param time
	 *            The time of the entry.
	 * @return Time in milliseconds since January 1 00:00:00, 1970 UTC
	 */
	private static long decodeDateTime(int date, int time) {
		final Calendar calendar = Calendar.getInstance();

		calendar.set(Calendar.YEAR, 1980 + (date >> 9));
		calendar.set(Calendar.MONTH, ((date >> 5) & 0x0f) - 1);
		calendar.set(Calendar.DAY_OF_MONTH, date & 0x0f);
		calendar.set(Calendar.HOUR_OF_DAY, time >> 11);
		calendar.set(Calendar.MINUTE, (time >> 5) & 0x3f);
		calendar.set(Calendar.SECOND, (time & 0x1f) * 2);

		return calendar.getTimeInMillis();
	}

	/**
	 * This method encodes the date given to a timestamp suitable for an
	 * {@link #FatDirectoryEntry()}.
	 * 
	 * @param timeInMillis
	 *            Time in milliseconds since January 1 00:00:00, 1970 UTC.
	 * @return The date suitable to store in an #{@link FatDirectoryEntry}.
	 */
	private static int encodeDate(long timeInMillis) {
		final Calendar calendar = Calendar.getInstance();

		calendar.setTimeInMillis(timeInMillis);

		return ((calendar.get(Calendar.YEAR) - 1980) << 9)
				+ ((calendar.get(Calendar.MONTH) + 1) << 5) + calendar.get(Calendar.DAY_OF_MONTH);

	}

	/**
	 * This method encodes the time given to a timestamp suitable for an
	 * {@link #FatDirectoryEntry()}.
	 * 
	 * @param timeInMillis
	 *            Time in milliseconds since January 1 00:00:00, 1970 UTC.
	 * @return The time suitable to store in an #{@link FatDirectoryEntry}.
	 */
	private static int encodeTime(long timeInMillis) {
		final Calendar calendar = Calendar.getInstance();

		calendar.setTimeInMillis(timeInMillis);

		return (calendar.get(Calendar.HOUR_OF_DAY) << 11) + (calendar.get(Calendar.MINUTE) << 5)
				+ calendar.get(Calendar.SECOND) / 2;

	}

	@Override
	public String toString() {
		return "[FatDirectoryEntry shortName=" + shortName.getString() + "]";
	}
}
