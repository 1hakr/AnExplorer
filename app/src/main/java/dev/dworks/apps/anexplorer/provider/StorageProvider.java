package dev.dworks.apps.anexplorer.provider;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio;
import android.provider.MediaStore.Audio.AudioColumns;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.provider.MediaStore.Video;
import android.provider.MediaStore.Video.VideoColumns;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;

import dev.dworks.apps.anexplorer.archive.DocumentArchiveHelper;
import dev.dworks.apps.anexplorer.libcore.io.IoUtils;
import dev.dworks.apps.anexplorer.model.DocumentsContract;

import static dev.dworks.apps.anexplorer.misc.ContentProviderClientCompat.buildAssetFileDescriptor;

public abstract class StorageProvider extends DocumentsProvider{

	private static final String TAG = "StorageProvider";

    public static final Uri FILE_URI = MediaStore.Files.getContentUri("external");
    protected DocumentArchiveHelper mArchiveHelper;

    @Override
    public boolean onCreate() {
        mArchiveHelper = new DocumentArchiveHelper(this, (char) 0);
        return super.onCreate();
    }

    protected interface AudioAlbumThumbnailQuery {
        String[] PROJECTION = new String[] {
        		Audio.Media._ID,
        		Audio.Media.ALBUM_ID,
        		Audio.Media.DATE_MODIFIED };

        int _ID = 0;
        int ALBUM_ID = 1;
        int DATE_MODIFIED = 2;
    }

    protected long getAlbumForPathCleared(String path) throws FileNotFoundException {
        final ContentResolver resolver = getContext().getContentResolver();
        Cursor cursor = null;
        try {
            cursor = resolver.query(Audio.Media.EXTERNAL_CONTENT_URI,
            		AudioAlbumThumbnailQuery.PROJECTION, Audio.Media.DATA + " LIKE ?",
            		new String[] { path.replaceAll("'", "''") }, Audio.Media.DATE_MODIFIED + " DESC");
            if (cursor.moveToFirst()) {
                return cursor.getLong(AudioAlbumThumbnailQuery.ALBUM_ID);
            }
        } finally {
            IoUtils.closeQuietly(cursor);
        }
        throw new FileNotFoundException("No Audio found for album");
    }

    protected long getAlbumForAudioCleared(long album) throws FileNotFoundException {
        final ContentResolver resolver = getContext().getContentResolver();
        Cursor cursor = null;
        try {
            cursor = resolver.query(Audio.Media.EXTERNAL_CONTENT_URI,
            		AudioAlbumThumbnailQuery.PROJECTION, AudioColumns._ID + "=" + album,
                    null, AudioColumns.DATE_MODIFIED + " DESC");
            if (cursor.moveToFirst()) {
                return cursor.getLong(AudioAlbumThumbnailQuery.ALBUM_ID);
            }
        } finally {
            IoUtils.closeQuietly(cursor);
        }
        throw new FileNotFoundException("No Audio found for album");
    }

    protected interface AudioThumbnailQuery {
        String[] PROJECTION = new String[] {
                Audio.Albums.ALBUM_ART };

        int _DATA = 0;
    }

    protected ParcelFileDescriptor openAudioThumbnailCleared(long id, CancellationSignal signal)
            throws FileNotFoundException {
        final ContentResolver resolver = getContext().getContentResolver();

        Cursor cursor = null;
        try {
            cursor = resolver.query(Audio.Albums.EXTERNAL_CONTENT_URI,
            		AudioThumbnailQuery.PROJECTION, Audio.Albums._ID + "=" + id,
                    null, null);
            if (cursor.moveToFirst()) {
                final String data = cursor.getString(AudioThumbnailQuery._DATA);
                return ParcelFileDescriptor.open(
                        new File(data), ParcelFileDescriptor.MODE_READ_ONLY);
            }
        } finally {
            IoUtils.closeQuietly(cursor);
        }
        return null;
    }

    protected AssetFileDescriptor openOrCreateAudioThumbnailCleared(
            long id, CancellationSignal signal) throws FileNotFoundException {
        final ContentResolver resolver = getContext().getContentResolver();

        ParcelFileDescriptor pfd = openAudioThumbnailCleared(id, signal);
        if (pfd == null) {
            // No thumbnail yet, so generate. This is messy, since we drop the
            // Bitmap on the floor, but its the least-complicated way.
            final BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            Images.Thumbnails.getThumbnail(resolver, id, Images.Thumbnails.MINI_KIND, opts);

            pfd = openAudioThumbnailCleared(id, signal);
        }

        if (pfd == null) {
            // Phoey, fallback to full image
            final Uri fullUri = ContentUris.withAppendedId(Images.Media.EXTERNAL_CONTENT_URI, id);
            pfd = resolver.openFileDescriptor(fullUri, "r");
        }

        final int orientation = queryOrientationForImage(id, signal);
        final Bundle extras;
        if (orientation != 0) {
            extras = new Bundle(1);
            extras.putInt(DocumentsContract.EXTRA_ORIENTATION, orientation);
        } else {
            extras = null;
        }

        return buildAssetFileDescriptor(pfd, 0, AssetFileDescriptor.UNKNOWN_LENGTH, extras);
    }


