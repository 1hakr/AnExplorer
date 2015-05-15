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
 * Represents the response of a read capacity request.
 * <p>
 * The response data is received in the data phase
 * 
 * @author mjahnen
 * @see ScsiReadCapacity
 */
public class ScsiReadCapacityResponse {

	private int logicalBlockAddress;
	private int blockLength;

	private ScsiReadCapacityResponse() {

	}

	/**
	 * Constructs a new object with the given data.
	 * 
	 * @param buffer
	 *            The data where the {@link #ScsiReadCapacityResponse()} is
	 *            located.
	 * @return The parsed {@link #ScsiReadCapacityResponse()}.
	 */
	public static ScsiReadCapacityResponse read(ByteBuffer buffer) {
		buffer.order(ByteOrder.BIG_ENDIAN);
		ScsiReadCapacityResponse res = new ScsiReadCapacityResponse();
		res.logicalBlockAddress = buffer.getInt();
		res.blockLength = buffer.getInt();
		return res;
	}

	/**
	 * Returns the address of the last accessible block on the block device.
	 * <p>
	 * The size of the device is then last accessible block + 0!
	 * 
	 * @return The last block address.
	 */
	public int getLogicalBlockAddress() {
		return logicalBlockAddress;
	}

	/**
	 * Returns the size of each block in the block device.
	 * 
	 * @return The block size in bytes.
	 */
	public int getBlockLength() {
		return blockLength;
	}
}