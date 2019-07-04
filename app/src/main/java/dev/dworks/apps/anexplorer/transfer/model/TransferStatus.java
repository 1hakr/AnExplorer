package dev.dworks.apps.anexplorer.transfer.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Status information about a transfer
 */
public class TransferStatus implements Parcelable {

    /**
     * Direction of transfer relative to the current device
     */
    public enum Direction {
        Receive,
        Send,
    }

    /**
     * State of the transfer
     */
    public enum State {
        Connecting,
        Transferring,
        Failed,
        Succeeded,
    }

    private int mId;
    private Direction mDirection;
    private String mRemoteDeviceName;
    private State mState;
    private int mProgress;
    private long mBytesTransferred = 0;
    private long mBytesTotal = 0;
    private String mError;

    /**
     * Initialize transfer status from a transfer
     */
    public TransferStatus(String deviceName, Direction direction, State state) {
        mDirection = direction;
        mRemoteDeviceName = deviceName;
        mState = state;
    }

    /**
     * Initialize transfer status from another instance
     */
    public TransferStatus(TransferStatus status) {
        this.mId = status.mId;
        this.mDirection = status.mDirection;
        this.mRemoteDeviceName = status.mRemoteDeviceName;
        this.mState = status.mState;
        this.mProgress = status.mProgress;
        this.mBytesTransferred = status.mBytesTransferred;
        this.mBytesTotal = status.mBytesTotal;
        this.mError = status.mError;
    }

    /**
     * Initialize transfer status from a parcel
     */
    private TransferStatus(Parcel in) {
        this.mId = in.readInt();
        this.mDirection = Direction.valueOf(in.readString());
        this.mRemoteDeviceName = in.readString();
        this.mState = State.valueOf(in.readString());
        this.mProgress = in.readInt();
        this.mBytesTransferred = in.readLong();
        this.mBytesTotal = in.readLong();
        this.mError = in.readString();
    }

    public static final Creator<TransferStatus> CREATOR = new Creator<TransferStatus>() {
        @Override
        public TransferStatus createFromParcel(Parcel in) {
            return new TransferStatus(in);
        }

        @Override
        public TransferStatus[] newArray(int size) {
            return new TransferStatus[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(mId);
        out.writeString(mDirection.name());
        out.writeString(mRemoteDeviceName);
        out.writeString(mState.name());
        out.writeInt(mProgress);
        out.writeLong(mBytesTransferred);
        out.writeLong(mBytesTotal);
        out.writeString(mError);
    }

    public int getId() {
        return mId;
    }

    public void setId(int id) {
        mId = id;
    }

    public Direction getDirection() {
        return mDirection;
    }

    public void setDirection(Direction direction) {
        mDirection = direction;
    }

    public String getRemoteDeviceName() {
        return mRemoteDeviceName;
    }

    public void setRemoteDeviceName(String remoteDeviceName) {
        mRemoteDeviceName = remoteDeviceName;
    }

    public State getState() {
        return mState;
    }

    public void setState(State state) {
        mState = state;
    }

    public int getProgress() {
        return mProgress;
    }

    public void setProgress(int progress) {
        mProgress = progress;
    }

    public long getBytesTransferred() {
        return mBytesTransferred;
    }

    public void setBytesTransferred(long bytesTransferred) {
        mBytesTransferred = bytesTransferred;
    }

    public long getBytesTotal() {
        return mBytesTotal;
    }

    public void setBytesTotal(long bytesTotal) {
        mBytesTotal = bytesTotal;
    }

    public String getError() {
        return mError;
    }

    public void setError(String error) {
        mError = error;
    }

    public boolean isFinished() {
        return mState == State.Succeeded || mState == State.Failed;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof TransferStatus) {
            return mId == ((TransferStatus) o).getId();
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return mId;
    }
}
