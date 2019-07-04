package dev.dworks.apps.anexplorer.adapter;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaQueueItem;
import com.google.android.gms.cast.framework.media.MediaQueue;
import com.google.android.gms.cast.framework.media.MediaQueueRecyclerViewAdapter;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.cast.CastUtils;
import dev.dworks.apps.anexplorer.common.RecyclerFragment.OnItemClickListener;
import dev.dworks.apps.anexplorer.misc.IconHelper;
import dev.dworks.apps.anexplorer.misc.IconUtils;
import dev.dworks.apps.anexplorer.model.DocumentsContract;

import static dev.dworks.apps.anexplorer.DocumentsApplication.isSpecialDevice;

public class QueueAdapter extends MediaQueueRecyclerViewAdapter<QueueAdapter.QueueHolder> {

    private final IconHelper mIconHelper;
    private OnItemClickListener onItemClickListener;

    public QueueAdapter(MediaQueue mediaQueue, IconHelper iconHelper) {
        super(mediaQueue);
        mIconHelper = iconHelper;
    }

    @Override
    public QueueHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        final View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_queue, parent, false);
        return new QueueHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final QueueHolder holder, final int position) {
        final MediaQueueItem item = getItem(position);
        holder.bind(item);
    }

    public void setOnItemClickListener(OnItemClickListener listener){
        onItemClickListener = listener;
    }

    public OnItemClickListener getOnItemClickListener(){
        return onItemClickListener;
    }

    public class QueueHolder extends RecyclerView.ViewHolder {

        protected final ImageView iconMime;
        protected final ImageView iconThumb;
        protected final View iconMimeBackground;
        protected final TextView title;
        protected final TextView summary;
        public MediaQueueItem mItem;

        QueueHolder(final View view) {
            super(view);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(null != onItemClickListener) {
                        onItemClickListener.onItemClick(QueueHolder.this, v, getLayoutPosition());
                    }
                }
            });
            view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if(null != onItemClickListener) {
                        onItemClickListener.onItemLongClick(QueueHolder.this, v, getLayoutPosition());
                    }
                    return false;
                }
            });
            title = view.findViewById(android.R.id.title);
            summary = view.findViewById(android.R.id.summary);
            iconMime = view.findViewById(R.id.icon_mime);
            iconThumb = view.findViewById(R.id.icon_thumb);
            iconMimeBackground = view.findViewById(R.id.icon_mime_background);
            final View popupButton = view.findViewById(R.id.button_popup);
            popupButton.setVisibility(isSpecialDevice() ? View.INVISIBLE : View.VISIBLE);
            popupButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(null != onItemClickListener) {
                        onItemClickListener.onItemViewClick(QueueHolder.this, popupButton, getLayoutPosition());
                    }
                }
            });
        }

        public void bind(MediaQueueItem item) {
            mItem = item;
            if (item == null) {
                return;
            }
            Context context = iconMimeBackground.getContext();
            MediaMetadata metaData = item.getMedia().getMetadata();
            title.setText(metaData.getString(MediaMetadata.KEY_TITLE));
            summary.setText(metaData.getString(MediaMetadata.KEY_ALBUM_TITLE));
            int color = ContextCompat.getColor(context, R.color.item_doc_audio);
            mIconHelper.stopLoading(iconThumb);
            String mimeType = CastUtils.getMimeType(metaData.getMediaType());
            if (!metaData.getImages().isEmpty()) {
                String url = metaData.getImages().get(0).getUrl().toString();
                final Uri thumbnailUri = Uri.parse(url);
                final String authority = thumbnailUri.getQueryParameter("authority");
                final String docid = thumbnailUri.getQueryParameter("docid");
                final Uri uri = DocumentsContract.buildDocumentUri(authority, docid);
                mIconHelper.load(uri, mimeType, iconThumb, iconMime, iconMimeBackground);
            } else {
                iconMime.setImageDrawable(IconUtils.loadMimeIcon(context, mimeType));
                iconMime.setVisibility(View.VISIBLE);
                iconMimeBackground.setBackgroundColor(color);
                iconMimeBackground.setVisibility(View.VISIBLE);
            }
        }
    }

}