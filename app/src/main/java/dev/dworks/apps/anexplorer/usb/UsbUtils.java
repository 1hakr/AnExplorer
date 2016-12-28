package dev.dworks.apps.anexplorer.usb;

import android.annotation.TargetApi;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.os.Build;

import dev.dworks.apps.anexplorer.misc.Utils;

/**
 * Created by HaKr on 17/01/15.
 */
public class UsbUtils {
        /* Helper Methods to Provide Readable Names for USB Constants */

    private String nameForClass(int classType) {
        switch (classType) {
            case UsbConstants.USB_CLASS_APP_SPEC:
                return String.format("Application Specific 0x%02x", classType);
            case UsbConstants.USB_CLASS_AUDIO:
                return "Audio";
            case UsbConstants.USB_CLASS_CDC_DATA:
                return "CDC Control";
            case UsbConstants.USB_CLASS_COMM:
                return "Communications";
            case UsbConstants.USB_CLASS_CONTENT_SEC:
                return "Content Security";
            case UsbConstants.USB_CLASS_CSCID:
                return "Content Smart Card";
            case UsbConstants.USB_CLASS_HID:
                return "Human Interface Device";
            case UsbConstants.USB_CLASS_HUB:
                return "Hub";
            case UsbConstants.USB_CLASS_MASS_STORAGE:
                return "Mass Storage";
            case UsbConstants.USB_CLASS_MISC:
                return "Wireless Miscellaneous";
            case UsbConstants.USB_CLASS_PER_INTERFACE:
                return "(Defined Per Interface)";
            case UsbConstants.USB_CLASS_PHYSICA:
                return "Physical";
            case UsbConstants.USB_CLASS_PRINTER:
                return "Printer";
            case UsbConstants.USB_CLASS_STILL_IMAGE:
                return "Still Image";
            case UsbConstants.USB_CLASS_VENDOR_SPEC:
                return String.format("Vendor Specific 0x%02x", classType);
            case UsbConstants.USB_CLASS_VIDEO:
                return "Video";
            case UsbConstants.USB_CLASS_WIRELESS_CONTROLLER:
                return "Wireless Controller";
            default:
                return String.format("0x%02x", classType);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static String getName(UsbDevice device){
        return Utils.hasLollipop() ? device.getProductName() : device.getDeviceName();
    }

    public static String getPath(UsbDevice device){
        return device.getDeviceName();
    }
}