package dev.dworks.apps.anexplorer.transfer.model;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Perform a transfer from one device to another
 *
 * This class takes care of communicating (via socket) with another device to
 * transfer a bundle (list of items) using packets.
 */
public class Transfer implements Runnable {

    private static final int CHUNK_SIZE = 65536;
    private static final Gson mGson = new Gson();

    /**
     * Listener for status changes
     */
    public interface StatusChangedListener {
        void onStatusChanged(TransferStatus transferStatus);
    }

    /**
     * Listener for item received events
     */
    public interface ItemReceivedListener {
        void onItemReceived(Item item);
    }

    /**
     * Transfer header
     */
    private class TransferHeader {
        String name;
        String count;
        String size;
    }

    // Internal state of the transfer
    private enum InternalState {
        TransferHeader,
        ItemHeader,
        ItemContent,
        Finished,
    }

    private final TransferStatus mTransferStatus;
    private volatile boolean mStop = false;

    private final List<StatusChangedListener> mStatusChangedListeners = new ArrayList<>();
    private final List<ItemReceivedListener> mItemReceivedListeners = new ArrayList<>();

    private Device mDevice;
    private Bundle mBundle;
    private String mDeviceName;
    private String mTransferDirectory;
    private boolean mOverwrite;

    private SocketChannel mSocketChannel;
    private Selector mSelector = Selector.open();

    private InternalState mInternalState = InternalState.TransferHeader;

    private Packet mReceivingPacket;
    private Packet mSendingPacket;

    private int mTransferItems;
    private long mTransferBytesTotal;
    private long mTransferBytesTransferred;

    private Item mItem;
    private int mItemIndex;
    private long mItemBytesRemaining;

    /**
     * Create a transfer for receiving items
     * @param socketChannel incoming channel
     * @param transferDirectory directory for incoming files
     * @param overwrite true to overwrite existing files
     * @param unknownDeviceName device name shown before being received
     */
    public Transfer(SocketChannel socketChannel, String transferDirectory, boolean overwrite, String unknownDeviceName) throws IOException {
        mTransferStatus = new TransferStatus(unknownDeviceName,
                TransferStatus.Direction.Receive, TransferStatus.State.Transferring);
        mTransferDirectory = transferDirectory;
        mOverwrite = overwrite;
        mSocketChannel = socketChannel;
        mSocketChannel.configureBlocking(false);
    }

    /**
     * Create a transfer for sending items
     * @param device device to connect to
     * @param deviceName device name to send to the remote device
     * @param bundle bundle to transfer
     */
    public Transfer(Device device, String deviceName, Bundle bundle) throws IOException {
        mTransferStatus = new TransferStatus(device.getName(),
                TransferStatus.Direction.Send, TransferStatus.State.Connecting);
        mDevice = device;
        mBundle = bundle;
        mDeviceName = deviceName;
        mSocketChannel = SocketChannel.open();
        mSocketChannel.configureBlocking(false);
        mTransferItems = bundle.size();
        mTransferBytesTotal = bundle.getTotalSize();
        mTransferStatus.setBytesTotal(mTransferBytesTotal);
    }

    /**
     * Set the transfer ID
     */
    public void setId(int id) {
        synchronized (mTransferStatus) {
            mTransferStatus.setId(id);
        }
    }

    /**
     * Retrieve the current transfer status
     * @return copy of the current status
     */
    public TransferStatus getStatus() {
        synchronized (mTransferStatus) {
            return new TransferStatus(mTransferStatus);
        }
    }

    /**
     * Close the socket and wake the selector, effectively aborting the transfer
     */
    public void stop() {
        mStop = true;
        mSelector.wakeup();
    }

    /**
     * Add a listener for status changes
     *
     * This method should not be invoked after starting the transfer.
     */
    public void addStatusChangedListener(StatusChangedListener statusChangedListener) {
        mStatusChangedListeners.add(statusChangedListener);
    }

    /**
     * Add a listener for items being recieved
     *
     * This method should not be invoked after starting the transfer.
     */
    public void addItemReceivedListener(ItemReceivedListener itemReceivedListener) {
        mItemReceivedListeners.add(itemReceivedListener);
    }

    /**
     * Notify all listeners that the status has changed
     */
    private void notifyStatusChangedListeners() {
        for (StatusChangedListener statusChangedListener : mStatusChangedListeners) {
            statusChangedListener.onStatusChanged(new TransferStatus(mTransferStatus));
        }
    }

    /**
     * Update current transfer progress
     */
    private void updateProgress() {
        int newProgress = (int) (100.0 * (mTransferBytesTotal != 0 ?
                (double) mTransferBytesTransferred / (double) mTransferBytesTotal : 0.0));
        if (newProgress != mTransferStatus.getProgress()) {
            synchronized (mTransferStatus) {
                mTransferStatus.setProgress(newProgress);
                mTransferStatus.setBytesTransferred(mTransferBytesTransferred);
                notifyStatusChangedListeners();
            }
        }
    }

