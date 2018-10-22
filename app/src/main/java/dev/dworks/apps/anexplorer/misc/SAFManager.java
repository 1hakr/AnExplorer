package dev.dworks.apps.anexplorer.misc;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.UriPermission;
import android.net.Uri;
import android.os.Build;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.support.provider.BasicDocumentFile;
import android.support.provider.DocumentFile;
import android.support.provider.UsbDocumentFile;
import android.text.Spanned;

import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import androidx.collection.ArrayMap;
import dev.dworks.apps.anexplorer.common.DialogBuilder;
import dev.dworks.apps.anexplorer.model.DocumentInfo;
import dev.dworks.apps.anexplorer.model.DocumentsContract;
import dev.dworks.apps.anexplorer.model.RootInfo;
import dev.dworks.apps.anexplorer.provider.ExternalStorageProvider;

import static android.app.Activity.RESULT_OK;
import static dev.dworks.apps.anexplorer.misc.IntentUtils.ACTION_OPEN_DOCUMENT_TREE;
import static dev.dworks.apps.anexplorer.provider.ExternalStorageProvider.ROOT_ID_SECONDARY;
import static dev.dworks.apps.anexplorer.provider.UsbStorageProvider.ROOT_ID_USB;


public class SAFManager {

    private static final String TAG = "SAFManager";
    public static final String DOCUMENT_AUTHORITY = "com.android.externalstorage.documents";
    public static final int ADD_STORAGE_REQUEST_CODE = 4010;
    public static ArrayMap<String, Uri> secondaryRoots = new ArrayMap<>();
    private final Context mContext;

    public SAFManager(Context context) {
        mContext = context;
    }

    public DocumentFile getDocumentFile(String docId, File file)
            throws FileNotFoundException {

        DocumentFile documentFile = null;
        if(null != file && file.canWrite()){
            documentFile = DocumentFile.fromFile(file);
            return documentFile;
        }
        if(docId.startsWith(ROOT_ID_SECONDARY) && Utils.hasLollipop()){
            String newDocId = docId.substring(ROOT_ID_SECONDARY.length());
            Uri uri = getRootUri(newDocId);
            if(null == uri){
                if(null != file) {
                    documentFile = DocumentFile.fromFile(file);
                }
                return documentFile;
            }
            Uri fileUri = buildDocumentUriMaybeUsingTree(uri, newDocId);
            documentFile = BasicDocumentFile.fromUri(mContext, fileUri);
        } else if(docId.startsWith(ROOT_ID_USB)){
            documentFile = UsbDocumentFile.fromUri(mContext, docId);
        } else {
            if(null != file){
                documentFile = DocumentFile.fromFile(file);
            } else {
                documentFile = BasicDocumentFile.fromUri(mContext,
                        DocumentsContract.buildDocumentUri(ExternalStorageProvider.AUTHORITY, docId));
            }
        }

        return documentFile;
    }

    public DocumentFile getDocumentFile(Uri uri)
            throws FileNotFoundException {
        String docId = getRootUri(uri);
        return getDocumentFile(docId, null);
    }

    public static String getRootUri(Uri uri) {
        if (isTreeUri(uri)) {
            return DocumentsContract.getTreeDocumentId(uri);
        }
        return DocumentsContract.getDocumentId(uri);
    }

    private static Uri buildDocumentUriMaybeUsingTree(Uri uri, String docId) {
        return DocumentsContract.buildDocumentUriMaybeUsingTree(uri, docId);
    }

    private static boolean isTreeUri(Uri uri) {
        return DocumentsContract.isTreeUri(uri);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private Uri getRootUri(String docId){
        Uri treeUri;
        final int splitIndex = docId.indexOf(':', 1);
        final String tag = docId.substring(0, splitIndex);

        //check in cache
        treeUri = secondaryRoots.get(tag);
        if(null != treeUri){
            return treeUri;
        }

        //get root dynamically
        List<UriPermission> permissions = mContext.getContentResolver().getPersistedUriPermissions();
        for (UriPermission permission :
                permissions) {
            String treeRootId = getRootUri(permission.getUri());
            if(docId.startsWith(treeRootId)){
                treeUri = permission.getUri();
                secondaryRoots.put(tag, treeUri);
                return treeUri;
            }
        }
        return treeUri;
    }

    @SuppressLint("NewApi")
    public static void takeCardUriPermission(final Activity activity, RootInfo root, DocumentInfo doc) {
        boolean useStorageAccess = Utils.hasNougat() && !Utils.hasOreo();
        if(useStorageAccess && null != doc.path){
            StorageManager storageManager = (StorageManager) activity.getSystemService(Context.STORAGE_SERVICE);
            StorageVolume storageVolume = storageManager.getStorageVolume(new File(doc.path));
            Intent intent = storageVolume.createAccessIntent(null);
            try {
                activity.startActivityForResult(intent, ADD_STORAGE_REQUEST_CODE);
            } catch (ActivityNotFoundException e){
                CrashReportingManager.logException(e, true);
            }
        } else if(Utils.hasLollipop()){
            DialogBuilder builder = new DialogBuilder(activity);
            Spanned message = Utils.fromHtml("Select root (outermost) folder of storage "
                    + "<b>" + root.title + "</b>"
                    + " to grant access from next screen");
            builder.setTitle("Grant accesss to External Storage")
                    .setMessage(message.toString())
                    .setPositiveButton("Give Access", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterfaceParam, int code) {
                            Intent intent = new Intent(ACTION_OPEN_DOCUMENT_TREE);
                            intent.setPackage("com.android.documentsui");
                            try {
                                activity.startActivityForResult(intent, ADD_STORAGE_REQUEST_CODE);
                            } catch (ActivityNotFoundException e){
                                CrashReportingManager.logException(e, true);
                            }
                        }
                    })
                    .setNegativeButton("Cancel", null);
            builder.showDialog();
        }
    }

    public static boolean onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        boolean accessGranted = false;
        boolean primaryStorage = false;
        if (requestCode == ADD_STORAGE_REQUEST_CODE ){
            if(resultCode == RESULT_OK) {
                if (data != null && data.getData() != null) {
                    Uri uri = data.getData();
                    if (Utils.hasKitKat()) {
                        activity.getContentResolver().takePersistableUriPermission(uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        String rootId = getRootUri(uri);
                        if(!rootId.startsWith(ExternalStorageProvider.ROOT_ID_PRIMARY_EMULATED)){
                            String secondaryRootId = ExternalStorageProvider.ROOT_ID_SECONDARY + rootId;
                            ExternalStorageProvider.notifyDocumentsChanged(activity, rootId);
                            accessGranted = true;
                        } else {
                            primaryStorage = true;
                        }
                    }
                }
            }
        }
        Utils.showSnackBar(activity, "Access"+ (accessGranted ? "" : " was not") +" granted"
                        + (primaryStorage ? ". Choose the external storage." : ""),
                Snackbar.LENGTH_SHORT, accessGranted ? "" : "ERROR", null);
        return accessGranted;
    }
}