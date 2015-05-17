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

package dev.dworks.apps.anexplorer.libaums.driver.scsi.commands;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * SCSI command to write to the mass storage device. The 10 means that the
 * transfer length is two byte and the logical block address field is four byte.
 * Thus the hole command takes 10 byte when serialized.
 * <p>
 * The actual data is transferred in the data phase.
 * 
 * @author mjahnen
 * 
 */
public class ScsiWrite10 extends CommandBlockWrapper {

	// private static final String TAG = ScsiWrite10.class.getSimpleName();
	private static final byte LENGTH = 0x10;
	private static final byte OPCODE = 0x2a;

	private int blockAddress;
	private int transferBytes;
	private int blockSize;
	private short transferBlocks;

	/**
	 * Constructs a new write command with the given information.
	 * 
	 * @param blockAddress
	 *            The logical block address the write should start.
	 * @param transferBytes
	 *            The bytes which should be transferred.
	 * @param blockSize
	 *            The block size of the mass storage device.
	 */
	public ScsiWrite10(int blockAddress, int transferBytes, int blockSize) {
		super(transferBytes, Direction.OUT, (byte) 0, LENGTH);
		this.blockAddress = blockAddress;
		this.transferBytes = transferBytes;
		this.blockSize = blockSize;
		short transferBlocks = (short) (transferBytes / blockSize);
		if (transferBytes % blockSize != 0) {
			throw new IllegalArgumentException("transfer bytes is not a multiple of block size");
		}
		this.transferBlocks = transferBlocks;
	}

	@Override
	public void serialize(ByteBuffer buffer) {
		super.serialize(buffer);
		buffer.order(ByteOrder.BIG_ENDIAN);
		buffer.put(OPCODE);
		buffer.put((byte) 0);
		buffer.putInt(blockAddress);
		buffer.put((byte) 0);
		buffer.putShort(transferBlocks);
	}

	@Override
	public String toString() {
		return "ScsiWrite10 [blockAddress=" + blockAddress + ", transferBytes=" + transferBytes
				+ ", blockSize=" + blockSize + ", transferBlocks=" + transferBlocks
				+ ", getdCbwDataTransferLength()=" + getdCbwDataTransferLength() + "]";
	}

}
