/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.dworks.apps.anexplorer.model;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.core.content.ContextCompat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.ProtocolException;

import dev.dworks.apps.anexplorer.DocumentsActivity;
import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.libcore.util.Objects;
import dev.dworks.apps.anexplorer.misc.AnalyticsManager;
import dev.dworks.apps.anexplorer.misc.IconUtils;
import dev.dworks.apps.anexplorer.model.DocumentsContract.Root;
import dev.dworks.apps.anexplorer.provider.AppsProvider;
import dev.dworks.apps.anexplorer.provider.CloudStorageProvider;
import dev.dworks.apps.anexplorer.provider.DownloadStorageProvider;
import dev.dworks.apps.anexplorer.provider.ExternalStorageProvider;
import dev.dworks.apps.anexplorer.provider.ExtraDocumentsProvider;
import dev.dworks.apps.anexplorer.provider.MediaDocumentsProvider;
import dev.dworks.apps.anexplorer.provider.NetworkStorageProvider;
import dev.dworks.apps.anexplorer.provider.NonMediaDocumentsProvider;
import dev.dworks.apps.anexplorer.provider.RecentsProvider;
import dev.dworks.apps.anexplorer.provider.RootedStorageProvider;
import dev.dworks.apps.anexplorer.provider.UsbStorageProvider;
import dev.dworks.apps.anexplorer.transfer.TransferHelper;

import static dev.dworks.apps.anexplorer.model.DocumentInfo.getCursorInt;
import static dev.dworks.apps.anexplorer.model.DocumentInfo.getCursorLong;
import static dev.dworks.apps.anexplorer.model.DocumentInfo.getCursorString;

/**
 * Representation of a {@link Root}.
 */
@SuppressLint("DefaultLocale")
public class RootInfo implements Durable, Parcelable {
    @SuppressWarnings("unused")
	private static final int VERSION_INIT = 1;
    private static final int VERSION_DROP_TYPE = 2;

    public String authority;
    public String rootId;
    public int flags;
    public int icon;
    public String title;
    public String summary;
    public String documentId;
    public long availableBytes;
    public long totalBytes;
    public String mimeTypes;
    public String path;
    public File visiblePath;

    /** Derived fields that aren't persisted */
    public String derivedPackageName;
    public String[] derivedMimeTypes;
    public int derivedIcon;
    public int derivedColor;
    public String derivedTag;

    public RootInfo() {
        reset();
    }

    @Override
    public void reset() {
        authority = null;
        rootId = null;
        flags = 0;
        icon = 0;
        title = null;
        summary = null;
        documentId = null;
        availableBytes = -1;
        totalBytes = -1;
        mimeTypes = null;
        path = null;

        derivedPackageName = null;
        derivedMimeTypes = null;
        derivedIcon = 0;
        derivedColor = 0;
    }

    @Override
    public void read(DataInputStream in) throws IOException {
        final int version = in.readInt();
        switch (version) {
            case VERSION_DROP_TYPE:
                authority = DurableUtils.readNullableString(in);
                rootId = DurableUtils.readNullableString(in);
                flags = in.readInt();
                icon = in.readInt();
                title = DurableUtils.readNullableString(in);
                summary = DurableUtils.readNullableString(in);
                documentId = DurableUtils.readNullableString(in);
                availableBytes = in.readLong();
                totalBytes = in.readLong();
                mimeTypes = DurableUtils.readNullableString(in);
                path = DurableUtils.readNullableString(in);
                deriveFields();
                break;
            default:
                throw new ProtocolException("Unknown version " + version);
        }
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeInt(VERSION_DROP_TYPE);
        DurableUtils.writeNullableString(out, authority);
        DurableUtils.writeNullableString(out, rootId);
        out.writeInt(flags);
        out.writeInt(icon);
        DurableUtils.writeNullableString(out, title);
        DurableUtils.writeNullableString(out, summary);
        DurableUtils.writeNullableString(out, documentId);
        out.writeLong(availableBytes);
        out.writeLong(totalBytes);
        DurableUtils.writeNullableString(out, mimeTypes);
        DurableUtils.writeNullableString(out, path);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        DurableUtils.writeToParcel(dest, this);
    }

