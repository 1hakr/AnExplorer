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
 * This class represents a SCSI Inquiry command. It is used to get important
 * information about the connected mass storage device. This information include
 * the supported SCSI commands.
 * <p>
 * The response is sent in the data phase.
 * 
 * @author mjahnen
 * @see dev.dworks.apps.anexplorer.libaums.driver.scsi.commands.ScsiInquiryResponse
 */
public class ScsiInquiry extends CommandBlockWrapper {

	private static final int RESPONSE_LENGTH = 0x24;
	private static final byte LENGTH = 0x6;
	private static final byte OPCODE = 0x12;

	public ScsiInquiry() {
		super(RESPONSE_LENGTH, Direction.IN, (byte) 0, LENGTH);
	}

	@Override
	public void serialize(ByteBuffer buffer) {
		super.serialize(buffer);
		buffer.put(OPCODE);
		buffer.put((byte) 0);
		buffer.put((byte) 0);
		buffer.put((byte) 0);
		buffer.put((byte) LENGTH);
	}

}
