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


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import dev.dworks.apps.anexplorer.BaseActivity.State.ViewMode;
import dev.dworks.apps.anexplorer.DocumentsApplication;
import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.libcore.util.BiConsumer;
import dev.dworks.apps.anexplorer.libcore.util.Consumer;
import dev.dworks.apps.anexplorer.model.DocumentInfo;
import dev.dworks.apps.anexplorer.model.DocumentsContract;
import dev.dworks.apps.anexplorer.setting.SettingsActivity;

import static dev.dworks.apps.anexplorer.BaseActivity.State.MODE_GRID;
import static dev.dworks.apps.anexplorer.BaseActivity.State.MODE_LIST;

/**
 * A class to assist with loading and managing the Images (i.e. thumbnails and icons) associated
 * with items in the directory listing.
 */
public class IconHelper {
    private static final String TAG = "IconHelper";

    private final Context mContext;
    private final ThumbnailCache mThumbnailCache;

    // The display mode (MODE_GRID, MODE_LIST, etc).
    private int mMode;
    private Point mCurrentSize;
    private boolean mThumbnailsEnabled = true;

    /**
     * @param context
     * @param mode MODE_GRID or MODE_LIST
     */
    public IconHelper(Context context, int mode) {
        mContext = context;
        setViewMode(mode);
        mThumbnailCache = DocumentsApplication.getThumbnailCache(context);
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
     * Sets the current display mode. This affects the thumbnail sizes that are loaded.
     *
     * @param mode See {@link State.MODE_LIST} and {@link State.MODE_GRID}.
     */
    public void setViewMode(@ViewMode int mode) {
        mMode = mode;
        int thumbSize = getThumbSize(mode);
        mCurrentSize = new Point(thumbSize, thumbSize);
    }

    private int getThumbSize(int mode) {
        int thumbSize;
        switch (mode) {
            case MODE_GRID:
                thumbSize = mContext.getResources().getDimensionPixelSize(R.dimen.grid_width);
                break;
            case MODE_LIST:
                thumbSize = mContext.getResources().getDimensionPixelSize(
                        R.dimen.icon_size);
                break;
            default:
                throw new IllegalArgumentException("Unsupported layout mode: " + mode);
        }
        return thumbSize;
    }

    /**
     * Cancels any ongoing load operations associated with the given ImageView.
     *
     * @param icon
     */
    public void stopLoading(ImageView icon) {
        final ThumbnailLoader oldTask = (ThumbnailLoader) icon.getTag();
        if (oldTask != null) {
            oldTask.preempt();
            icon.setTag(null);
        }
    }

    /**
     * Load thumbnails for a directory list item.
     *
     * @param doc The document
     * @param iconThumb The itemview's thumbnail icon.
     * @param iconMime The itemview's mime icon. Hidden when iconThumb is shown.
     * @param subIconMime The second itemview's mime icon. Always visible.
     * @return
     */
    public void load(
            DocumentInfo doc,
            ImageView iconThumb,
            ImageView iconMime,
            @Nullable View subIconMime) {
        load(doc.derivedUri, doc.path, doc.mimeType, doc.flags, doc.icon, doc.lastModified,
                iconThumb, iconMime, subIconMime);
    }

    public void load(
            Uri uri, String mimeType,
            ImageView iconThumb,
            ImageView iconMime,
            @Nullable View subIconMime) {
        loadThumbnail(uri, mimeType, iconThumb, iconMime, subIconMime);
    }

    /**
     * Load thumbnails for a directory list item.
     *
     * @param uri The URI for the file being represented.
     * @param mimeType The mime type of the file being represented.
     * @param docFlags Flags for the file being represented.
     * @param docIcon Custom icon (if any) for the file being requested.
     * @param docLastModified the last modified value of the file being requested.
     * @param iconThumb The itemview's thumbnail icon.
     * @param iconMime The itemview's mime icon. Hidden when iconThumb is shown.
     * @param subIconMime The second itemview's mime icon. Always visible.
     * @return
     */
    public void load(Uri uri, String docPath, String mimeType, int docFlags, int docIcon, long docLastModified,
            ImageView iconThumb, ImageView iconMime, @Nullable View subIconMime) {
        boolean loadedThumbnail = false;

        final String docAuthority = uri.getAuthority();

        final boolean supportsThumbnail = (docFlags & DocumentsContract.Document.FLAG_SUPPORTS_THUMBNAIL) != 0;
        final boolean allowThumbnail = (mMode == MODE_GRID)
                || MimePredicate.mimeMatches(MimePredicate.VISUAL_MIMES, mimeType);
        final boolean showThumbnail = supportsThumbnail && allowThumbnail && mThumbnailsEnabled;
        if (showThumbnail) {
            loadedThumbnail =
                loadThumbnail(uri, docAuthority, docLastModified, docPath,
                        mimeType, iconThumb, iconMime, subIconMime);
        }

        final String docId = DocumentsContract.getDocumentId(uri);
        final Drawable mimeIcon = getDocumentIcon(mContext, docAuthority,
                docId, mimeType, docIcon);

        if (subIconMime != null) {
            setMimeBackground(subIconMime, getDocumentColor(mContext, docAuthority, docId, mimeType));
        }

        if (loadedThumbnail) {
            hideImageView(iconMime);
            iconThumb.setAlpha(1f);
            subIconMime.setVisibility(View.INVISIBLE);
        } else {
            // Add a mime icon if the thumbnail is not shown.
            setMimeIcon(iconMime, mimeIcon);
            hideImageView(iconThumb);
            iconThumb.setAlpha(0f);
            subIconMime.setVisibility(View.VISIBLE);
        }
    }

    private boolean loadThumbnail(Uri uri, String docAuthority, long docLastModified, final String docPath,
                                  final String mimeType, final ImageView iconThumb,
                                  final ImageView iconMime, final View subIconMime) {
        final ThumbnailCache.Result result = mThumbnailCache.getThumbnail(uri, mCurrentSize);

        try {
            final Bitmap cachedThumbnail = result.getThumbnail();
            if(result.isExactHit()){
                setImage(iconThumb, cachedThumbnail, mimeType, docPath);
                subIconMime.setVisibility(View.INVISIBLE);
            }

            boolean stale = (docLastModified > result.getLastModified());
            if (!result.isExactHit() || stale) {
                final BiConsumer<View, View> animator =
                        (cachedThumbnail == null ? ThumbnailLoader.ANIM_FADE_IN :
                                ThumbnailLoader.ANIM_NO_OP);

                Consumer<Bitmap> callback = new Consumer<Bitmap>() {
                    @Override
                    public void accept(Bitmap bitmap) {
                        if (bitmap != null) {
                            setImage(iconThumb, bitmap, mimeType, docPath);
                            subIconMime.setVisibility(View.INVISIBLE);
                            animator.accept(iconMime, iconThumb);
                        } else {
                            subIconMime.setVisibility(View.VISIBLE);
                        }
                    }
                };

                final ThumbnailLoader task = new ThumbnailLoader(uri, iconThumb,
                    mCurrentSize, docLastModified, docPath, mimeType,
                        callback, true);

                ProviderExecutor.forAuthority(docAuthority).execute(task);
            }

            return result.isHit();
        } finally {
            result.recycle();
        }
    }

    private boolean loadThumbnail(Uri uri, final String mimeType, final ImageView iconThumb,
                                  final ImageView iconMime, final View subIconMime) {
        final ThumbnailCache.Result result = mThumbnailCache.getThumbnail(uri, mCurrentSize);

        try {
            final Bitmap cachedThumbnail = result.getThumbnail();
            if(result.isExactHit()){
                setImage(iconThumb, cachedThumbnail, mimeType, "");
                subIconMime.setVisibility(View.INVISIBLE);
            }

            if (!result.isExactHit()) {
                final BiConsumer<View, View> animator =
                        (cachedThumbnail == null ? ThumbnailLoader.ANIM_FADE_IN :
                                ThumbnailLoader.ANIM_NO_OP);

                Consumer<Bitmap> callback = new Consumer<Bitmap>() {
                    @Override
                    public void accept(Bitmap bitmap) {
                        if (bitmap != null) {
                            setImage(iconThumb, bitmap, mimeType, "");
                            subIconMime.setVisibility(View.INVISIBLE);
                            animator.accept(iconMime, iconThumb);
                        } else {
                            subIconMime.setVisibility(View.VISIBLE);
                        }
                    }
                };

                final ThumbnailLoader task = new ThumbnailLoader(uri, iconThumb,
                        mCurrentSize, 0, "", mimeType,
                        callback, true);

                String docAuthority = "ImageLoading";
                ProviderExecutor.forAuthority(docAuthority).execute(task);
            }

            return result.isHit();
        } finally {
            result.recycle();
        }
    }

    private void setMimeIcon(ImageView view, Drawable icon) {
        view.setImageDrawable(icon);
        view.setAlpha(1f);
    }

    private void hideImageView(ImageView view) {
        view.setImageDrawable(null);
        view.setAlpha(0f);
    }

    private void setMimeBackground(View view, int color) {
        view.setBackgroundColor(color);
    }

    private Drawable getDocumentIcon(
        Context context, String authority, String id, String mimeType, int icon) {
        if (icon != 0) {
            return IconUtils.loadPackageIcon(context, authority, icon);
        } else {
            return IconUtils.loadMimeIcon(context, mimeType, authority, id, mMode);
        }
    }

    /**
     * Returns a mime icon or package icon for a {@link DocumentInfo}.
     */
    public Drawable getDocumentIcon(Context context, DocumentInfo doc) {
        return getDocumentIcon(
                context, doc.authority, doc.documentId, doc.mimeType, doc.icon);
    }

    private int getDocumentColor(
            Context context, String authority, String id, String mimeType) {
        int defaultColor = SettingsActivity.getPrimaryColor();

        return IconColorUtils.loadMimeColor(context, mimeType, authority, id, defaultColor);
    }

    private void setImage(ImageView imageView, Bitmap bitmap, String docMimeType, String docPath){
        imageView.setImageBitmap(bitmap);
        imageView.setScaleType(getIconScaleType(docMimeType, docPath));
    }

    private ImageView.ScaleType getIconScaleType(String docMimeType, String docPath){
        return Utils.isAPK(docMimeType) && !TextUtils.isEmpty(docPath) ? ImageView.ScaleType.CENTER_INSIDE
                : ImageView.ScaleType.CENTER_CROP;
    }
}
