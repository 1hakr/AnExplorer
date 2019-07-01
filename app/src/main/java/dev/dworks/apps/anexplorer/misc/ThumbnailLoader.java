/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.OperationCanceledException;
import android.util.Log;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.ImageView;

import java.io.File;

import dev.dworks.apps.anexplorer.DocumentsApplication;
import dev.dworks.apps.anexplorer.libcore.util.BiConsumer;
import dev.dworks.apps.anexplorer.libcore.util.Consumer;
import dev.dworks.apps.anexplorer.model.DocumentsContract;
import dev.dworks.apps.anexplorer.provider.ExtraDocumentsProvider;

/**
 *  Loads a Thumbnails asynchronously then animates from the mime icon to the thumbnail
 */
public final class ThumbnailLoader extends AsyncTask<Uri, Void, Bitmap> implements ProviderExecutor.Preemptable {

    private static final String TAG = ThumbnailLoader.class.getCanonicalName();

    /**
     * Two animations applied to image views. The first is used to switch mime icon and thumbnail.
     * The second is used when we need to update thumbnail.
     */
    public static final BiConsumer<View, View> ANIM_FADE_IN = new BiConsumer<View, View>() {
        @Override
        public void accept(View mime, View thumb) {
            float alpha = mime.getAlpha();
            mime.animate().alpha(0f).start();
            thumb.setAlpha(0f);
            thumb.animate().alpha(alpha).start();
        }
    };

    public static final BiConsumer<View, View> ANIM_NO_OP = new BiConsumer<View, View>() {
        @Override
        public void accept(View view, View view2) {

        }
    };

    private final ImageView mIconThumb;
    private final Point mThumbSize;
    private final Uri mUri;
    private final long mLastModified;
    private final Consumer<Bitmap> mCallback;
    private final boolean mAddToCache;
    private final CancellationSignal mSignal;
    private final String mPath;
    private final String mMimeType;

    /**
     * @param uri - to a thumbnail.
     * @param iconThumb - ImageView to display the thumbnail.
     * @param thumbSize - size of the thumbnail.
     * @param lastModified - used for updating thumbnail caches.
     * @param addToCache - flag that determines if the loader saves the thumbnail to the cache.
     */
    public ThumbnailLoader(Uri uri, ImageView iconThumb, Point thumbSize, long lastModified,
                           String path, String mimeType,
                           Consumer<Bitmap> callback, boolean addToCache) {

        mUri = uri;
        mIconThumb = iconThumb;
        mThumbSize = thumbSize;
        mLastModified = lastModified;
        mCallback = callback;
        mAddToCache = addToCache;
        mSignal = new CancellationSignal();
        mIconThumb.setTag(this);
        mPath = path;
        mMimeType = mimeType;
    }

    @Override
    public void preempt() {
        cancel(false);
        mSignal.cancel();
    }

    @Override
    protected Bitmap doInBackground(Uri... params) {
        if (isCancelled()) {
            return null;
        }

        final Context context = mIconThumb.getContext();
        final ContentResolver resolver = context.getContentResolver();

        ContentProviderClient client = null;
        Bitmap result = null;
        try {
            if(URLUtil.isNetworkUrl(mUri.toString())){
                result = ImageUtils.getThumbnail(resolver, mUri, mThumbSize.x, mThumbSize.y);
            }
            if (null == result) {
                final String docId = DocumentsContract.getDocumentId(mUri);
                boolean isDir = false;
                File file = null;
                if (null != mPath){
                    file  = new File(mPath);
                    isDir = file.isDirectory();
                }
                if (Utils.isAPK(mMimeType)) {
                    result = ((BitmapDrawable) IconUtils.loadPackagePathIcon(context, mPath, DocumentsContract.Document.MIME_TYPE_APK)).getBitmap();
                } else if (Utils.isPDF(mMimeType) && Utils.hasLollipop()) {
                    result = PdfUtils.getPdfThumbnail(file, mThumbSize);
                } else if(ExtraDocumentsProvider.AUTHORITY.equals(mUri.getAuthority())
                        && !ExtraDocumentsProvider.ROOT_ID_WHATSAPP.startsWith(docId) && isDir) {
                    final File previewFile = FileUtils.getPreviewFile(file);
                    final String mimeTypePreview = FileUtils.getTypeForFile(previewFile);
                    return ImageUtils.getThumbnail(previewFile.getAbsolutePath(), mimeTypePreview, mThumbSize.x, mThumbSize.y);
                } else {
                    client = DocumentsApplication.acquireUnstableProviderOrThrow(resolver, mUri.getAuthority());
                    result = DocumentsContract.getDocumentThumbnail(resolver, mUri, mThumbSize, mSignal);
                }
            }
            if (null == result){
                result = ImageUtils.getThumbnail(mPath, mMimeType, mThumbSize.x, mThumbSize.y);
            }
            if (result != null && mAddToCache) {
                final ThumbnailCache thumbs = DocumentsApplication.getThumbnailsCache(context, mThumbSize);
                thumbs.putThumbnail(mUri, mThumbSize, result, mLastModified);
            }
        } catch (Exception e) {
            if (!(e instanceof OperationCanceledException)) {
                Log.w(TAG, "Failed to load thumbnail for " + mUri + ": " + e);
            }
            CrashReportingManager.logException(e);
        } finally {
            ContentProviderClientCompat.releaseQuietly(client);
        }
        return result;
    }

    @Override
    protected void onPostExecute(Bitmap result) {
        if (mIconThumb.getTag() == this) {
            mIconThumb.setTag(null);
            mCallback.accept(result);
        }
    }
}