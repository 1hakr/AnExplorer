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

package dev.dworks.apps.anexplorer.libaums;

/**
 * This Interface describes a low level device to perform USB transfers. At the
 * moment only bulk IN and OUT transfer are supported. Every class that follows
 * {@link dev.dworks.apps.anexplorer.libaums.driver.BlockDeviceDriver} can use this to
 * communicate with the underlying USB stack.
 * 
 * @author mjahnen
 * 
 */
public interface UsbCommunication {
	/**
	 * Performs a bulk out transfer beginning at offset zero in the
	 * <code>buffer</code> with the given <code>length</code>.
	 * <p>
	 * This is mostly equivalent to the call
	 * <code>bulkOutTransfer(buffer, 0, length)</code>.
	 * 
	 * @param buffer
	 *            The data to transfer.
	 * @param length
	 *            Amount of bytes to transfer.
	 * @return Bytes transmitted if successful, or -1.
	 * @see #bulkInTransfer(byte[], int, int)
	 */
	public int bulkOutTransfer(byte[] buffer, int length);

	/**
	 * Performs a bulk out transfer beginning at the given offset in the
	 * <code>buffer</code> with the given <code>length</code>.
	 * 
	 * @param buffer
	 *            The data to transfer.
	 * @param offset
	 *            Starting point to transfer data in the <code>buffer</code>
	 *            array.
	 * @param length
	 *            Amount of bytes to transfer.
	 * @return Bytes transmitted if successful, or -1.
	 * @see #bulkInTransfer(byte[], int)
	 */
	public int bulkOutTransfer(byte[] buffer, int offset, int length);

	/**
	 * Performs a bulk in transfer beginning at offset zero in the
	 * <code>buffer</code> with the given <code>length</code>.
	 * <p>
	 * This is mostly equivalent to the call
	 * <code>bulkInTransfer(buffer, 0, length)</code>.
	 * 
	 * @param buffer
	 *            The buffer where data should be transferred.
	 * @param length
	 *            Amount of bytes to transfer.
	 * @return Bytes read if successful, or -1.
	 * @see #bulkInTransfer(byte[], int, int)
	 */
	public int bulkInTransfer(byte[] buffer, int length);

	/**
	 * Performs a bulk in transfer beginning at the given offset in the
	 * <code>buffer</code> with the given <code>length</code>.
	 * 
	 * @param buffer
	 *            The buffer where data should be transferred.
	 * @param offset
	 *            Starting point to transfer data into the <code>buffer</code>
	 *            array.
	 * @param length
	 *            Amount of bytes to transfer.
	 * @return Bytes read if successful, or -1.
	 * @see #bulkInTransfer(byte[], int)
	 */
	public int bulkInTransfer(byte[] buffer, int offset, int length);
}
