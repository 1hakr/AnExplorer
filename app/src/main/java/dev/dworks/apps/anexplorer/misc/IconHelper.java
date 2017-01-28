/*
 * Copyright (C) 2015 The Android Open Source Project
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
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.OperationCanceledException;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import dev.dworks.apps.anexplorer.DocumentsActivity;
import dev.dworks.apps.anexplorer.DocumentsApplication;
import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.model.DocumentsContract;
import dev.dworks.apps.anexplorer.model.DocumentsContract.Document;


/**
 * A class to assist with loading and managing the Images (i.e. thumbnails and icons) associated
 * with items in the directory listing.
 */
public class IconHelper {
    private static String TAG = "IconHelper";

    private final Context mContext;
    private ThumbnailCache mCache;
    private Point mThumbSize;
    private boolean mThumbnailsEnabled = true;

    public IconHelper(Context context) {
        mContext = context;
        int thumbSize = mContext.getResources().getDimensionPixelSize(R.dimen.grid_width);
        mThumbSize = new Point(thumbSize, thumbSize);
        mCache = DocumentsApplication.getThumbnailsCache(mContext, mThumbSize);
    }

    /**
     * Enables or disables thumbnails. When thumbnails are disabled, mime icons (or custom icons, if
     * specified by the document) are used instead.
     *
     * @param enabled
     */
    public void setThumbnailsEnabled(boolean enabled) {
        mThumbnailsEnabled = enabled;
    }

    /**
     * Cancels any ongoing load operations associated with the given ImageView.
     * @param icon
     */
    public void stopLoading(ImageView icon) {
        if(null == icon){
            return;
        }
        final LoaderTask oldTask = (LoaderTask) icon.getTag();
        if (oldTask != null) {
            oldTask.preempt();
            icon.setTag(null);
        }
    }

    /** Internal task for loading thumbnails asynchronously. */
    private static class LoaderTask
            extends AsyncTask<Uri, Void, Bitmap>
            implements ProviderExecutor.Preemptable {
        private final Uri mUri;
        private final ImageView mIconThumb;
        private final ImageView mIconMime;
        private final View mIconMimeBackground;
        private final Point mThumbSize;
        private final CancellationSignal mSignal;
        private final String mPath;
        private final String mimeType;

        public LoaderTask(Uri uri, String path, String mime,
                Point thumbSize, ImageView iconThumb, ImageView iconMime, View iconMimeBackground) {
            mUri = uri;
            mIconThumb = iconThumb;
            mIconMime = iconMime;
            mThumbSize = thumbSize;
            mIconMimeBackground = iconMimeBackground;
            mimeType = mime;
            mSignal = new CancellationSignal();
            mPath = path;
        }

        @Override
        public void preempt() {
            cancel(false);
            mSignal.cancel();
        }

        @Override
        protected Bitmap doInBackground(Uri... params) {
            if (isCancelled())
                return null;

            final Context context = mIconThumb.getContext();
            final ContentResolver resolver = context.getContentResolver();

            ContentProviderClient client = null;
            Bitmap result = null;
            try {
                client = DocumentsApplication.acquireUnstableProviderOrThrow(resolver, mUri.getAuthority());
                result = DocumentsContract.getDocumentThumbnail(resolver, mUri, mThumbSize, mSignal);

                if (null == result){
                    result = ImageUtils.getThumbnail(mPath, mimeType, mThumbSize.x, mThumbSize.y);
                }
                if (result != null) {
                    final ThumbnailCache thumbs = DocumentsApplication.getThumbnailsCache(context, mThumbSize);
                    thumbs.put(mUri, result);
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
            if (isCancelled()) {
                result = null;
            }
            if (mIconThumb.getTag() == this && result != null) {
                mIconThumb.setTag(null);
                mIconThumb.setImageBitmap(result);
                mIconMimeBackground.setVisibility(View.GONE);

                float alpha = mIconMime.getAlpha();
                mIconMime.animate().alpha(0f).start();
                mIconThumb.setAlpha(0f);
                mIconThumb.animate().alpha(alpha).start();
            }
        }
    }

    /**
     * Load thumbnails for a directory list item.
     * @param uri The URI for the file being represented.
     * @param mimeType The mime type of the file being represented.
     * @param docFlags Flags for the file being represented.
     * @param iconThumb The itemview's thumbnail icon.
     * @param iconMimeBackground
     * @return
     */
    public void loadThumbnail(Uri uri, String path, String mimeType, int docFlags, int docIcon,
                              ImageView iconMime, ImageView iconThumb, View iconMimeBackground) {
        boolean cacheHit = false;

        final String docAuthority = uri.getAuthority();
        String docId = DocumentsContract.getDocumentId(uri);
        final boolean supportsThumbnail = (docFlags & Document.FLAG_SUPPORTS_THUMBNAIL) != 0;
        final boolean allowThumbnail = MimePredicate.mimeMatches(MimePredicate.VISUAL_MIMES, mimeType);
        final boolean showThumbnail = supportsThumbnail && allowThumbnail && mThumbnailsEnabled;
        if (showThumbnail) {
            final Bitmap cachedResult = mCache.get(uri);
            if (cachedResult != null) {
                iconThumb.setImageBitmap(cachedResult);
                cacheHit = true;
                iconMimeBackground.setVisibility(View.GONE);
            } else {
                iconThumb.setImageDrawable(null);
                final LoaderTask task = new LoaderTask(uri, path, mimeType, mThumbSize, iconThumb,
                        iconMime, iconMimeBackground);
                iconThumb.setTag(task);
                ProviderExecutor.forAuthority(docAuthority).execute(task);
            }
        }

        if (cacheHit) {
            iconMime.setImageDrawable(null);
            iconMime.setAlpha(0f);
            iconThumb.setAlpha(1f);
        } else {
            // Add a mime icon if the thumbnail is being loaded in the background.
            iconThumb.setImageDrawable(null);
            iconMime.setImageDrawable(getDocumentIcon(mContext, docAuthority, docId, mimeType, docIcon));
            iconMime.setAlpha(1f);
            iconThumb.setAlpha(0f);
        }
    }

    /**
     * Gets a mime icon or package icon for a file.
     * @param context
     * @param authority The authority string of the file.
     * @param id The document ID of the file.
     * @param mimeType The mime type of the file.
     * @param icon The custom icon (if any) of the file.
     * @return
     */
    public Drawable getDocumentIcon(Context context, String authority, String id,
                                    String mimeType, int icon) {
        if (icon != 0) {
            return IconUtils.loadPackageIcon(context, authority, icon);
        } else {
            return IconUtils.loadMimeIcon(context, mimeType, authority, id, DocumentsActivity.State.MODE_GRID);
        }
    }
}