    /**
     * Process the transfer header
     */
    private void processTransferHeader() throws IOException {
        TransferHeader transferHeader;
        try {
            transferHeader = mGson.fromJson(new String(
                    mReceivingPacket.getBuffer().array(), Charset.forName("UTF-8")),
                    TransferHeader.class);
            mTransferItems = Integer.parseInt(transferHeader.count);
            mTransferBytesTotal = Long.parseLong(transferHeader.size);
        } catch (JsonSyntaxException | NumberFormatException e) {
            throw new IOException(e.getMessage());
        }
        mInternalState = mItemIndex == mTransferItems ? InternalState.Finished : InternalState.ItemHeader;
        synchronized (mTransferStatus) {
            mTransferStatus.setRemoteDeviceName(transferHeader.name);
            mTransferStatus.setBytesTotal(mTransferBytesTotal);
            notifyStatusChangedListeners();
        }
    }

    /**
     * Process the header for an individual item
     */
    private void processItemHeader() throws IOException {
        Type type = new TypeToken<Map<String, Object>>(){}.getType();
        Map<String, Object> map;
        try {
            map = mGson.fromJson(new String(
                    mReceivingPacket.getBuffer().array(), Charset.forName("UTF-8")), type);
        } catch (JsonSyntaxException e) {
            throw new IOException(e.getMessage());
        }
        String itemType = (String) map.get(Item.TYPE);
        if (itemType == null) {
            itemType = FileItem.TYPE_NAME;
        }
        switch (itemType) {
            case FileItem.TYPE_NAME:
                mItem = new FileItem(mTransferDirectory, map, mOverwrite);
                break;
            case UrlItem.TYPE_NAME:
                mItem = new UrlItem(map);
                break;
            default:
                throw new IOException("unrecognized item type");
        }
        long itemSize = mItem.getLongProperty(Item.SIZE, true);
        if (itemSize != 0) {
            mInternalState = InternalState.ItemContent;
            mItem.open(Item.Mode.Write);
            mItemBytesRemaining = itemSize;
        } else {
            processNext();
        }
    }

    /**
     * Process item contents
     */
    private void processItemContent() throws IOException {
        mItem.write(mReceivingPacket.getBuffer().array());
        int numBytes = mReceivingPacket.getBuffer().capacity();
        mTransferBytesTransferred += numBytes;
        mItemBytesRemaining -= numBytes;
        updateProgress();
        if (mItemBytesRemaining <= 0) {
            mItem.close();
            processNext();
        }
    }

    /**
     * Prepare to process the next item
     */
    private void processNext() {
        mItemIndex += 1;
        mInternalState = mItemIndex == mTransferItems ? InternalState.Finished : InternalState.ItemHeader;
        for (ItemReceivedListener itemReceivedListener : mItemReceivedListeners) {
            itemReceivedListener.onItemReceived(mItem);
        }
    }

    /**
     * Process the next packet by reading it and then invoking the correct method
     * @return true if there are more packets expected
     */
    private boolean processNextPacket() throws IOException {
        if (mReceivingPacket == null) {
            mReceivingPacket = new Packet();
        }
        mReceivingPacket.read(mSocketChannel);
        if (mReceivingPacket.isFull()) {
            if (mReceivingPacket.getType() == Packet.ERROR) {
                throw new IOException(new String(mReceivingPacket.getBuffer().array(),
                        Charset.forName("UTF-8")));
            }
            if (mTransferStatus.getDirection() == TransferStatus.Direction.Receive) {
                if (mInternalState == InternalState.TransferHeader && mReceivingPacket.getType() == Packet.JSON) {
                    processTransferHeader();
                } else if (mInternalState == InternalState.ItemHeader && mReceivingPacket.getType() == Packet.JSON) {
                    processItemHeader();
                } else if (mInternalState == InternalState.ItemContent && mReceivingPacket.getType() == Packet.BINARY) {
                    processItemContent();
                } else {
                    throw new IOException("unexpected packet");
                }
                mReceivingPacket = null;
                return mInternalState != InternalState.Finished;
            } else {
                if (mInternalState == InternalState.Finished && mReceivingPacket.getType() == Packet.SUCCESS) {
                    return false;
                } else {
                    throw new IOException("unexpected packet");
                }
            }
        } else {
            return true;
        }
    }