    public static final Creator<RootInfo> CREATOR = new Creator<RootInfo>() {
        @Override
        public RootInfo createFromParcel(Parcel in) {
            final RootInfo root = new RootInfo();
            DurableUtils.readFromParcel(in, root);
            return root;
        }

        @Override
        public RootInfo[] newArray(int size) {
            return new RootInfo[size];
        }
    };

    public static RootInfo fromRootsCursor(String authority, Cursor cursor) {
        final RootInfo root = new RootInfo();
        root.authority = authority;
        root.rootId = getCursorString(cursor, Root.COLUMN_ROOT_ID);
        root.flags = getCursorInt(cursor, Root.COLUMN_FLAGS);
        root.icon = getCursorInt(cursor, Root.COLUMN_ICON);
        root.title = getCursorString(cursor, Root.COLUMN_TITLE);
        root.summary = getCursorString(cursor, Root.COLUMN_SUMMARY);
        root.documentId = getCursorString(cursor, Root.COLUMN_DOCUMENT_ID);
        root.availableBytes = getCursorLong(cursor, Root.COLUMN_AVAILABLE_BYTES);
        root.totalBytes = getCursorLong(cursor, Root.COLUMN_CAPACITY_BYTES);
        root.mimeTypes = getCursorString(cursor, Root.COLUMN_MIME_TYPES);
        root.path = getCursorString(cursor, Root.COLUMN_PATH);
        root.deriveFields();
        return root;
    }

