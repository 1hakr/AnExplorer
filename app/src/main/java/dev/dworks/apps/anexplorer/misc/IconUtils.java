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

package dev.dworks.apps.anexplorer.misc;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.TypedValue;

import androidx.annotation.RequiresApi;
import androidx.collection.ArrayMap;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import dev.dworks.apps.anexplorer.DocumentsActivity;
import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.model.DocumentsContract.Document;
import dev.dworks.apps.anexplorer.provider.MediaDocumentsProvider;

import static dev.dworks.apps.anexplorer.network.NetworkConnection.CLIENT;
import static dev.dworks.apps.anexplorer.network.NetworkConnection.SERVER;
import static dev.dworks.apps.anexplorer.provider.CloudStorageProvider.TYPE_BOX;
import static dev.dworks.apps.anexplorer.provider.CloudStorageProvider.TYPE_DROPBOX;
import static dev.dworks.apps.anexplorer.provider.CloudStorageProvider.TYPE_GDRIVE;
import static dev.dworks.apps.anexplorer.provider.CloudStorageProvider.TYPE_ONEDRIVE;

public class IconUtils {

    private static ArrayMap<String, Integer> sMimeIcons = new ArrayMap<>();

    private static void add(String mimeType, int resId) {
        if (sMimeIcons.put(mimeType, resId) != null) {
            throw new RuntimeException(mimeType + " already registered!");
        }
    }

