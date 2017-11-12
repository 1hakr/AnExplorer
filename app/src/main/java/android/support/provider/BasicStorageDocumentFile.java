package android.support.provider;

import android.content.Context;
import android.net.Uri;

import dev.dworks.apps.anexplorer.misc.Utils;

/**
 * Created by HaKr on 25/01/17.
 */

public class BasicStorageDocumentFile extends DocumentFile {
    private Context mContext;
    private Uri mUri;

    BasicStorageDocumentFile(DocumentFile parent, Context context, Uri uri) {
        super(parent);
        mContext = context;
        mUri = uri;
    }

    @Override
    public DocumentFile createFile(String mimeType, String displayName) {
        final Uri result = DocumentsContractCompat.createFile(mContext, mUri, mimeType, displayName);
        return (result != null) ? new TreeDocumentFile(this, mContext, result) : null;
    }

    @Override
    public DocumentFile createDirectory(String displayName) {
        final Uri result = DocumentsContractCompat.createDirectory(mContext, mUri, displayName);
        return (result != null) ? new TreeDocumentFile(this, mContext, result) : null;
    }

    @Override
    public Uri getUri() {
        return mUri;
    }

    @Override
    public String getName() {
        return DocumentsContractCompat.getName(mContext, mUri);
    }

    @Override
    public String getType() {
        return DocumentsContractCompat.getType(mContext, mUri);
    }

    @Override
    public boolean isDirectory() {
        return DocumentsContractCompat.isDirectory(mContext, mUri);
    }

    @Override
    public boolean isFile() {
        return DocumentsContractCompat.isFile(mContext, mUri);
    }

    @Override
    public boolean isVirtual() {
        return DocumentsContractCompat.isVirtual(mContext, mUri);
    }

    @Override
    public long lastModified() {
        return DocumentsContractCompat.lastModified(mContext, mUri);
    }

    @Override
    public long length() {
        return DocumentsContractCompat.length(mContext, mUri);
    }

    @Override
    public boolean canRead() {
        return DocumentsContractCompat.canRead(mContext, mUri);
    }

    @Override
    public boolean canWrite() {
        return DocumentsContractCompat.canWrite(mContext, mUri);
    }

    @Override
    public boolean delete() {
        return DocumentsContractCompat.delete(mContext, mUri);
    }

    @Override
    public boolean exists() {
        return DocumentsContractCompat.exists(mContext, mUri);
    }

    @Override
    public DocumentFile[] listFiles() {
        final Uri[] result = DocumentsContractCompat.listFiles(mContext, mUri);
        final DocumentFile[] resultFiles = new DocumentFile[result.length];
        for (int i = 0; i < result.length; i++) {
            resultFiles[i] = new TreeDocumentFile(this, mContext, result[i]);
        }
        return resultFiles;
    }

    @Override
    public boolean renameTo(String displayName) {
        final Uri result = DocumentsContractCompat.renameTo(mContext, mUri, displayName);
        if (result != null) {
            mUri = result;
            return true;
        } else {
            return false;
        }
    }

    public static DocumentFile fromUri(Context context, Uri treeUri) {
        if (Utils.hasLollipop()) {
            return new BasicDocumentFile(null, context, treeUri);
        } else {
            return new BasicStorageDocumentFile(null, context, treeUri);
        }
    }
}
