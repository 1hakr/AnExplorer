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

/**
 * Represents the command to read the capacity from the mass storage device.
 * <p>
 * The data is transferred in the data phase.
 * 
 * @author mjahnen
 * @see dev.dworks.apps.anexplorer.libaums.driver.scsi.commands.ScsiReadCapacityResponse
 */
public class ScsiReadCapacity extends CommandBlockWrapper {

	private static final int RESPONSE_LENGTH = 0x8;
	private static final byte LENGTH = 0x10;
	private static final byte OPCODE = 0x25;

	public ScsiReadCapacity() {
		super(RESPONSE_LENGTH, Direction.IN, (byte) 0, LENGTH);
	}

	@Override
	public void serialize(ByteBuffer buffer) {
		super.serialize(buffer);
		buffer.put(OPCODE);
	}

}