    static {
        int icon;

        // Package
        icon = R.drawable.ic_doc_apk;
        add("application/vnd.android.package-archive", icon);

        // Audio
        icon = R.drawable.ic_doc_audio;
        add("application/ogg", icon);
        add("application/x-flac", icon);

        // Certificate
        icon = R.drawable.ic_doc_certificate;
        add("application/pgp-keys", icon);
        add("application/pgp-signature", icon);
        add("application/x-pkcs12", icon);
        add("application/x-pkcs7-certreqresp", icon);
        add("application/x-pkcs7-crl", icon);
        add("application/x-x509-ca-cert", icon);
        add("application/x-x509-user-cert", icon);
        add("application/x-pkcs7-certificates", icon);
        add("application/x-pkcs7-mime", icon);
        add("application/x-pkcs7-signature", icon);

        // Source code
        icon = R.drawable.ic_doc_codes;
        add("application/rdf+xml", icon);
        add("application/rss+xml", icon);
        add("application/x-object", icon);
        add("application/xhtml+xml", icon);
        add("text/css", icon);
        add("text/html", icon);
        add("text/xml", icon);
        add("text/x-c++hdr", icon);
        add("text/x-c++src", icon);
        add("text/x-chdr", icon);
        add("text/x-csrc", icon);
        add("text/x-dsrc", icon);
        add("text/x-csh", icon);
        add("text/x-haskell", icon);
        add("text/x-java", icon);
        add("text/x-literate-haskell", icon);
        add("text/x-pascal", icon);
        add("text/x-tcl", icon);
        add("text/x-tex", icon);
        add("application/x-latex", icon);
        add("application/x-texinfo", icon);
        add("application/atom+xml", icon);
        add("application/ecmascript", icon);
        add("application/json", icon);
        add("application/javascript", icon);
        add("application/xml", icon);
        add("text/javascript", icon);
        add("application/x-javascript", icon);

        // Compressed
        icon = R.drawable.ic_doc_archive;
        add("application/mac-binhex40", icon);
        add("application/rar", icon);
        add("application/zip", icon);
        add("application/x-apple-diskimage", icon);
        add("application/x-debian-package", icon);
        add("application/x-gtar", icon);
        add("application/x-iso9660-image", icon);
        add("application/x-lha", icon);
        add("application/x-lzh", icon);
        add("application/x-lzx", icon);
        add("application/x-stuffit", icon);
        add("application/x-tar", icon);
        add("application/x-webarchive", icon);
        add("application/x-webarchive-xml", icon);
        add("application/gzip", icon);
        add("application/x-7z-compressed", icon);
        add("application/x-deb", icon);
        add("application/x-rar-compressed", icon);

        // Contact
        icon = R.drawable.ic_doc_contact;
        add("text/x-vcard", icon);
        add("text/vcard", icon);

        // Event
        icon = R.drawable.ic_doc_event;
        add("text/calendar", icon);
        add("text/x-vcalendar", icon);

        // Font
        icon = R.drawable.ic_doc_font;
        add("application/x-font", icon);
        add("application/font-woff", icon);
        add("application/x-font-woff", icon);
        add("application/x-font-ttf", icon);

        // Image
        icon = R.drawable.ic_doc_image;
        add("application/vnd.oasis.opendocument.graphics", icon);
        add("application/vnd.oasis.opendocument.graphics-template", icon);
        add("application/vnd.oasis.opendocument.image", icon);
        add("application/vnd.stardivision.draw", icon);
        add("application/vnd.sun.xml.draw", icon);
        add("application/vnd.sun.xml.draw.template", icon);

        // PDF
        icon = R.drawable.ic_doc_pdf;
        add("application/pdf", icon);

        // Presentation
        icon = R.drawable.ic_doc_presentation;
        add("application/vnd.stardivision.impress", icon);
        add("application/vnd.sun.xml.impress", icon);
        add("application/vnd.sun.xml.impress.template", icon);
        add("application/x-kpresenter", icon);
        add("application/vnd.oasis.opendocument.presentation", icon);

        // Spreadsheet
        icon = R.drawable.ic_doc_spreadsheet;
        add("application/vnd.oasis.opendocument.spreadsheet", icon);
        add("application/vnd.oasis.opendocument.spreadsheet-template", icon);
        add("application/vnd.stardivision.calc", icon);
        add("application/vnd.sun.xml.calc", icon);
        add("application/vnd.sun.xml.calc.template", icon);
        add("application/x-kspread", icon);

        // Text
        icon = R.drawable.ic_doc_document;
        add("application/vnd.oasis.opendocument.text", icon);
        add("application/vnd.oasis.opendocument.text-master", icon);
        add("application/vnd.oasis.opendocument.text-template", icon);
        add("application/vnd.oasis.opendocument.text-web", icon);
        add("application/vnd.stardivision.writer", icon);
        add("application/vnd.stardivision.writer-global", icon);
        add("application/vnd.sun.xml.writer", icon);
        add("application/vnd.sun.xml.writer.global", icon);
        add("application/vnd.sun.xml.writer.template", icon);
        add("application/x-abiword", icon);
        add("application/x-kword", icon);

        // Video
        icon = R.drawable.ic_doc_video;
        add("application/x-quicktimeplayer", icon);
        add("application/x-shockwave-flash", icon);

        // Word
        icon = R.drawable.ic_doc_word;
        add("application/msword", icon);
        add("application/vnd.openxmlformats-officedocument.wordprocessingml.document", icon);
        add("application/vnd.openxmlformats-officedocument.wordprocessingml.template", icon);

        // Excel
        icon = R.drawable.ic_doc_excel;
        add("application/vnd.ms-excel", icon);
        add("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", icon);
        add("application/vnd.openxmlformats-officedocument.spreadsheetml.template", icon);

        // Powerpoint
        icon = R.drawable.ic_doc_powerpoint;
        add("application/vnd.ms-powerpoint", icon);
        add("application/vnd.openxmlformats-officedocument.presentationml.presentation", icon);
        add("application/vnd.openxmlformats-officedocument.presentationml.template", icon);
        add("application/vnd.openxmlformats-officedocument.presentationml.slideshow", icon);

        //folder
        icon = R.drawable.ic_doc_folder;
        add(Document.MIME_TYPE_HIDDEN, icon);
    }

    public static Drawable loadPackageIcon(Context context, String authority, int icon) {
        if (icon != 0) {
            if (authority != null) {
                final PackageManager pm = context.getPackageManager();
                final ProviderInfo info = pm.resolveContentProvider(authority, 0);
                if (info != null) {
                    return pm.getDrawable(info.packageName, icon, info.applicationInfo);
                }
            } else {
                return ContextCompat.getDrawable(context, icon);
            }
        }
        return null;
    }
    
