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
 * This command is used to determine if the logical unit of the mass storage
 * device is ready. Sometimes this command fails even if the unit can process
 * all commands successfully. Thus this command issues only a warning in the
 * {@link dev.dworks.apps.anexplorer.libaums.driver.scsi.ScsiBlockDevice}.
 * <p>
 * This command has no data phase, the result is determined by
 * {@link dev.dworks.apps.anexplorer.libaums.driver.scsi.commands.CommandStatusWrapper #getbCswStatus()}.
 * 
 * @author mjahnen
 * 
 */
public class ScsiTestUnitReady extends CommandBlockWrapper {

	private static final byte LENGTH = 0x6;
	private static final byte OPCODE = 0x0;

	public ScsiTestUnitReady() {
		super(0, Direction.NONE, (byte) 0, LENGTH);
	}

	@Override
	public void serialize(ByteBuffer buffer) {
		super.serialize(buffer);
		buffer.put(OPCODE);
	}

}
