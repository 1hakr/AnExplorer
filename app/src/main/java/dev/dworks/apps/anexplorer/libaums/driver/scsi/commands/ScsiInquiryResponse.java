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
 * This class represents the response of a SCSI Inquiry. It holds various
 * information about the mass storage device.
 * <p>
 * This response is received in the data phase.
 * 
 * @author mjahnen
 * @see ScsiInquiry
 */
public class ScsiInquiryResponse {

	private byte peripheralQualifier;
	private byte peripheralDeviceType;
	boolean removableMedia;
	byte spcVersion;
	byte responseDataFormat;

	private ScsiInquiryResponse() {

	}

	/**
	 * Constructs a new object with the given data.
	 * 
	 * @param buffer
	 *            The data where the {@link #ScsiInquiryResponse()} is located.
	 * @return The parsed {@link #ScsiInquiryResponse()}.
	 */
	public static ScsiInquiryResponse read(ByteBuffer buffer) {
		ScsiInquiryResponse response = new ScsiInquiryResponse();
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		byte b = buffer.get();
		response.peripheralQualifier = (byte) (b & (byte) 0xe0);
		response.peripheralDeviceType = (byte) (b & (byte) 0x1f);
		response.removableMedia = buffer.get() == 0x80;
		response.spcVersion = buffer.get();
		response.responseDataFormat = (byte) (buffer.get() & (byte) 0x7);
		return response;
	}

	/**
	 * 
	 * @return Zero if a device is connected to the unit.
	 */
	public byte getPeripheralQualifier() {
		return peripheralQualifier;
	}

	/**
	 * The type of the mass storage device.
	 * 
	 * @return Zero for a direct access block device.
	 */
	public byte getPeripheralDeviceType() {
		return peripheralDeviceType;
	}

	/**
	 * 
	 * @return True if the media can be removed (eg. card reader).
	 */
	public boolean isRemovableMedia() {
		return removableMedia;
	}

	/**
	 * This method returns the version of the SCSI Primary Commands (SPC)
	 * standard the device supports.
	 * 
	 * @return Version of the SPC standard
	 */
	public byte getSpcVersion() {
		return spcVersion;
	}

	public byte getResponseDataFormat() {
		return responseDataFormat;
	}

	@Override
	public String toString() {
		return "ScsiInquiryResponse [peripheralQualifier=" + peripheralQualifier
				+ ", peripheralDeviceType=" + peripheralDeviceType + ", removableMedia="
				+ removableMedia + ", spcVersion=" + spcVersion + ", responseDataFormat="
				+ responseDataFormat + "]";
	}
}