    /**
     * Send the transfer header
     */
    private void sendTransferHeader() {
        Map<String, String> map = new HashMap<>();
        map.put("name", mDeviceName);
        map.put("count", Integer.toString(mBundle.size()));
        map.put("size", Long.toString(mBundle.getTotalSize()));
        mSendingPacket = new Packet(Packet.JSON, mGson.toJson(map).getBytes(
                Charset.forName("UTF-8")));
        mInternalState = mItemIndex == mTransferItems ? InternalState.Finished : InternalState.ItemHeader;
    }

    /**
     * Send the header for an individual item
     */
    private void sendItemHeader() throws IOException {
        mItem = mBundle.get(mItemIndex);
        mSendingPacket = new Packet(Packet.JSON, mGson.toJson(
                mItem.getProperties()).getBytes(Charset.forName("UTF-8")));
        long itemSize = mItem.getLongProperty(Item.SIZE, true);
        if (itemSize != 0) {
            mInternalState = InternalState.ItemContent;
            mItem.open(Item.Mode.Read);
            mItemBytesRemaining = itemSize;
        } else {
            mItemIndex += 1;
            mInternalState = mItemIndex == mTransferItems ? InternalState.Finished : InternalState.ItemHeader;
        }
    }

    /**
     * Send item contents
     */
    private void sendItemContent() throws IOException {
        byte buffer[] = new byte[CHUNK_SIZE];
        int numBytes = mItem.read(buffer);
        mSendingPacket = new Packet(Packet.BINARY, buffer, numBytes);
        mTransferBytesTransferred += numBytes;
        mItemBytesRemaining -= numBytes;
        updateProgress();
        if (mItemBytesRemaining <= 0) {
            mItem.close();
            mItemIndex += 1;
            mInternalState = mItemIndex == mTransferItems ? InternalState.Finished : InternalState.ItemHeader;
        }
    }

    /**
     * Send the next packet by evaluating the current state
     * @return true if there are more packets to send
     */
    private boolean sendNextPacket() throws IOException {
        if (mSendingPacket == null) {
            if (mTransferStatus.getDirection() == TransferStatus.Direction.Receive) {
                mSendingPacket = new Packet(Packet.SUCCESS);
            } else {
                switch (mInternalState) {
                    case TransferHeader:
                        sendTransferHeader();
                        break;
                    case ItemHeader:
                        sendItemHeader();
                        break;
                    case ItemContent:
                        sendItemContent();
                        break;
                    default:
                        throw new IOException("unreachable code");
                }
            }
        }
        mSocketChannel.write(mSendingPacket.getBuffer());
        if (mSendingPacket.isFull()) {
            mSendingPacket = null;
            return mInternalState != InternalState.Finished;
        }
        return true;
    }

    /**
     * Perform the transfer until it completes or an error occurs
     */
    @Override
    public void run() {
        try {
            // Indicate which operations select() should select for
            SelectionKey selectionKey = mSocketChannel.register(
                    mSelector,
                    mTransferStatus.getDirection() == TransferStatus.Direction.Receive ?
                            SelectionKey.OP_READ :
                            SelectionKey.OP_CONNECT
            );

            // For a sending transfer, connect to the remote device
            if (mTransferStatus.getDirection() == TransferStatus.Direction.Send) {
                mSocketChannel.connect(new InetSocketAddress(mDevice.getHost(), mDevice.getPort()));
            }

            while (true) {
                mSelector.select();
                if (mStop) {
                    break;
                }
                if (selectionKey.isConnectable()) {
                    mSocketChannel.finishConnect();
                    selectionKey.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);

                    synchronized (mTransferStatus) {
                        mTransferStatus.setState(TransferStatus.State.Transferring);
                        notifyStatusChangedListeners();
                    }
                }
                if (selectionKey.isReadable()) {
                    if (!processNextPacket()) {
                        if (mTransferStatus.getDirection() == TransferStatus.Direction.Receive) {
                            selectionKey.interestOps(SelectionKey.OP_WRITE);
                        } else {
                            break;
                        }
                    }
                }
                if (selectionKey.isWritable()) {
                    if (!sendNextPacket()) {
                        if (mTransferStatus.getDirection() == TransferStatus.Direction.Receive) {
                            break;
                        } else {
                            selectionKey.interestOps(SelectionKey.OP_READ);
                        }
                    }
                }
            }

            // Close the socket
            mSocketChannel.close();

            // If interrupted, throw an error
            if (mStop) {
                throw new IOException("transfer was cancelled");
            }

            // Indicate success
            synchronized (mTransferStatus) {
                mTransferStatus.setState(TransferStatus.State.Succeeded);
                notifyStatusChangedListeners();
            }

        } catch (IOException e) {
            synchronized (mTransferStatus) {
                mTransferStatus.setState(TransferStatus.State.Failed);
                mTransferStatus.setError(e.getMessage());
                notifyStatusChangedListeners();
            }
        }
    }
}
