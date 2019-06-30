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

import android.content.Context;

import androidx.collection.ArrayMap;
import androidx.core.content.ContextCompat;
import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.model.DocumentsContract.Document;
import dev.dworks.apps.anexplorer.provider.ExtraDocumentsProvider;
import dev.dworks.apps.anexplorer.provider.MediaDocumentsProvider;
import dev.dworks.apps.anexplorer.provider.NonMediaDocumentsProvider;

import static dev.dworks.apps.anexplorer.network.NetworkConnection.CLIENT;
import static dev.dworks.apps.anexplorer.network.NetworkConnection.SERVER;
import static dev.dworks.apps.anexplorer.provider.CloudStorageProvider.TYPE_BOX;
import static dev.dworks.apps.anexplorer.provider.CloudStorageProvider.TYPE_DROPBOX;
import static dev.dworks.apps.anexplorer.provider.CloudStorageProvider.TYPE_GDRIVE;
import static dev.dworks.apps.anexplorer.provider.CloudStorageProvider.TYPE_ONEDRIVE;

public class IconColorUtils {

    private static ArrayMap<String, Integer> sMimeColors = new ArrayMap<>();

    private static void add(String mimeType, int resId) {
        if (sMimeColors.put(mimeType, resId) != null) {
            throw new RuntimeException(mimeType + " already registered!");
        }
    }

    static {
        int icon;

        // Package
        icon = R.color.item_doc_apk;
        add("application/vnd.android.package-archive", icon);

        // Audio
        icon = R.color.item_doc_audio;
        add("application/ogg", icon);
        add("application/x-flac", icon);

        // Certificate
        icon = R.color.item_doc_certificate;
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
        icon = R.color.item_doc_code;
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
        icon = R.color.item_doc_compressed;
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
        icon = R.color.item_doc_contact;
        add("text/x-vcard", icon);
        add("text/vcard", icon);

        // Event
        icon = R.color.item_doc_event;
        add("text/calendar", icon);
        add("text/x-vcalendar", icon);

        // Font
        icon = R.color.item_doc_font;
        add("application/x-font", icon);
        add("application/font-woff", icon);
        add("application/x-font-woff", icon);
        add("application/x-font-ttf", icon);

        // Image
        icon = R.color.item_doc_image;
        add("application/vnd.oasis.opendocument.graphics", icon);
        add("application/vnd.oasis.opendocument.graphics-template", icon);
        add("application/vnd.oasis.opendocument.image", icon);
        add("application/vnd.stardivision.draw", icon);
        add("application/vnd.sun.xml.draw", icon);
        add("application/vnd.sun.xml.draw.template", icon);

        // PDF
        icon = R.color.item_doc_pdf;
        add("application/pdf", icon);

        // Presentation
        icon = R.color.item_doc_slide;
        add("application/vnd.stardivision.impress", icon);
        add("application/vnd.sun.xml.impress", icon);
        add("application/vnd.sun.xml.impress.template", icon);
        add("application/x-kpresenter", icon);
        add("application/vnd.oasis.opendocument.presentation", icon);

        // Spreadsheet
        icon = R.color.item_doc_sheet;
        add("application/vnd.oasis.opendocument.spreadsheet", icon);
        add("application/vnd.oasis.opendocument.spreadsheet-template", icon);
        add("application/vnd.stardivision.calc", icon);
        add("application/vnd.sun.xml.calc", icon);
        add("application/vnd.sun.xml.calc.template", icon);
        add("application/x-kspread", icon);

        // Text
        icon = R.color.item_doc_doc;
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
        icon = R.color.item_doc_video;
        add("application/x-quicktimeplayer", icon);
        add("application/x-shockwave-flash", icon);

        // Word
        icon = R.color.item_doc_word;
        add("application/msword", icon);
        add("application/vnd.openxmlformats-officedocument.wordprocessingml.document", icon);
        add("application/vnd.openxmlformats-officedocument.wordprocessingml.template", icon);

        // Excel
        icon = R.color.item_doc_excel;
        add("application/vnd.ms-excel", icon);
        add("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", icon);
        add("application/vnd.openxmlformats-officedocument.spreadsheetml.template", icon);

        // Powerpoint
        icon = R.color.item_doc_powerpoint;
        add("application/vnd.ms-powerpoint", icon);
        add("application/vnd.openxmlformats-officedocument.presentationml.presentation", icon);
        add("application/vnd.openxmlformats-officedocument.presentationml.template", icon);
        add("application/vnd.openxmlformats-officedocument.presentationml.slideshow", icon);

        //folder
        icon = R.color.item_doc_file;
        add(Document.MIME_TYPE_HIDDEN, icon);
    }

