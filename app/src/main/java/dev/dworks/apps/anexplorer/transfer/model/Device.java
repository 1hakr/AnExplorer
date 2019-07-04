package dev.dworks.apps.anexplorer.transfer.model;

import android.net.nsd.NsdServiceInfo;

import java.io.Serializable;
import java.net.InetAddress;

import dev.dworks.apps.anexplorer.misc.Utils;

import static dev.dworks.apps.anexplorer.transfer.TransferHelper.SERVICE_TYPE;
import static dev.dworks.apps.anexplorer.transfer.TransferHelper.UUID;

/**
 * Device discoverable through mDNS
 *
 * The device UUID is also used as the service name
 */
public class Device implements Serializable {

    private String mName;
    private String mUuid;
    private InetAddress mHost;
    private int mPort;

    /**
     * Create a device from the provided information
     * @param name device name
     * @param uuid unique identifier for the device
     * @param host device host
     * @param port port for the service
     */
    public Device(String name, String uuid, InetAddress host, int port) {
        mName = name;
        mUuid = uuid;
        mHost = host;
        mPort = port;
    }

    public String getName() {
        return mName;
    }

    public InetAddress getHost() {
        return mHost;
    }

    public int getPort() {
        return mPort;
    }

    public NsdServiceInfo toServiceInfo() {
        NsdServiceInfo serviceInfo = new NsdServiceInfo();
        serviceInfo.setServiceType(SERVICE_TYPE);
        serviceInfo.setServiceName(mName);
        if (Utils.hasLollipop()){
           //serviceInfo.setAttribute(UUID, mUuid);
        }
        serviceInfo.setPort(mPort);
        return serviceInfo;
    }
}
