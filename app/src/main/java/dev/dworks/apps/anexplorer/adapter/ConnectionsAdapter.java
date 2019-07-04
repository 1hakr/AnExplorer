package dev.dworks.apps.anexplorer.adapter;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.cloud.CloudConnection;
import dev.dworks.apps.anexplorer.common.CursorRecyclerViewAdapter;
import dev.dworks.apps.anexplorer.misc.IconColorUtils;
import dev.dworks.apps.anexplorer.misc.IconUtils;
import dev.dworks.apps.anexplorer.network.NetworkConnection;

import static dev.dworks.apps.anexplorer.DocumentsApplication.isSpecialDevice;
import static dev.dworks.apps.anexplorer.provider.CloudStorageProvider.TYPE_CLOUD;

public class ConnectionsAdapter extends CursorRecyclerViewAdapter<ConnectionsAdapter.ViewHolder> {

    private Context mContext;
    private OnItemClickListener onItemClickListener;

    public ConnectionsAdapter(Context context, Cursor cursor){
        super(context, cursor);
        mContext = context;
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Cursor cursor) {
        viewHolder.setData(cursor);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_connection_list, parent, false);
        return new ViewHolder(itemView);
    }

    public void setOnItemClickListener(OnItemClickListener listener){
        onItemClickListener = listener;
    }

    public OnItemClickListener getOnItemClickListener(){
        return onItemClickListener;
    }

    public interface OnItemClickListener {
        void onItemClick(ViewHolder item, View view, int position);
        void onItemLongClick(ViewHolder item, View view, int position);
        void onItemViewClick(ViewHolder item, View view, int position);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView iconMime;
        private final View iconMimeBackground;
        private final TextView summary;
        private final TextView title;
        private final View popupButton;

        public ViewHolder(View v) {
            super(v);
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(null != onItemClickListener) {
                        onItemClickListener.onItemClick(ViewHolder.this, v, getLayoutPosition());
                    }
                }
            });
            v.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if(null != onItemClickListener) {
                        onItemClickListener.onItemLongClick(ViewHolder.this, v, getLayoutPosition());
                    }
                    return false;
                }
            });

            iconMime = (ImageView) v.findViewById(R.id.icon_mime);
            iconMimeBackground = v.findViewById(R.id.icon_mime_background);
            title = (TextView) v.findViewById(android.R.id.title);
            summary = (TextView) v.findViewById(android.R.id.summary);
            popupButton = v.findViewById(R.id.button_popup);
            popupButton.setVisibility(isSpecialDevice() ? View.INVISIBLE : View.VISIBLE);
            popupButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(null != onItemClickListener) {
                        onItemClickListener.onItemViewClick(ViewHolder.this, popupButton, getLayoutPosition());
                    }
                }
            });
        }

        public void setData(Cursor cursor){
            NetworkConnection networkConnection = NetworkConnection.fromConnectionsCursor(cursor);
            if(networkConnection.type.startsWith(TYPE_CLOUD)){
                title.setText(CloudConnection.getTypeName(networkConnection.type));
                summary.setText(networkConnection.username);
                iconMimeBackground.setVisibility(View.VISIBLE);
                iconMimeBackground.setBackgroundColor(
                        IconColorUtils.loadCloudColor(mContext, networkConnection.getType()));
                iconMime.setImageDrawable(IconUtils.loadCloudIcon(mContext, networkConnection.type));
            } else {
                title.setText(networkConnection.getName());
                summary.setText(networkConnection.getSummary());
                iconMimeBackground.setVisibility(View.VISIBLE);
                iconMimeBackground.setBackgroundColor(
                        IconColorUtils.loadSchmeColor(mContext, networkConnection.getType()));
                iconMime.setImageDrawable(IconUtils.loadSchemeIcon(mContext, networkConnection.type));
            }
        }
    }
}