    public static int loadMimeColor(Context context, String mimeType,
                                    String authority, String docId, int defaultColor) {

        if (Utils.isDir(mimeType)) {
            if (MediaDocumentsProvider.AUTHORITY.equals(authority)){
                if(docId.startsWith(MediaDocumentsProvider.TYPE_ALBUM)){
                    return ContextCompat.getColor(context, R.color.item_doc_audio);
                }
                else if(docId.startsWith(MediaDocumentsProvider.TYPE_IMAGES_BUCKET)){
                    return ContextCompat.getColor(context, R.color.item_doc_image);
                }
                else if(docId.startsWith(MediaDocumentsProvider.TYPE_VIDEOS_BUCKET)){
                    return ContextCompat.getColor(context, R.color.item_doc_video);
                }
            } else if (NonMediaDocumentsProvider.AUTHORITY.equals(authority)){
                if(docId.startsWith(NonMediaDocumentsProvider.TYPE_APK_ROOT)){
                    return ContextCompat.getColor(context, R.color.item_doc_apk);
                }
                else if(docId.startsWith(NonMediaDocumentsProvider.TYPE_ARCHIVE_ROOT)){
                    return ContextCompat.getColor(context, R.color.item_doc_compressed);
                }
                else if(docId.startsWith(NonMediaDocumentsProvider.TYPE_DOCUMENT_ROOT)){
                    return ContextCompat.getColor(context, R.color.item_doc_pdf);
                }
            } else if (ExtraDocumentsProvider.AUTHORITY.equals(authority)){
                if(docId.startsWith(ExtraDocumentsProvider.ROOT_ID_WHATSAPP)){
                    return ContextCompat.getColor(context, R.color.item_whatsapp);
                }
                else if(docId.startsWith(ExtraDocumentsProvider.ROOT_ID_TELEGRAMX)){
                    return ContextCompat.getColor(context, R.color.item_telegramx);
                }
                else if(docId.startsWith(ExtraDocumentsProvider.ROOT_ID_TELEGRAM)){
                    return ContextCompat.getColor(context, R.color.item_telegram);
                }
            }
            return defaultColor;
        }

        // Look for exact match first
        Integer resId = sMimeColors.get(mimeType);
        if (resId != null) {
            return ContextCompat.getColor(context, resId);
        }

        if (mimeType == null) {
            // TODO: generic icon?
            return ContextCompat.getColor(context, R.color.item_doc_generic);
        }

        // Otherwise look for partial match
        final String typeOnly = mimeType.split("/")[0];

        if ("audio".equals(typeOnly)) {
            return ContextCompat.getColor(context, R.color.item_doc_audio);
        } else if ("image".equals(typeOnly)) {
            return ContextCompat.getColor(context, R.color.item_doc_image);
        } else if ("text".equals(typeOnly)) {
            return ContextCompat.getColor(context, R.color.item_doc_video);
        } else if ("video".equals(typeOnly)) {
            return ContextCompat.getColor(context, R.color.item_doc_video);
        } else {
            return ContextCompat.getColor(context, R.color.item_doc_file);
        }
    }

    public static int loadSchmeColor(Context context, String type) {

        if (SERVER.equals(type)) {
            return ContextCompat.getColor(context, R.color.item_connection_server);
        } else if (CLIENT.equals(type)) {
            return ContextCompat.getColor(context, R.color.item_connection_client);
        } else {
            return ContextCompat.getColor(context, R.color.item_connection_server);
        }
    }

    public static int loadCloudColor(Context context, String type) {

        if (TYPE_GDRIVE.equals(type)) {
            return ContextCompat.getColor(context, R.color.item_connection_gdrive);
        } else if (TYPE_DROPBOX.equals(type)) {
            return ContextCompat.getColor(context, R.color.item_connection_dropbox);
        } else if (TYPE_ONEDRIVE.equals(type)) {
            return ContextCompat.getColor(context, R.color.item_connection_onedrive);
        } else if (TYPE_BOX.equals(type)) {
            return ContextCompat.getColor(context, R.color.item_connection_box);
        } else {
            return ContextCompat.getColor(context, R.color.item_connection_cloud);
        }
    }
}