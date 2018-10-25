package dev.dworks.apps.anexplorer.adapter;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ProtocolException;

import dev.dworks.apps.anexplorer.model.DocumentInfo;
import dev.dworks.apps.anexplorer.model.Durable;
import dev.dworks.apps.anexplorer.model.DurableUtils;
import dev.dworks.apps.anexplorer.model.RootInfo;

import static dev.dworks.apps.anexplorer.adapter.HomeAdapter.TYPE_RECENT;

public class CommonInfo implements Durable, Parcelable {
    private static final int VERSION_INIT = 1;
    private static final int VERSION_DROP_TYPE = 2;

    public int type;
    public DocumentInfo documentInfo;
    public RootInfo rootInfo;

    public CommonInfo() {
        reset();
    }

    @Override
    public void reset() {
        documentInfo = null;
        rootInfo = null;

    }

    public static CommonInfo from(RootInfo rootInfo, int type){
        CommonInfo commonInfo = new CommonInfo();
        commonInfo.type = type;
        commonInfo.rootInfo = rootInfo;
        return commonInfo;
    }

    public static CommonInfo from(DocumentInfo documentInfo, int type){
        CommonInfo commonInfo = new CommonInfo();
        commonInfo.type = type;
        commonInfo.documentInfo = documentInfo;
        return commonInfo;
    }

    public static CommonInfo from(Cursor cursor){
        DocumentInfo documentInfo = DocumentInfo.fromDirectoryCursor(cursor);
        CommonInfo commonInfo = from(documentInfo, TYPE_RECENT);
        return commonInfo;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(flags);
        DurableUtils.writeToParcel(dest, rootInfo);
        DurableUtils.writeToParcel(dest, documentInfo);
    }

    @Override
    public void read(DataInputStream in) throws IOException {
        final int version = in.readInt();
        switch (version) {
            case VERSION_DROP_TYPE:
                final RootInfo root = new RootInfo();
                final DocumentInfo documentInfo = new DocumentInfo();
                type = in.readInt();
                root.read(in);
                documentInfo.read(in);
                documentInfo.deriveFields();
                break;
            default:
                throw new ProtocolException("Unknown version " + version);
        }
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeInt(VERSION_DROP_TYPE);
        out.writeInt(type);
        rootInfo.write(out);
        documentInfo.write(out);
    }

    public static final Creator<CommonInfo> CREATOR = new Creator<CommonInfo>() {
        @Override
        public CommonInfo createFromParcel(Parcel in) {
            final CommonInfo commonInfo = new CommonInfo();
            DurableUtils.readFromParcel(in, commonInfo);
            return commonInfo;
        }

        @Override
        public CommonInfo[] newArray(int size) {
            return new CommonInfo[size];
        }
    };
}
