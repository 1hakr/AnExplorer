package dev.dworks.apps.anexplorer.usb;

import android.annotation.TargetApi;
import android.hardware.usb.UsbDevice;
import android.os.Build;

import java.io.File;

import dev.dworks.apps.anexplorer.misc.Utils;

public class UsbRootFile {

	private String  name = "", permission = "", trimName = "";
	private int length = 0;
    private boolean isAvailable = true;
    private boolean hasPermission = false;
    private int type = 0;
    private String[] flds;
    private String path;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public UsbRootFile(UsbDevice device) {
        this.name = Utils.hasLollipop() ? device.getProductName() : device.getDeviceName();
        this.path = device.getDeviceName() + "";
	}
	
	public boolean isAvailable() {
		return isAvailable;
	}

	public boolean hasPermission() {
		return isAvailable;
	}

	public String getName() {
		return name;
	}
	
	public int length() {
		return length;
	}

	public boolean canWrite() {
		return true;
	}

	public String getPath() {
		return path;
	}

    public String getAbsolutePath() {
        return path;
    }
}