    public void deriveFields() {
        derivedMimeTypes = (mimeTypes != null) ? mimeTypes.split("\n") : null;
        derivedColor = R.color.primaryColor;
        derivedTag = title;

        // TODO: remove these special case icons
        if (isInternalStorage()) {
            derivedIcon = R.drawable.ic_root_internal;
            derivedTag = "storage";
        } else if (isExternalStorage()) {
            derivedIcon = R.drawable.ic_root_sdcard;
            derivedTag = "external_storage";
        } else if (isRootedStorage()) {
            derivedIcon = R.drawable.ic_root_root;
            derivedTag = "root";
        } else if (isPhoneStorage()) {
            derivedIcon = R.drawable.ic_root_device;
            derivedTag = "phone";
        } else if (isSecondaryStorage()) {
            derivedIcon = R.drawable.ic_root_sdcard;
            if (isUsb() || isSecondaryStorageUSB()) {
	            derivedIcon = R.drawable.ic_root_usb;
	        } else if (isSecondaryStorageHDD()) {
	            derivedIcon = R.drawable.ic_root_hdd;
	        }
            derivedTag = "secondary_storage";
        } else if (isUsbStorage()) {
            derivedIcon = R.drawable.ic_root_usb;
            derivedTag = "usb_storage";
        } else if (isDownloadsFolder()) {
            derivedIcon = R.drawable.ic_root_download;
            derivedTag = "downloads";
        } else if (isBluetoothFolder()) {
            derivedIcon = R.drawable.ic_root_bluetooth;
            derivedTag = "bluetooth";
        } else if (isAppBackupFolder()) {
            derivedIcon = R.drawable.ic_root_appbackup;
            derivedTag = "appbackup";
        } else if (isBookmarkFolder()) {
            derivedIcon = R.drawable.ic_root_bookmark;
            derivedTag = "bookmark";
        } else if (isHiddenFolder()) {
            derivedIcon = R.drawable.ic_root_hidden;
            derivedTag = "hidden";
        } else if (isDownloads()) {
            derivedIcon = R.drawable.ic_root_download;
            derivedTag = "downloads";
        } else if (isImages()) {
            derivedIcon = R.drawable.ic_root_image;
            derivedColor = R.color.item_doc_image;
            derivedTag = "images";
        } else if (isVideos()) {
            derivedIcon = R.drawable.ic_root_video;
            derivedColor = R.color.item_doc_video;
            derivedTag = "videos";
        } else if (isAudio()) {
            derivedIcon = R.drawable.ic_root_audio;
            derivedColor = R.color.item_doc_audio;
            derivedTag = "audio";
        } else if (isDocument()) {
            derivedIcon = R.drawable.ic_root_document;
            derivedColor = R.color.item_doc_pdf;
            derivedTag = "document";
        } else if (isArchive()) {
            derivedIcon = R.drawable.ic_root_archive;
            derivedColor = R.color.item_doc_compressed;
            derivedTag = "archive";
        } else if (isApk()) {
            derivedIcon = R.drawable.ic_root_apk;
            derivedColor = R.color.item_doc_apk;
            derivedTag = "apk";
        } else if (isUserApp()) {
            derivedIcon = R.drawable.ic_root_apps;
            derivedColor = R.color.item_doc_apps;
            derivedTag = "user_apps";
        } else if (isSystemApp()) {
            derivedIcon = R.drawable.ic_root_system_apps;
            derivedColor = R.color.item_doc_apps;
            derivedTag = "system_apps";
        } else if (isAppProcess()) {
            derivedIcon = R.drawable.ic_root_process;
            derivedTag = "process";
        } else if (isRecents()) {
            derivedIcon = R.drawable.ic_root_recent;
            derivedTag = "recent";
        } else if (isHome()) {
            derivedIcon = R.drawable.ic_root_home;
            derivedTag = "home";
        } else if (isConnections()) {
            derivedIcon = R.drawable.ic_root_connections;
            derivedTag = "connections";
        } else if (isServerStorage()) {
            derivedIcon = R.drawable.ic_root_server;
            derivedColor = R.color.item_connection_server;
            derivedTag = "server";
        } else if (isNetworkStorage()) {
            derivedIcon = R.drawable.ic_root_network;
            derivedColor = R.color.item_connection_client;
            derivedTag = "network";
        } else if (isCloudStorage()) {
            if (isCloudGDrive()) {
                derivedIcon = R.drawable.ic_root_gdrive;
            } else if (isCloudDropBox()) {
                derivedIcon = R.drawable.ic_root_dropbox;
            } else if (isCloudOneDrive()) {
                derivedIcon = R.drawable.ic_root_onedrive;
            } else if (isCloudBox()) {
                derivedIcon = R.drawable.ic_root_box;
            } else {
                derivedIcon = R.drawable.ic_root_cloud;
            }
            derivedColor = R.color.item_connection_cloud;
            derivedTag = "cloud";
        } else if (isExtraStorage()) {
            if (isWhatsApp()) {
                derivedIcon = R.drawable.ic_root_whatsapp;
                derivedColor = R.color.item_whatsapp;
                derivedTag = "whatsapp";
            } else if (isTelegram()) {
                derivedIcon = R.drawable.ic_root_telegram;
                derivedColor = R.color.item_telegram;
                derivedTag = "telegram";
            } else if (isTelegramX()) {
                derivedIcon = R.drawable.ic_root_telegram;
                derivedColor = R.color.item_telegramx;
                derivedTag = "telegramx";
            }
        } else if (isTransfer()) {
            derivedIcon = R.drawable.ic_root_transfer;
            derivedColor = R.color.item_transfer;
            derivedTag = "transfer";
        }  else if (isCast()) {
            derivedIcon = R.drawable.ic_root_cast;
            derivedColor = R.color.item_cast;
            derivedTag = "cast";
        }  else if (isReceiveFolder()) {
            derivedIcon = R.drawable.ic_stat_download;
            derivedTag = "receivefiles";
        }
    }

/*    public boolean isHome() {
        // Note that "home" is the expected root id for the auto-created
        // user home directory on external storage. The "home" value should
        // match ExternalStorageProvider.ROOT_ID_HOME.
        return isExternalStorage() && "home".equals(rootId);
    }*/

    public boolean isHome() {
        return authority == null && "home".equals(rootId);
    }

    public boolean isConnections() {
        return authority == null && "connections".equals(rootId);
    }

    public boolean isTransfer() {
        return TransferHelper.AUTHORITY.equals(authority)  && "transfer".equals(rootId);
    }

    public boolean isCast() {
        return authority == null && "cast".equals(rootId);
    }

    public boolean isRecents() {
        return RecentsProvider.AUTHORITY.equals(authority) && "recents".equals(rootId);
    }

    public boolean isStorage() {
        return isInternalStorage() || isExternalStorage() || isSecondaryStorage();
    }

    public boolean isRootedStorage() {
        return RootedStorageProvider.AUTHORITY.equals(authority);
    }

    public boolean isExternalStorage() {
        return ExternalStorageProvider.AUTHORITY.equals(authority)
                && ExternalStorageProvider.ROOT_ID_PRIMARY_EMULATED.equals(rootId);
    }

    public boolean isInternalStorage() {
        return ExternalStorageProvider.AUTHORITY.equals(authority)
                && title.toLowerCase().contains("internal");
    }