    public static Drawable loadPackagePathIcon(Context context, String path, String mimeType){
    	int icon =  sMimeIcons.get(mimeType);
        if (path != null) {
            final PackageManager pm = context.getPackageManager();
			try {
				final PackageInfo packageInfo = pm.getPackageArchiveInfo(path, PackageManager.GET_ACTIVITIES);
				if (packageInfo != null) {
					packageInfo.applicationInfo.sourceDir = packageInfo.applicationInfo.publicSourceDir = path;
					// know issue with nine patch image instead of drawable
                    if(Utils.hasOreo()){
                        return new BitmapDrawable(context.getResources(), getAppIcon(pm, packageInfo.packageName));
                    } else {
                        return pm.getApplicationIcon(packageInfo.applicationInfo);
                    }
				}
			} catch (Exception e) {
				return ContextCompat.getDrawable(context, icon);
			}
        } else {
            return ContextCompat.getDrawable(context, icon);
        }
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static Bitmap getAppIcon(PackageManager mPackageManager, String packageName) {

        try {
            Drawable drawable = mPackageManager.getApplicationIcon(packageName);

            if (drawable instanceof BitmapDrawable) {
                return ((BitmapDrawable) drawable).getBitmap();
            } else if (drawable instanceof AdaptiveIconDrawable) {
                return drawableToBitmap(drawable);
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public static Drawable loadMimeIcon(
            Context context, String mimeType, String authority, String docId, int mode) {

        if (Document.MIME_TYPE_DIR.equals(mimeType)) {
            if (MediaDocumentsProvider.AUTHORITY.equals(authority)) {
                if(docId.startsWith(MediaDocumentsProvider.TYPE_ALBUM)){
                    return ContextCompat.getDrawable(context, R.drawable.ic_doc_album);
                }
                else if(docId.startsWith(MediaDocumentsProvider.TYPE_IMAGES_BUCKET)){
                    return ContextCompat.getDrawable(context, R.drawable.ic_doc_folder);
                }
                else if(docId.startsWith(MediaDocumentsProvider.TYPE_VIDEOS_BUCKET)){
                    return ContextCompat.getDrawable(context, R.drawable.ic_doc_folder);
                }
            }

            if (mode == DocumentsActivity.State.MODE_GRID) {
                return ContextCompat.getDrawable(context, R.drawable.ic_grid_folder);
            } else {
                return ContextCompat.getDrawable(context, R.drawable.ic_doc_folder);
            }
        }

        return loadMimeIcon(context, mimeType);
    }

    public static Drawable loadMimeIcon(Context context, String mimeType) {

        if (Document.MIME_TYPE_DIR.equals(mimeType)) {
            // TODO: return a mipmap, since this is used for grid
            return ContextCompat.getDrawable(context, R.drawable.ic_doc_folder);
        }

        // Look for exact match first
        Integer resId = sMimeIcons.get(mimeType);
        if (resId != null) {
            return getDrawable(context, resId);
        }

        if (mimeType == null) {
            return ContextCompat.getDrawable(context, R.drawable.ic_doc_generic);
        }

        // Otherwise look for partial match
        final String typeOnly = mimeType.split("/")[0];
        if ("audio".equals(typeOnly)) {
            return ContextCompat.getDrawable(context, R.drawable.ic_doc_audio);
        } else if ("image".equals(typeOnly)) {
            return ContextCompat.getDrawable(context, R.drawable.ic_doc_image);
        } else if ("text".equals(typeOnly)) {
            return ContextCompat.getDrawable(context, R.drawable.ic_doc_text);
        } else if ("video".equals(typeOnly)) {
            return ContextCompat.getDrawable(context, R.drawable.ic_doc_video);
        } else {
            return ContextCompat.getDrawable(context, R.drawable.ic_doc_generic);
        }
    }

    public static Drawable loadSchemeIcon(Context context, String type) {

        if (SERVER.equals(type)) {
            return ContextCompat.getDrawable(context, R.drawable.ic_connection_server);
        } else if (CLIENT.equals(type)) {
            return ContextCompat.getDrawable(context, R.drawable.ic_connection_network);
        } else {
            return ContextCompat.getDrawable(context, R.drawable.ic_connection_server);
        }
    }

    public static Drawable loadCloudIcon(Context context, String type) {

        if (TYPE_GDRIVE.equals(type)) {
            return ContextCompat.getDrawable(context, R.drawable.ic_root_gdrive);
        } else if (TYPE_DROPBOX.equals(type)) {
            return ContextCompat.getDrawable(context, R.drawable.ic_root_dropbox);
        } else if (TYPE_ONEDRIVE.equals(type)) {
            return ContextCompat.getDrawable(context, R.drawable.ic_root_onedrive);
        } else if (TYPE_BOX.equals(type)) {
            return ContextCompat.getDrawable(context, R.drawable.ic_root_box);
        } else {
            return ContextCompat.getDrawable(context, R.drawable.ic_root_cloud);
        }
    }
    
    public static String getTypeNameFromMimeType(String mimeType) {
        int resource = 0;
        if (Document.MIME_TYPE_DIR.equals(mimeType)) {
            return "folder";
        }

        // Look for exact match first
        Integer resId = sMimeIcons.get(mimeType);
        if (resId != null) {
            resource = resId;
        }


        switch (resource) {
            case R.drawable.ic_doc_apk:
                return "apk";
            case R.drawable.ic_doc_audio:
                return "audio";
            case R.drawable.ic_doc_certificate:
                return "certificate";
            case R.drawable.ic_doc_codes:
                return "source code";
            case R.drawable.ic_doc_archive:
                return "compressed";
            case R.drawable.ic_doc_contact:
                return "contact";
            case R.drawable.ic_doc_event:
                return "event";
            case R.drawable.ic_doc_font:
                return "font";
            case R.drawable.ic_doc_image:
                return "image";
            case R.drawable.ic_doc_pdf:
                return "pdf";
            case R.drawable.ic_doc_presentation:
                return "presentation";
            case R.drawable.ic_doc_spreadsheet:
                return "spreadsheet";
            case R.drawable.ic_doc_text:
                return "text";
            case R.drawable.ic_doc_video:
                return "video";
        }

        if (mimeType == null) {
            return "file";
        }

        // Otherwise look for partial match
        final String typeOnly = mimeType.split("/")[0];
        if ("audio".equals(typeOnly)) {
            return typeOnly;
        } else if ("image".equals(typeOnly)) {
            return typeOnly;
        } else if ("text".equals(typeOnly)) {
            return typeOnly;
        } else if ("video".equals(typeOnly)) {
            return typeOnly;
        }

        return "file";
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static Drawable applyTintList(Context context, int drawableId, int tintColorId) {
        final Drawable icon = getDrawable(context, drawableId);
        icon.mutate();
        DrawableCompat.setTintList(DrawableCompat.wrap(icon), ContextCompat.getColorStateList(context, tintColorId));
        return icon;
    }

    public static Drawable applyTint(Context context, int drawableId, int tintColorId) {
        final Drawable icon = getDrawable(context, drawableId);
        icon.mutate();
        DrawableCompat.setTint(DrawableCompat.wrap(icon), tintColorId);
        return icon;
    }

    public static Drawable applyTintAttr(Context context, int drawableId, int tintAttrId) {
        final TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(tintAttrId, outValue, true);
        return applyTintList(context, drawableId, outValue.resourceId);
    }

    private static Drawable getDrawable(Context context, int drawableId){
        try {
            return ContextCompat.getDrawable(context, drawableId);
        } catch (Resources.NotFoundException e){
            return ContextCompat.getDrawable(context, R.drawable.ic_doc_generic);
        }
    }
}