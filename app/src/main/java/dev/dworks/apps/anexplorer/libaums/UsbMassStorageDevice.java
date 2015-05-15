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

import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.util.Log;

import dev.dworks.apps.anexplorer.libaums.driver.BlockDeviceDriver;
import dev.dworks.apps.anexplorer.libaums.driver.BlockDeviceDriverFactory;
import dev.dworks.apps.anexplorer.libaums.partition.Partition;
import dev.dworks.apps.anexplorer.libaums.partition.PartitionTable;
import dev.dworks.apps.anexplorer.libaums.partition.PartitionTableEntry;
import dev.dworks.apps.anexplorer.libaums.partition.PartitionTableFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Class representing a connected USB mass storage device. You can enumerate
 * through all connected mass storage devices via
 * {@link #getMassStorageDevices(Context)}. This method only returns supported
 * devices or if no device is connected an empty array.
 * <p>
 * After choosing a device you have to get the permission for the underlying
 * {@link UsbDevice}. The underlying
 * {@link UsbDevice} can be accessed via
 * {@link #getUsbDevice()}.
 * <p>
 * After that you need to call {@link #setupDevice()}. This will initialize the
 * mass storage device and read the partitions (
 * {@link Partition}).
 * <p>
 * The supported partitions can then be accessed via {@link #getPartitions()}
 * and you can begin to read directories and files.
 * 
 * @author mjahnen
 * 
 */
public class UsbMassStorageDevice {

	/**
	 * Usb communication which uses the newer API in Android Jelly Bean MR2 (API
	 * level 18). It just delegates the calls to the {@link UsbDeviceConnection}
	 * .
	 * 
	 * @author mjahnen
	 * 
	 */
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
	private class JellyBeanMr2Communication implements UsbCommunication {
		@Override
		public int bulkOutTransfer(byte[] buffer, int length) {
			return deviceConnection.bulkTransfer(outEndpoint, buffer, length, TRANSFER_TIMEOUT);
		}

		@Override
		public int bulkOutTransfer(byte[] buffer, int offset, int length) {
			return deviceConnection.bulkTransfer(outEndpoint, buffer, offset, length,
					TRANSFER_TIMEOUT);
		}

		@Override
		public int bulkInTransfer(byte[] buffer, int length) {
			return deviceConnection.bulkTransfer(inEndpoint, buffer, length, TRANSFER_TIMEOUT);
		}

		@Override
		public int bulkInTransfer(byte[] buffer, int offset, int length) {
			return deviceConnection.bulkTransfer(inEndpoint, buffer, offset, length,
					TRANSFER_TIMEOUT);
		}
	}

	/**
	 * On Android API level lower 18 (Jelly Bean MR2) we cannot specify a start
	 * offset in the source/destination array. Because of that we have to use
	 * this workaround, where we have to copy the data every time offset is non
	 * zero.
	 * 
	 * @author mjahnen
	 * 
	 */
	private class HoneyCombMr1Communication implements UsbCommunication {
		@Override
		public int bulkOutTransfer(byte[] buffer, int length) {
			return deviceConnection.bulkTransfer(outEndpoint, buffer, length, TRANSFER_TIMEOUT);
		}

		@Override
		public int bulkOutTransfer(byte[] buffer, int offset, int length) {
			if (offset == 0)
				return deviceConnection.bulkTransfer(outEndpoint, buffer, length, TRANSFER_TIMEOUT);

			byte[] tmpBuffer = new byte[length];
			System.arraycopy(buffer, offset, tmpBuffer, 0, length);
			int result = deviceConnection.bulkTransfer(outEndpoint, tmpBuffer, length,
					TRANSFER_TIMEOUT);
			return result;
		}

		@Override
		public int bulkInTransfer(byte[] buffer, int length) {
			return deviceConnection.bulkTransfer(inEndpoint, buffer, length, TRANSFER_TIMEOUT);
		}

		@Override
		public int bulkInTransfer(byte[] buffer, int offset, int length) {
			if (offset == 0)
				return deviceConnection.bulkTransfer(inEndpoint, buffer, length, TRANSFER_TIMEOUT);

			byte[] tmpBuffer = new byte[length];
			int result = deviceConnection.bulkTransfer(inEndpoint, tmpBuffer, length,
					TRANSFER_TIMEOUT);
			System.arraycopy(tmpBuffer, 0, buffer, offset, length);
			return result;
		}
	}

	private static final String TAG = UsbMassStorageDevice.class.getSimpleName();

	/**
	 * subclass 6 means that the usb mass storage device implements the SCSI
	 * transparent command set
	 */
	private static final int INTERFACE_SUBCLASS = 6;

	/**
	 * protocol 80 means the communication happens only via bulk transfers
	 */
	private static final int INTERFACE_PROTOCOL = 80;

	private static int TRANSFER_TIMEOUT = 21000;

	private UsbManager usbManager;
	private UsbDeviceConnection deviceConnection;
	private UsbDevice usbDevice;
	private UsbInterface usbInterface;
	private UsbEndpoint inEndpoint;
	private UsbEndpoint outEndpoint;

	private BlockDeviceDriver blockDevice;
	private PartitionTable partitionTable;
	private List<Partition> partitions = new ArrayList<Partition>();

	/**
	 * Construct a new {@link UsbMassStorageDevice}.
	 * The given parameters have to actually be a mass storage device, this is
	 * not checked in the constructor!
	 * 
	 * @param usbManager
	 * @param usbDevice
	 * @param usbInterface
	 * @param inEndpoint
	 * @param outEndpoint
	 */
	private UsbMassStorageDevice(UsbManager usbManager, UsbDevice usbDevice,
			UsbInterface usbInterface, UsbEndpoint inEndpoint, UsbEndpoint outEndpoint) {
		this.usbManager = usbManager;
		this.usbDevice = usbDevice;
		this.usbInterface = usbInterface;
		this.inEndpoint = inEndpoint;
		this.outEndpoint = outEndpoint;
	}

	/**
	 * This method iterates through all connected USB devices and searches for
	 * mass storage devices.
	 * 
	 * @param context
	 *            Context to get the {@link UsbManager}
	 * @return An array of suitable mass storage devices or an empty array if
	 *         none could be found.
	 */
	public static UsbMassStorageDevice[] getMassStorageDevices(Context context) {
		UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
		ArrayList<UsbMassStorageDevice> result = new ArrayList<UsbMassStorageDevice>();

		for (UsbDevice device : usbManager.getDeviceList().values()) {
			Log.i(TAG, "found usb device: " + device);

			int interfaceCount = device.getInterfaceCount();
			for (int i = 0; i < interfaceCount; i++) {
				UsbInterface usbInterface = device.getInterface(i);
				Log.i(TAG, "found usb interface: " + usbInterface);

				// we currently only support SCSI transparent command set with
				// bulk transfers only!
				if (usbInterface.getInterfaceClass() != UsbConstants.USB_CLASS_MASS_STORAGE
						|| usbInterface.getInterfaceSubclass() != INTERFACE_SUBCLASS
						|| usbInterface.getInterfaceProtocol() != INTERFACE_PROTOCOL) {
					Log.i(TAG, "device interface not suitable!");
					continue;
				}

				// Every mass storage device has exactly two endpoints
				// One IN and one OUT endpoint
				int endpointCount = usbInterface.getEndpointCount();
				if (endpointCount != 2) {
					Log.w(TAG, "inteface endpoint count != 2");
				}

				UsbEndpoint outEndpoint = null;
				UsbEndpoint inEndpoint = null;
				for (int j = 0; j < endpointCount; j++) {
					UsbEndpoint endpoint = usbInterface.getEndpoint(j);
					Log.i(TAG, "found usb endpoint: " + endpoint);
					if(endpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
						if (endpoint.getDirection() == UsbConstants.USB_DIR_OUT) {
						    outEndpoint = endpoint;
						} else {
						    inEndpoint = endpoint;
						}
					}
				}

				if (outEndpoint == null || inEndpoint == null) {
					Log.e(TAG, "Not all needed endpoints found!");
					continue;
				}

				result.add(new UsbMassStorageDevice(usbManager, device, usbInterface, inEndpoint,
						outEndpoint));

			}
		}

		return result.toArray(new UsbMassStorageDevice[0]);
	}

	/**
	 * Initializes the mass storage device and determines different things like
	 * for example the MBR or the file systems for the different partitions.
	 * 
	 * @throws IOException
	 *             If reading from the physical device fails.
	 * @throws IllegalStateException
	 *             If permission to communicate with the underlying
	 *             {@link UsbDevice} is missing.
	 * @see #getUsbDevice()
	 */
	public void init() throws IOException {
		if (usbManager.hasPermission(usbDevice))
			setupDevice();
		else
			throw new IllegalStateException("Missing permission to access usb device: " + usbDevice);

	}

	/**
	 * Sets the device up. Claims interface and initiates the device connection.
	 * Chooses the right{@link UsbCommunication}
	 * depending on the Android version (
	 * {@link HoneyCombMr1Communication}
	 * or (
	 * {@link JellyBeanMr2Communication}
	 * ).
	 * <p>
	 * Initializes the {@link #blockDevice} and reads the partitions.
	 * 
	 * @throws IOException
	 *             If reading from the physical device fails.
	 * @see #init()
	 */
	private void setupDevice() throws IOException {
		Log.d(TAG, "setup device");
		deviceConnection = usbManager.openDevice(usbDevice);
		if (deviceConnection == null) {
			Log.e(TAG, "deviceConnetion is null!");
			return;
		}

		boolean claim = deviceConnection.claimInterface(usbInterface, true);
		if (!claim) {
			Log.e(TAG, "could not claim interface!");
			return;
		}

		UsbCommunication communication;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
			communication = new JellyBeanMr2Communication();
		} else {
			Log.i(TAG, "using workaround usb communication");
			communication = new HoneyCombMr1Communication();
		}

		blockDevice = BlockDeviceDriverFactory.createBlockDevice(communication);
		blockDevice.init();
		partitionTable = PartitionTableFactory.createPartitionTable(blockDevice);
		initPartitions();
	}

	/**
	 * Fills {@link #partitions} with the information received by the
	 * {@link #partitionTable}.
	 * 
	 * @throws IOException
	 *             If reading from the {@link #blockDevice} fails.
	 */
	private void initPartitions() throws IOException {
		Collection<PartitionTableEntry> partitionEntrys = partitionTable.getPartitionTableEntries();

		for (PartitionTableEntry entry : partitionEntrys) {
			Partition partition = Partition.createPartition(entry, blockDevice);
			if (partition != null) {
				partitions.add(partition);
			}
		}
	}

	/**
	 * Releases the {@link UsbInterface} and closes the
	 * {@link UsbDeviceConnection}. After calling this
	 * method no further communication is possible. That means you can not read
	 * or write from or to the partitions returned by {@link #getPartitions()}.
	 */
	public void close() {
		Log.d(TAG, "close device");
		if(deviceConnection == null) return;
		
		boolean release = deviceConnection.releaseInterface(usbInterface);
		if (!release) {
			Log.e(TAG, "could not release interface!");
		}
		deviceConnection.close();
	}

	/**
	 * Returns the available partitions of the mass storage device. You have to
	 * call {@link #init()} before calling this method!
	 * 
	 * @return List of partitions.
	 */
	public List<Partition> getPartitions() {
		return partitions;
	}

	/**
	 * This returns the {@link UsbDevice} which can be used
	 * to request permission for communication.
	 * 
	 * @return Underlying {@link UsbDevice} used for
	 *         communication.
	 */
	public UsbDevice getUsbDevice() {
		return usbDevice;
	}
}