    public boolean isPhoneStorage() {
        return ExternalStorageProvider.AUTHORITY.equals(authority)
                && ExternalStorageProvider.ROOT_ID_DEVICE.equals(rootId);
    }
    
    public boolean isSecondaryStorage() {
        return ExternalStorageProvider.AUTHORITY.equals(authority)
        		&& rootId.startsWith(ExternalStorageProvider.ROOT_ID_SECONDARY);
    }

    public boolean isSecondaryStorageSD() {
        return contains(path, "sd", "card", "emmc") || contains(title, "sd", "card", "emmc");
    }
    
    public boolean isSecondaryStorageUSB() {
        return contains(path, "usb") || contains(title, "usb");
    }
    
    public boolean isSecondaryStorageHDD() {
        return contains(path, "hdd") || contains(title, "hdd");
    }

    public boolean isDownloadsFolder() {
        return ExternalStorageProvider.AUTHORITY.equals(authority)
                && ExternalStorageProvider.ROOT_ID_DOWNLOAD.equals(rootId);
    }

    public boolean isAppBackupFolder() {
        return ExternalStorageProvider.AUTHORITY.equals(authority)
                && ExternalStorageProvider.ROOT_ID_APP_BACKUP.equals(rootId);
    }

    public boolean isReceiveFolder() {
        return ExternalStorageProvider.AUTHORITY.equals(authority)
                && ExternalStorageProvider.ROOT_ID_RECIEVE_FLES.equals(rootId);
    }

    public boolean isBluetoothFolder() {
        return ExternalStorageProvider.AUTHORITY.equals(authority)
                && ExternalStorageProvider.ROOT_ID_BLUETOOTH.equals(rootId);
    }

    public boolean isBookmarkFolder() {
        return ExternalStorageProvider.AUTHORITY.equals(authority)
                && rootId.startsWith(ExternalStorageProvider.ROOT_ID_BOOKMARK);
    }

    public boolean isHiddenFolder() {
        return ExternalStorageProvider.AUTHORITY.equals(authority)
                && ExternalStorageProvider.ROOT_ID_HIDDEN.equals(rootId);
    }
    
    public boolean isDownloads() {
        return DownloadStorageProvider.AUTHORITY.equals(authority);
    }

    public boolean isImages() {
        return MediaDocumentsProvider.AUTHORITY.equals(authority)
                && MediaDocumentsProvider.TYPE_IMAGES_ROOT.equals(rootId);
    }

    public boolean isVideos() {
        return MediaDocumentsProvider.AUTHORITY.equals(authority)
                && MediaDocumentsProvider.TYPE_VIDEOS_ROOT.equals(rootId);
    }

    public boolean isAudio() {
        return MediaDocumentsProvider.AUTHORITY.equals(authority)
                && MediaDocumentsProvider.TYPE_AUDIO_ROOT.equals(rootId);
    }

    public boolean isDocument() {
        return NonMediaDocumentsProvider.AUTHORITY.equals(authority)
                && NonMediaDocumentsProvider.TYPE_DOCUMENT_ROOT.equals(rootId);
    }

    public boolean isExtraStorage() {
        return ExtraDocumentsProvider.AUTHORITY.equals(authority);
    }

    public boolean isWhatsApp() {
        return ExtraDocumentsProvider.AUTHORITY.equals(authority)
                && ExtraDocumentsProvider.ROOT_ID_WHATSAPP.equals(rootId);
    }

    public boolean isTelegram() {
        return ExtraDocumentsProvider.AUTHORITY.equals(authority)
                && ExtraDocumentsProvider.ROOT_ID_TELEGRAM.equals(rootId);
    }

    public boolean isTelegramX() {
        return ExtraDocumentsProvider.AUTHORITY.equals(authority)
                && ExtraDocumentsProvider.ROOT_ID_TELEGRAMX.equals(rootId);
    }

    public boolean isArchive() {
        return NonMediaDocumentsProvider.AUTHORITY.equals(authority)
                && NonMediaDocumentsProvider.TYPE_ARCHIVE_ROOT.equals(rootId);
    }

    public boolean isApk() {
        return NonMediaDocumentsProvider.AUTHORITY.equals(authority)
                && NonMediaDocumentsProvider.TYPE_APK_ROOT.equals(rootId);
    }

    public boolean isApp() {
        return AppsProvider.AUTHORITY.equals(authority);
    }
    
