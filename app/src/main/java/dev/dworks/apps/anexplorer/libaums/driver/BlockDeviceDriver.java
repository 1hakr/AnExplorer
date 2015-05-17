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

package dev.dworks.apps.anexplorer.libaums.driver;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * This interface describes a simple block device with a certain block size and
 * the ability to read and write at a certain device offset.
 * 
 * @author mjahnen
 * 
 */
public interface BlockDeviceDriver {
	/**
	 * Initializes the block device for further use. This method should be
	 * called before doing anything else on the block device.
	 * 
	 * @throws IOException
	 *             If initializing fails
	 */
	public void init() throws IOException;

	/**
	 * Reads from the block device at a certain offset into the given buffer.
	 * The amount of bytes to be read are determined by
	 * {@link ByteBuffer#remaining()}.
	 * <p>
	 * The deviceOffset can either be the amount of bytes or a logical block
	 * addressing using the block size. To get the bytes in the last case you
	 * have to multiply the lba with the block size (offset *
	 * {@link #getBlockSize()}).
	 * 
	 * @param deviceOffset
	 *            The offset where the reading should begin.
	 * @param buffer
	 *            The buffer where the data should be read into.
	 * @throws IOException
	 *             If reading fails.
	 */
	public void read(long deviceOffset, ByteBuffer buffer) throws IOException;

	/**
	 * Writes to the block device at a certain offset from the given buffer. The
	 * amount of bytes to be written are determined by
	 * {@link ByteBuffer#remaining()}.
	 * <p>
	 * The deviceOffset can either be the amount of bytes or a logical block
	 * addressing using the block size. To get the bytes in the last case you
	 * have to multiply the lba with the block size (offset *
	 * {@link #getBlockSize()}).
	 * 
	 * @param deviceOffset
	 *            The offset where the writing should begin.
	 * @param buffer
	 *            The buffer with the data to be transferred.
	 * @throws IOException
	 *             If writing fails.
	 */
	public void write(long deviceOffset, ByteBuffer buffer) throws IOException;

	/**
	 * Returns the block size of the block device. Every block device can only
	 * read and store bytes in a specific block with a certain size.
	 * <p>
	 * That means that it is only possible to read or write hole blocks!
	 * 
	 * @return The block size in bytes, mostly 512 bytes.
	 */
	public int getBlockSize();
}
