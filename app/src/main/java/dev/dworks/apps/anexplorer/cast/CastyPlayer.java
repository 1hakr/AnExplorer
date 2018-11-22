package dev.dworks.apps.anexplorer.cast;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaLoadOptions;
import com.google.android.gms.cast.MediaQueueItem;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

public class CastyPlayer {
    private RemoteMediaClient remoteMediaClient;
    private OnMediaLoadedListener onMediaLoadedListener;

    //Needed for NoOp instance
    CastyPlayer() {
        //no-op
    }

    CastyPlayer(OnMediaLoadedListener onMediaLoadedListener) {
        this.onMediaLoadedListener = onMediaLoadedListener;
    }

    void setRemoteMediaClient(RemoteMediaClient remoteMediaClient) {
        this.remoteMediaClient = remoteMediaClient;
    }

    public RemoteMediaClient getRemoteMediaClient() {
        return remoteMediaClient;
    }

    /**
     * Plays the current media file if it is paused
     */
    public void play() {
        if (isPaused()) remoteMediaClient.play();
    }

    /**
     * Pauses the current media file if it is playing
     */
    public void pause() {
        if (isPlaying()) remoteMediaClient.pause();
    }

    /**
     * Seeks the current media file
     *
     * @param time the number of milliseconds to seek by
     */
    public void seek(long time) {
        if (remoteMediaClient != null) remoteMediaClient.seek(time);
    }

    /**
     * Tries to play or pause the current media file, depending of the current state
     */
    public void togglePlayPause() {
        if (remoteMediaClient != null) {
            if (remoteMediaClient.isPlaying()) {
                remoteMediaClient.pause();
            } else if (remoteMediaClient.isPaused()) {
                remoteMediaClient.play();
            }
        }
    }

    /**
     * Checks if the media file is playing
     *
     * @return true if the media file is playing, false otherwise
     */
    public boolean isPlaying() {
        return remoteMediaClient != null && remoteMediaClient.isPlaying();
    }

    /**
     * Checks if the media file is paused
     *
     * @return true if the media file is paused, false otherwise
     */
    public boolean isPaused() {
        return remoteMediaClient != null && remoteMediaClient.isPaused();
    }

    /**
     * Checks if the media file is buffering
     *
     * @return true if the media file is buffering, false otherwise
     */
    public boolean isBuffering() {
        return remoteMediaClient != null && remoteMediaClient.isBuffering();
    }

    /**
     * Tries to load the media file and play it in the {@link ExpandedControlsActivity}
     *
     * @param mediaData Information about the media
     * @return true if attempt was successful, false otherwise
     * @see MediaData
     */
    @MainThread
    public boolean loadMediaAndPlay(@NonNull MediaData mediaData) {
        return loadMediaAndPlay(mediaData.createMediaInfo(), mediaData.autoPlay, mediaData.position);
    }

    /**
     * Tries to load the media file and play it in the {@link ExpandedControlsActivity}
     *
     * @param mediaInfo Information about the media
     * @return true if attempt was successful, false otherwise
     * @see MediaInfo
     */
    @MainThread
    public boolean loadMediaAndPlay(@NonNull MediaInfo mediaInfo) {
        return loadMediaAndPlay(mediaInfo, true, 0);
    }

    /**
     * Tries to load the media file and play it in the {@link ExpandedControlsActivity}
     *
     * @param mediaInfo Information about the media
     * @param autoPlay True if the media file should start automatically
     * @param position Start position of video in milliseconds
     * @return true if attempt was successful, false otherwise
     * @see MediaInfo
     */
    @MainThread
    public boolean loadMediaAndPlay(@NonNull MediaInfo mediaInfo, boolean autoPlay, long position) {
        return playMediaBaseMethod(mediaInfo, autoPlay, position, false);
    }

    /**
     * Tries to load the media file and play in background
     *
     * @param mediaData Information about the media
     * @return true if attempt was successful, false otherwise
     * @see MediaData
     */
    @MainThread
    public boolean loadMediaAndPlayInBackground(@NonNull MediaData mediaData) {
        return loadMediaAndPlayInBackground(mediaData.createMediaInfo(), mediaData.autoPlay, mediaData.position);
    }

    /**
     * Tries to load the media file and play in background
     *
     * @param mediaInfo Information about the media
     * @return true if attempt was successful, false otherwise
     * @see MediaInfo
     */
    @MainThread
    public boolean loadMediaAndPlayInBackground(@NonNull MediaInfo mediaInfo) {
        return loadMediaAndPlayInBackground(mediaInfo, true, 0);
    }

    /**
     * Tries to load the media file and play in background
     *
     * @param mediaInfo Information about the media
     * @param autoPlay True if the media file should start automatically
     * @param position Start position of video in milliseconds
     * @return true if attempt was successful, false otherwise
     * @see MediaInfo
     */
    @MainThread
    public boolean loadMediaAndPlayInBackground(@NonNull MediaInfo mediaInfo, boolean autoPlay, long position) {
        return playMediaBaseMethod(mediaInfo, autoPlay, position, true);
    }

    private boolean playMediaBaseMethod(MediaInfo mediaInfo, boolean autoPlay, long position, boolean inBackground) {
        if (remoteMediaClient == null) {
            return false;
        }
        if (!inBackground) {
            remoteMediaClient.registerCallback(createRemoteMediaClientListener());
        }
        MediaLoadOptions loadOptions = new MediaLoadOptions.Builder()
                .setAutoplay(autoPlay)
                .setPlayPosition(position)
                .build();
        remoteMediaClient.load(mediaInfo, loadOptions);
        return true;
    }

    @MainThread
    public boolean loadMediaInQueueAndPlay(MediaQueueItem queueItem){
        return playMediaQueueBasedMethod(queueItem, false);
    }

    @MainThread
    public boolean loadMediaInQueueAndPlayInBackground(MediaQueueItem queueItem){
        return playMediaQueueBasedMethod(queueItem, true);
    }

    private boolean playMediaQueueBasedMethod(MediaQueueItem queueItem, boolean inBackground){
        if (remoteMediaClient == null) {
            return false;
        }

        MediaQueueItem currentItem = remoteMediaClient.getCurrentItem();
        int currentId = 0;
        if(null != currentItem) {
            currentId = currentItem.getItemId();
        }
        remoteMediaClient.queueInsertAndPlayItem(queueItem, currentId, null);
        return true;
    }

    private RemoteMediaClient.Callback createRemoteMediaClientListener() {
        return new RemoteMediaClient.Callback() {
            @Override
            public void onStatusUpdated() {
                onMediaLoadedListener.onMediaLoaded();
                remoteMediaClient.unregisterCallback(this);
            }

            @Override
            public void onMetadataUpdated() {
                //no-op
            }

            @Override
            public void onQueueStatusUpdated() {
                onMediaLoadedListener.onMediaLoaded();
                remoteMediaClient.unregisterCallback(this);
            }

            @Override
            public void onPreloadStatusUpdated() {
                //no-op
            }

            @Override
            public void onSendingRemoteMediaRequest() {
                //no-op
            }

            @Override
            public void onAdBreakStatusUpdated() {
                //no-op
            }
        };
    }

    interface OnMediaLoadedListener {
        void onMediaLoaded();
    }
}