    protected interface ImagesBucketThumbnailQuery {
        String[] PROJECTION = new String[] {
                ImageColumns._ID,
                ImageColumns.BUCKET_ID,
                ImageColumns.DATE_MODIFIED };

        int _ID = 0;
        int BUCKET_ID = 1;
        int DATE_MODIFIED = 2;
    }

    protected long getImageForPathCleared(String path) throws FileNotFoundException {
        final ContentResolver resolver = getContext().getContentResolver();
        Cursor cursor = null;
        try {
            cursor = resolver.query(Images.Media.EXTERNAL_CONTENT_URI,
                    ImagesBucketThumbnailQuery.PROJECTION, ImageColumns.DATA + "= ? ",
                    new String[] { path.replaceAll("'", "''") }, ImageColumns.DATE_MODIFIED + " DESC");
            if (cursor.moveToFirst()) {
                return cursor.getLong(ImagesBucketThumbnailQuery._ID);
            }
        } finally {
            IoUtils.closeQuietly(cursor);
        }
        throw new FileNotFoundException("No image found for bucket");
    }

    protected long getImageForBucketCleared(long bucketId) throws FileNotFoundException {
        final ContentResolver resolver = getContext().getContentResolver();
        Cursor cursor = null;
        try {
            cursor = resolver.query(Images.Media.EXTERNAL_CONTENT_URI,
                    ImagesBucketThumbnailQuery.PROJECTION, ImageColumns.BUCKET_ID + "=" + bucketId,
                    null, ImageColumns.DATE_MODIFIED + " DESC");
            if (cursor.moveToFirst()) {
                return cursor.getLong(ImagesBucketThumbnailQuery._ID);
            }
        } finally {
            IoUtils.closeQuietly(cursor);
        }
        throw new FileNotFoundException("No video found for bucket");
    }
    
    protected interface ImageThumbnailQuery {
        String[] PROJECTION = new String[] {
                Images.Thumbnails.DATA };

        int _DATA = 0;
    }

    protected ParcelFileDescriptor openImageThumbnailCleared(long id, CancellationSignal signal)
            throws FileNotFoundException {
        final ContentResolver resolver = getContext().getContentResolver();

        Cursor cursor = null;
        try {
            cursor = resolver.query(Images.Thumbnails.EXTERNAL_CONTENT_URI,
                    ImageThumbnailQuery.PROJECTION, Images.Thumbnails.IMAGE_ID + "=" + id, null,
                    null);
            if (cursor.moveToFirst()) {
                final String data = cursor.getString(ImageThumbnailQuery._DATA);
                return ParcelFileDescriptor.open(
                        new File(data), ParcelFileDescriptor.MODE_READ_ONLY);
            }
        } finally {
            IoUtils.closeQuietly(cursor);
        }
        return null;
    }

    protected AssetFileDescriptor openOrCreateImageThumbnailCleared(
            long id, CancellationSignal signal) throws FileNotFoundException {
        final ContentResolver resolver = getContext().getContentResolver();

        ParcelFileDescriptor pfd = openImageThumbnailCleared(id, signal);
        if (pfd == null) {
            // No thumbnail yet, so generate. This is messy, since we drop the
            // Bitmap on the floor, but its the least-complicated way.
            final BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            Images.Thumbnails.getThumbnail(resolver, id, Images.Thumbnails.MINI_KIND, opts);

            pfd = openImageThumbnailCleared(id, signal);
        }

        if (pfd == null) {
            // Phoey, fallback to full image
            final Uri fullUri = ContentUris.withAppendedId(Images.Media.EXTERNAL_CONTENT_URI, id);
            pfd = resolver.openFileDescriptor(fullUri, "r");
        }

        final int orientation = queryOrientationForImage(id, signal);
        final Bundle extras;
        if (orientation != 0) {
            extras = new Bundle(1);
            extras.putInt(DocumentsContract.EXTRA_ORIENTATION, orientation);
        } else {
            extras = null;
        }

        return buildAssetFileDescriptor(pfd, 0, AssetFileDescriptor.UNKNOWN_LENGTH, extras);
    }

