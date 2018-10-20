package dev.dworks.apps.anexplorer.cast;

import android.net.Uri;
import android.text.TextUtils;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.common.images.WebImage;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.IntDef;

/**
 * Media information class
 */
public class MediaData {
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({STREAM_TYPE_NONE, STREAM_TYPE_BUFFERED, STREAM_TYPE_LIVE})
    public @interface StreamType {}
    public static final int STREAM_TYPE_NONE = 0;
    public static final int STREAM_TYPE_BUFFERED = 1;
    public static final int STREAM_TYPE_LIVE = 2;
    public static final int STREAM_TYPE_INVALID = -1;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({MEDIA_TYPE_GENERIC, MEDIA_TYPE_MOVIE, MEDIA_TYPE_TV_SHOW, MEDIA_TYPE_MUSIC_TRACK, MEDIA_TYPE_PHOTO, MEDIA_TYPE_USER})
    public @interface MediaType {}
    public static final int MEDIA_TYPE_GENERIC = 0;
    public static final int MEDIA_TYPE_MOVIE = 1;
    public static final int MEDIA_TYPE_TV_SHOW = 2;
    public static final int MEDIA_TYPE_MUSIC_TRACK = 3;
    public static final int MEDIA_TYPE_PHOTO = 4;
    public static final int MEDIA_TYPE_USER = 100;

    public static final long UNKNOWN_DURATION = -1L;

    private String url;
    private int streamType = STREAM_TYPE_NONE;
    private String contentType;
    private long streamDuration = UNKNOWN_DURATION;

    private int mediaType = MEDIA_TYPE_GENERIC;
    private String title;
    private String subtitle;

    boolean autoPlay = true;
    long position;

    private List<String> imageUrls;

    private MediaData(String url) {
        this.url = url;
        imageUrls = new ArrayList<>();
    }

    private void setStreamType(int streamType) {
        this.streamType = streamType;
    }

    private void setContentType(String contentType) {
        this.contentType = contentType;
    }

    private void setStreamDuration(long streamDuration) {
        this.streamDuration = streamDuration;
    }

    private void setTitle(String title) {
        this.title = title;
    }

    private void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    private void setMediaType(int mediaType) {
        this.mediaType = mediaType;
    }

    private void setAutoPlay(boolean autoPlay) {
        this.autoPlay = autoPlay;
    }

    private void setPosition(long position) {
        this.position = position;
    }

    MediaInfo createMediaInfo() {
        MediaMetadata mediaMetadata = new MediaMetadata(mediaType);

        if (!TextUtils.isEmpty(title)) mediaMetadata.putString(MediaMetadata.KEY_TITLE, title);
        if (!TextUtils.isEmpty(subtitle)) mediaMetadata.putString(MediaMetadata.KEY_SUBTITLE, subtitle);

        for (String imageUrl : imageUrls) {
            mediaMetadata.addImage(new WebImage(Uri.parse(imageUrl)));
        }

        return new MediaInfo.Builder(url)
                .setStreamType(streamType)
                .setContentType(contentType)
                .setStreamDuration(streamDuration)
                .setMetadata(mediaMetadata)
                .build();
    }

    public static class Builder {
        private final MediaData mediaData;

        /**
         * Create the MediaData builder
         * @param url String url of media data
         */
        public Builder(String url) {
            mediaData = new MediaData(url);
        }

        /**
         * Sets the stream type. Required.
         * @param streamType One of {@link #STREAM_TYPE_NONE}, {@link #STREAM_TYPE_BUFFERED}, {@link #STREAM_TYPE_LIVE}
         * @return this instance for chain calls
         */
        public Builder setStreamType(@StreamType int streamType) {
            mediaData.setStreamType(streamType);
            return this;
        }

        /**
         * Sets the content type. Required.
         * @param contentType Valid content type, supported by Google Cast
         * @return this instance for chain calls
         */
        public Builder setContentType(String contentType) {
            mediaData.setContentType(contentType);
            return this;
        }

        /**
         * Sets stream duration.
         * @param streamDuration Valid stream duration
         * @return this instance for chain calls
         */
        public Builder setStreamDuration(long streamDuration) {
            mediaData.setStreamDuration(streamDuration);
            return this;
        }

        /**
         * Sets the title.
         * @param title any String
         * @return this instance for chain calls
         */
        public Builder setTitle(String title) {
            mediaData.setTitle(title);
            return this;
        }

        /**
         * Sets the subtitle.
         * @param subtitle any String
         * @return this instance for chain calls
         */
        public Builder setSubtitle(String subtitle) {
            mediaData.setSubtitle(subtitle);
            return this;
        }

        /**
         * Sets the media type.
         * @param mediaType One of {@link #MEDIA_TYPE_GENERIC}, {@link #MEDIA_TYPE_MOVIE}, {@link #MEDIA_TYPE_TV_SHOW}, {@link #MEDIA_TYPE_MUSIC_TRACK},
         * {@link #MEDIA_TYPE_PHOTO}, {@link #MEDIA_TYPE_USER}
         * @return this instance for chain calls
         */
        public Builder setMediaType(@MediaType int mediaType) {
            mediaData.setMediaType(mediaType);
            return this;
        }

        /**
         * Adds the photo url
         * @param photoUrl valid url to image
         * @return this instance for chain calls
         */
        public Builder addPhotoUrl(String photoUrl) {
            mediaData.imageUrls.add(photoUrl);
            return this;
        }

        /**
         * Sets up playing on start
         * @param autoPlay True if the media file should start automatically
         * @return this instance for chain calls
         */
        public Builder setAutoPlay(boolean autoPlay) {
            mediaData.autoPlay = autoPlay;
            return this;
        }

        /**
         * Sets the start position
         * @param position Start position of video in milliseconds
         * @return this instance for chain calls
         */
        public Builder setPosition(long position) {
            mediaData.position = position;
            return this;
        }

        public MediaData build() {
            return this.mediaData;
        }
    }
}