    public boolean isAppPackage() {
        return AppsProvider.AUTHORITY.equals(authority)
                && (AppsProvider.ROOT_ID_USER_APP.equals(rootId)
                || AppsProvider.ROOT_ID_SYSTEM_APP.equals(rootId));
    }

    public boolean isUserApp() {
        return AppsProvider.AUTHORITY.equals(authority)
                && AppsProvider.ROOT_ID_USER_APP.equals(rootId);
    }

    public boolean isSystemApp() {
        return AppsProvider.AUTHORITY.equals(authority)
                && AppsProvider.ROOT_ID_SYSTEM_APP.equals(rootId);
    }
    
    public boolean isAppProcess() {
        return AppsProvider.AUTHORITY.equals(authority)
                && AppsProvider.ROOT_ID_PROCESS.equals(rootId);
    }

    public boolean isNetworkStorage() {
        return NetworkStorageProvider.AUTHORITY.equals(authority);
    }

    public boolean isServerStorage() {
        return NetworkStorageProvider.AUTHORITY.equals(authority) && isServer();
    }

    public boolean isCloudStorage() {
        return CloudStorageProvider.AUTHORITY.equals(authority);
    }

    public boolean isCloudGDrive() {
        return CloudStorageProvider.AUTHORITY.equals(authority)
                && rootId.startsWith(CloudStorageProvider.TYPE_GDRIVE);
    }

    public boolean isCloudDropBox() {
        return CloudStorageProvider.AUTHORITY.equals(authority)
                && rootId.startsWith(CloudStorageProvider.TYPE_DROPBOX);
    }
    public boolean isCloudOneDrive() {
        return CloudStorageProvider.AUTHORITY.equals(authority)
                && rootId.startsWith(CloudStorageProvider.TYPE_ONEDRIVE);
    }
    public boolean isCloudBox() {
        return CloudStorageProvider.AUTHORITY.equals(authority)
                && rootId.startsWith(CloudStorageProvider.TYPE_BOX);
    }

    public boolean isUsbStorage() {
        return UsbStorageProvider.AUTHORITY.equals(authority);
    }
    
    public boolean isEditSupported() {
        return (flags & Root.FLAG_SUPPORTS_EDIT) != 0;
    }

    public Uri getUri() {
        return DocumentsContract.buildRootUri(authority, rootId);
    }

    public boolean isMtp() {
        return "com.android.mtp.documents".equals(authority);
    }

    public boolean hasSettings() {
        return (flags & Root.FLAG_HAS_SETTINGS) != 0;
    }
    public boolean supportsChildren() {
        return (flags & Root.FLAG_SUPPORTS_IS_CHILD) != 0;
    }
    public boolean supportsCreate() {
        return (flags & Root.FLAG_SUPPORTS_CREATE) != 0;
    }
    public boolean supportsRecents() {
        return (flags & Root.FLAG_SUPPORTS_RECENTS) != 0;
    }
    public boolean supportsSearch() {
        return (flags & Root.FLAG_SUPPORTS_SEARCH) != 0;
    }
    public boolean isAdvanced() {
        return (flags & Root.FLAG_ADVANCED) != 0;
    }
    public boolean isLocalOnly() {
        return (flags & Root.FLAG_LOCAL_ONLY) != 0;
    }

    public boolean isEmpty() {
        return (flags & Root.FLAG_EMPTY) != 0;
    }

    public boolean isSd() {
        return (flags & Root.FLAG_REMOVABLE_SD) != 0;
    }

    public boolean isUsb() {
        return (flags & Root.FLAG_REMOVABLE_USB) != 0;
    }

    public boolean isServer() {
        return (flags & Root.FLAG_CONNECTION_SERVER) != 0;
    }

    public Drawable loadIcon(Context context) {
        if (derivedIcon != 0) {
            return IconUtils.applyTintAttr(context, derivedIcon,
                    android.R.attr.textColorPrimary);
        } else {
            return IconUtils.loadPackageIcon(context, authority, icon);
        }
    }

    public Drawable loadDrawerIcon(Context context) {
        if (derivedIcon != 0) {
            return IconUtils.applyTintAttr(context, derivedIcon,
                    android.R.attr.textColorPrimary);
        } else {
            return IconUtils.loadPackageIcon(context, authority, icon);
        }
    }

