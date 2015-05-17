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
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * This class represents a 8.3 short name of a {@link FatDirectoryEntry}.
 * <p>
 * The short name has 8 characters for the name and 3 characters for the
 * extension. The period between them is not saved in the short name. The short
 * name can only hold eight bit characters. Only upper case characters and these
 * special characters are allowed:
 * <p>
 * For more information regarding short names, please refer to the official
 * FAT32 specification.
 * 
 * @author mjahnen
 * 
 */
/* package */class ShortName {

	private static int SIZE = 11;

	private ByteBuffer data;

	/**
	 * Construct a new short name with the given name and extension. Name length
	 * maximum is 8 and extension maximum length is 3.
	 * 
	 * @param name
	 *            The name, must not be null or empty.
	 * @param extension
	 *            The extension, must not be null, but can be empty.
	 */
	/* package */ShortName(String name, String extension) {
		byte[] tmp = new byte[SIZE];
		// fill with spaces
		Arrays.fill(tmp, (byte) 0x20);

		int length = Math.min(name.length(), 8);

		System.arraycopy(name.getBytes(Charset.forName("ASCII")), 0, tmp, 0, length);
		System.arraycopy(extension.getBytes(Charset.forName("ASCII")), 0, tmp, 8,
				extension.length());

		// 0xe5 means entry deleted, so we have to convert it
		if (tmp[0] == 0xe5) {
			// KANJI lead byte, see Fat32 specification
			tmp[0] = 0x05;
		}

		data = ByteBuffer.wrap(tmp);
	}

	/**
	 * Construct a short name with the given data from a 32 byte
	 * {@link FatDirectoryEntry}.
	 * 
	 * @param data
	 *            The 11 bytes representing the name.
	 */
	private ShortName(ByteBuffer data) {
		this.data = data;
	}

	/**
	 * Construct a short name with the given data from a 32 byte
	 * {@link FatDirectoryEntry}.
	 * 
	 * @param data
	 *            The 32 bytes from the entry.
	 */
	/* package */static ShortName parse(ByteBuffer data) {
		byte[] tmp = new byte[SIZE];
		data.get(tmp);
		return new ShortName(ByteBuffer.wrap(tmp));
	}

	/**
	 * Returns a human readable String of the short name.
	 * 
	 * @return The name.
	 */
	/* package */String getString() {
		final char[] name = new char[8];
		final char[] extension = new char[3];

		for (int i = 0; i < 8; i++) {
			name[i] = (char) (data.get(i) & 0xFF);
		}

		// if first byte is 0x05 it is actually 0xe5 (KANJI lead byte, see Fat32
		// specification)
		// this has to be done because 0xe5 is the magic for an deleted entry
		if (data.get(0) == 0x05) {
			name[0] = (char) 0xe5;
		}

		for (int i = 0; i < 3; i++) {
			extension[i] = (char) (data.get(i + 8) & 0xFF);
		}

		String strName = new String(name).trim();
		String strExt = new String(extension).trim();

		return strExt.isEmpty() ? strName : strName + "." + strExt;
	}

	/**
	 * Serializes the short name so that it can be written to disk. This method
	 * does not alter the position of the given ByteBuffer!
	 * 
	 * @param buffer
	 *            The buffer where the data shall be stored.
	 */
	/* package */void serialize(ByteBuffer buffer) {
		buffer.put(data.array(), 0, SIZE);
	}

	/**
	 * Calculates the checksum of the short name which is needed for the long
	 * file entries.
	 * 
	 * @return The checksum.
	 * @see FatLfnDirectoryEntry
	 * @see FatLfnDirectoryEntry#serialize(ByteBuffer)
	 */
	/* package */byte calculateCheckSum() {
		int sum = 0;

		for (int i = 0; i < SIZE; i++) {
			sum = ((sum & 1) == 1 ? 0x80 : 0) + ((sum & 0xff) >> 1) + data.get(i);
		}

		return (byte) (sum & 0xff);
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof ShortName))
			return false;

		return Arrays.equals(data.array(), ((ShortName) other).data.array());
	}

	@Override
	public String toString() {
		return getString();
	}
}
