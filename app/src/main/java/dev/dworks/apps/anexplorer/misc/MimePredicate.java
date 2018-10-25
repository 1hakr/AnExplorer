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


import android.text.TextUtils;

import dev.dworks.apps.anexplorer.libcore.util.Predicate;
import dev.dworks.apps.anexplorer.model.DocumentInfo;
import dev.dworks.apps.anexplorer.model.DocumentsContract;
import dev.dworks.apps.anexplorer.model.DocumentsContract.Document;

public class MimePredicate implements Predicate<DocumentInfo> {
    private final String[] mFilters;

    /**
     * MIME types that are visual in nature. For example, they should always be
     * shown as thumbnails in list mode.
     */
    public static final String[] VISUAL_MIMES = new String[] { 
    	"image/*",
    	"video/*",
    	"audio/*",
    	Document.MIME_TYPE_APK};

    public static final String[] MEDIA_MIMES = new String[] {
            "image/*",
            "video/*",
            "audio/*"};

    public static final String[] SPECIAL_MIMES = new String[] { 
    	"application/zip",
    	"application/rar",
    	"application/gzip",
    	Document.MIME_TYPE_APK};
    
    public static final String[] COMPRESSED_MIMES = new String[] { 
    	"application/zip",
    	"application/rar",
    	"application/gzip"};

    public static final String[] SHARE_SKIP_MIMES = new String[] {
            DocumentsContract.Document.MIME_TYPE_APK };

    public static final String[] TEXT_MIMES = new String[] {
            "text/*", };

    public MimePredicate(String[] filters) {
        mFilters = filters;
    }

    @Override
    public boolean apply(DocumentInfo doc) {
        if (doc.isDirectory()) {
            return true;
        }
        return mimeMatches(mFilters, doc.mimeType);
    }

    public static boolean mimeMatches(String[] filters, String[] tests) {
        if (tests == null) {
            return false;
        }
        for (String test : tests) {
            if (mimeMatches(filters, test)) {
                return true;
            }
        }
        return false;
    }

    public static boolean mimeMatches(String filter, String[] tests) {
        if (tests == null) {
            return true;
        }
        for (String test : tests) {
            if (mimeMatches(filter, test)) {
                return true;
            }
        }
        return false;
    }

    public static boolean mimeMatches(String[] filters, String test) {
        if (filters == null) {
            return true;
        }

        if( TextUtils.isEmpty(test)){
            return false;
        }

        for (String filter : filters) {
            if (mimeMatches(filter, test)) {
                return true;
            }
        }
        return false;
    }

    public static boolean mimeMatches(String filter, String test) {
        if (test == null) {
            return false;
        } else if (filter == null || "*/*".equals(filter)) {
            return true;
        } else if (filter.equals(test)) {
            return true;
        } else if (filter.endsWith("/*")) {
            return filter.regionMatches(0, test, 0, filter.indexOf('/'));
        } else {
            return false;
        }
    }
}