    public Drawable loadNavDrawerIcon(Context context) {
        if (derivedIcon != 0) {
            return IconUtils.applyTint(context, derivedIcon,
                    ContextCompat.getColor(context, android.R.color.white));
        } else {
            return IconUtils.loadPackageIcon(context, authority, icon);
        }
    }

    public Drawable loadGridIcon(Context context) {
        if (derivedIcon != 0) {
            return IconUtils.applyTintAttr(context, derivedIcon,
                    android.R.attr.textColorPrimaryInverse);
        } else {
            return IconUtils.loadPackageIcon(context, authority, icon);
        }
    }

    public Drawable loadToolbarIcon(Context context) {
        if (derivedIcon != 0) {
            return IconUtils.applyTintAttr(context, derivedIcon, R.attr.colorControlNormal);
        } else {
            return IconUtils.loadPackageIcon(context, authority, icon);
        }
    }


    public Drawable loadShortcutIcon(Context context) {
        if (derivedIcon != 0) {
            return IconUtils.applyTint(context, derivedIcon,
                    ContextCompat.getColor(context, android.R.color.white));
        } else {
            return IconUtils.loadPackageIcon(context, authority, icon);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof RootInfo) {
            final RootInfo root = (RootInfo) o;
            return Objects.equals(authority, root.authority) && Objects.equals(rootId, root.rootId);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(authority, rootId);
    }

    public String getDirectoryString() {
        return !TextUtils.isEmpty(summary) ? summary : title;
    }

    /**
     * Checks if the path contains any of the given tags or not
     *
     * @param path The Folder path
     * @param tags the list of tags to check against
     * @return true if path has atleast one tag matched else false
     */
    public boolean contains(String path, String... tags) {
        if(!TextUtils.isEmpty(path)){
            for (String tag : tags){
                if(path.toLowerCase().contains(tag)){
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "Root{"
                + "authority=" + authority
                + ", rootId=" + rootId
                + ", documentId=" + documentId
                + ", path=" + path
                + ", title=" + title
                + ", isUsb=" + isUsb()
                + ", isSd=" + isSd()
                + ", isMtp=" + isMtp()
                + "}";
    }

    public static void openRoot(DocumentsActivity activity, RootInfo rootInfo, RootInfo parentRootInfo){
        activity.onRootPicked(rootInfo, parentRootInfo);
        AnalyticsManager.logEvent("open_shortcuts", rootInfo ,new Bundle());
    }

    public static boolean isStorage(RootInfo root){
        return root.isHome() || root.isPhoneStorage() || root.isStorage() || root.isUsbStorage();
    }

    public static boolean isLibraryMedia(RootInfo root){
        return root.isRecents() || root.isImages() || root.isVideos() || root.isAudio();
    }

    public static boolean isLibraryNonMedia(RootInfo root){
        return root.isDocument() || root.isArchive() || root.isApk();
    }

    public static boolean isLibraryExtra(RootInfo root){
        return root.isWhatsApp() || root.isTelegram() || root.isTelegramX();
    }

    public static boolean isFolder(RootInfo root){
        return root.isBluetoothFolder() || root.isDownloadsFolder() || root.isDownloads();
    }

    public static boolean isBookmark(RootInfo root){
        return root.isBookmarkFolder();
    }

    public static boolean isTools(RootInfo root){
        return root.isConnections() || root.isRootedStorage() || root.isAppPackage()
                || root.isAppProcess();
    }

    public static boolean isNetwork(RootInfo root){
        return root.isNetworkStorage();
    }

    public static boolean isCloud(RootInfo root){
        return root.isCloudStorage();
    }

    public static boolean isApps(RootInfo root){
        return root.isAppPackage() || root.isAppProcess();
    }

    public static boolean isOtherRoot(RootInfo root){
        return null != root && (root.isHome() || root.isConnections() || root.isTransfer()
                || root.isNetworkStorage() || root.isCast());
    }

    public static boolean isMedia(RootInfo root){
        return root.isImages() || root.isVideos() || root.isAudio();
    }

    public static boolean isProFeature(RootInfo root){
        return root.isSecondaryStorage() || root.isUsbStorage() || root.isRootedStorage();
    }

    public static boolean isChromecastFeature(RootInfo root){
        return RootInfo.isMedia(root) || root.isHome() || root.isStorage() || root.isCast();
    }

}