    protected interface VideosBucketThumbnailQuery {
        String[] PROJECTION = new String[] {
                VideoColumns._ID,
                VideoColumns.BUCKET_ID,
                VideoColumns.DATE_MODIFIED };

        int _ID = 0;
        int BUCKET_ID = 1;
        int DATE_MODIFIED = 2;
    }

    protected long getVideoForPathCleared(String path)throws FileNotFoundException {
        final ContentResolver resolver = getContext().getContentResolver();
        Cursor cursor = null;
        try {
            cursor = resolver.query(Video.Media.EXTERNAL_CONTENT_URI,
                    VideosBucketThumbnailQuery.PROJECTION, VideoColumns.DATA + "=? ",
                    new String[] { path.replaceAll("'", "''") }, VideoColumns.DATE_MODIFIED + " DESC");
            if (cursor.moveToFirst()) {
                return cursor.getLong(VideosBucketThumbnailQuery._ID);
            }
        } finally {
            IoUtils.closeQuietly(cursor);
        }
        throw new FileNotFoundException("No video found for bucket");
    }
    

    protected long getVideoForBucketCleared(long bucketId)
            throws FileNotFoundException {
        final ContentResolver resolver = getContext().getContentResolver();
        Cursor cursor = null;
        try {
            cursor = resolver.query(Video.Media.EXTERNAL_CONTENT_URI,
                    VideosBucketThumbnailQuery.PROJECTION, VideoColumns.BUCKET_ID + "=" + bucketId,
                    null, VideoColumns.DATE_MODIFIED + " DESC");
            if (cursor.moveToFirst()) {
                return cursor.getLong(VideosBucketThumbnailQuery._ID);
            }
        } finally {
            IoUtils.closeQuietly(cursor);
        }
        throw new FileNotFoundException("No video found for bucket");
    }

    protected interface VideoThumbnailQuery {
        String[] PROJECTION = new String[] {
                Video.Thumbnails.DATA };

        int _DATA = 0;
    }

    protected AssetFileDescriptor openVideoThumbnailCleared(long id, CancellationSignal signal)
            throws FileNotFoundException {
        final ContentResolver resolver = getContext().getContentResolver();
        Cursor cursor = null;
        try {
            cursor = resolver.query(Video.Thumbnails.EXTERNAL_CONTENT_URI,
                    VideoThumbnailQuery.PROJECTION, Video.Thumbnails.VIDEO_ID + "=" + id, null,
                    null);
            if (cursor.moveToFirst()) {
                final String data = cursor.getString(VideoThumbnailQuery._DATA);
                return new AssetFileDescriptor(ParcelFileDescriptor.open(
                        new File(data), ParcelFileDescriptor.MODE_READ_ONLY), 0,
                        AssetFileDescriptor.UNKNOWN_LENGTH);
            }
        } finally {
            IoUtils.closeQuietly(cursor);
        }
        return null;
    }

    protected AssetFileDescriptor openOrCreateVideoThumbnailCleared(
            long id, CancellationSignal signal) throws FileNotFoundException {
        final ContentResolver resolver = getContext().getContentResolver();

        AssetFileDescriptor afd = openVideoThumbnailCleared(id, signal);
        if (afd == null) {
            // No thumbnail yet, so generate. This is messy, since we drop the
            // Bitmap on the floor, but its the least-complicated way.
            final BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            Video.Thumbnails.getThumbnail(resolver, id, Video.Thumbnails.MINI_KIND, opts);

            afd = openVideoThumbnailCleared(id, signal);
        }

        return afd;
    }

    protected interface ImageOrientationQuery {
        String[] PROJECTION = new String[] {
                ImageColumns.ORIENTATION };

        int ORIENTATION = 0;
    }

    protected int queryOrientationForImage(long id, CancellationSignal signal) {
        final ContentResolver resolver = getContext().getContentResolver();

        Cursor cursor = null;
        try {
            cursor = resolver.query(Images.Media.EXTERNAL_CONTENT_URI,
                    ImageOrientationQuery.PROJECTION, ImageColumns._ID + "=" + id, null, null);
            if (cursor.moveToFirst()) {
                return cursor.getInt(ImageOrientationQuery.ORIENTATION);
            } else {
                Log.w(TAG, "Missing orientation data for " + id);
                return 0;
            }
        } finally {
            IoUtils.closeQuietly(cursor);
        }
    }
}