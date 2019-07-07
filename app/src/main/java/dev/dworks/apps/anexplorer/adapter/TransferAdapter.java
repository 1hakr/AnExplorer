package dev.dworks.apps.anexplorer.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import dev.dworks.apps.anexplorer.DocumentsApplication;
import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.common.ArrayRecyclerAdapter;
import dev.dworks.apps.anexplorer.misc.IconUtils;
import dev.dworks.apps.anexplorer.misc.Utils;
import dev.dworks.apps.anexplorer.service.TransferService;
import dev.dworks.apps.anexplorer.setting.SettingsActivity;
import dev.dworks.apps.anexplorer.transfer.TransferHelper;
import dev.dworks.apps.anexplorer.transfer.model.TransferStatus;
import dev.dworks.apps.anexplorer.ui.CircleImage;
import dev.dworks.apps.anexplorer.ui.NumberProgressBar;

import static dev.dworks.apps.anexplorer.DocumentsApplication.isSpecialDevice;
import static dev.dworks.apps.anexplorer.transfer.TransferHelper.ACTION_STOP_TRANSFER;
import static dev.dworks.apps.anexplorer.transfer.TransferHelper.EXTRA_TRANSFER;

public class TransferAdapter extends ArrayRecyclerAdapter<TransferStatus, ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;
    private Context mContext;
    private OnItemClickListener onItemClickListener;

    public TransferAdapter(Context context) {
        mContext = context;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        if (viewType == TYPE_HEADER) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transfer_header, parent, false);
            HeaderViewHolder viewHolder = new HeaderViewHolder(view);
            return  viewHolder;
        }
        view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transfer, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
         if (holder instanceof ViewHolder) {
            ViewHolder viewHolder = (ViewHolder) holder;
            viewHolder.setData(position);
        } else if (holder instanceof HeaderViewHolder) {
             HeaderViewHolder viewHolder = (HeaderViewHolder) holder;
            viewHolder.setData(position);
        }
    }

    @Override
    public long getItemId(int position) {
        if (position == 0) {
            return TYPE_HEADER;
        }
        return getItem(position).getId();
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return TYPE_HEADER;
        }
        return TYPE_ITEM;
    }

    @Override
    public int getItemCount() {
        return super.getItemCount() + 1;
    }

    @Override
    public TransferStatus getItem(int position) {
        return super.getItem(position - 1);
    }

    public void setOnItemClickListener(OnItemClickListener listener){
        onItemClickListener = listener;
    }

    public OnItemClickListener getOnItemClickListener(){
        return onItemClickListener;
    }

    public interface OnItemClickListener {
        void onItemClick(RecyclerView.ViewHolder item, View view, int position);
        void onItemLongClick(RecyclerView.ViewHolder item, View view, int position);
        void onItemViewClick(RecyclerView.ViewHolder item, View view, int position);
    }

    public void update(TransferStatus transferStatus) {
        int index = indexOf(transferStatus);
        Log.i("yoyi", String.valueOf(index));
        if (index < 0) {
            add(transferStatus);
        } else {
            set(index, transferStatus);
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private final ImageView iconMime;
        private final View iconMimeBackground;
        private TextView mDevice;
        private TextView mState;
        private TextView mBytes;
        private TextView mStop;
        private NumberProgressBar mProgress;

        public ViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(null != onItemClickListener) {
                        onItemClickListener.onItemClick(ViewHolder.this, v, getLayoutPosition());
                    }
                }
            });
            iconMime = (ImageView) itemView.findViewById(R.id.icon_mime);
            iconMimeBackground = itemView.findViewById(R.id.icon_mime_background);
            mDevice = itemView.findViewById(android.R.id.title);
            mState = itemView.findViewById(R.id.state);
            mProgress = itemView.findViewById(android.R.id.progress);
            mBytes = itemView.findViewById(android.R.id.summary);
            mStop = itemView.findViewById(R.id.action);

            // mStop.setIcon(R.drawable.ic_action_stop);
            mStop.setText(R.string.adapter_transfer_stop);
        }

        public void setData(int position) {
            TransferStatus transferStatus = getItem(position);
            // Generate transfer byte string
            final CharSequence bytesText;
            if (transferStatus.getBytesTotal() == 0) {
                bytesText = mContext.getString(R.string.adapter_transfer_unknown);
            } else {
                bytesText = mContext.getString(
                        R.string.adapter_transfer_bytes,
                        Formatter.formatShortFileSize(mContext, transferStatus.getBytesTransferred()),
                        Formatter.formatShortFileSize(mContext, transferStatus.getBytesTotal())
                );
            }

            // Set the attributes
            iconMime.setImageResource(R.drawable.ic_stat_download);
            iconMimeBackground.setVisibility(View.VISIBLE);
            iconMimeBackground.setBackgroundColor(ContextCompat.getColor(mContext, R.color.item_transfer));
            mDevice.setText(transferStatus.getRemoteDeviceName());
            mProgress.setMax(0);
            mProgress.setProgress(transferStatus.getProgress());
            mProgress.setColor(SettingsActivity.getAccentColor());
            mBytes.setText(bytesText);

            // Display the correct state string in the correct style
            setState(transferStatus);
        }

        private void setState(final TransferStatus transferStatus) {
            switch (transferStatus.getState()) {
                case Connecting:
                case Transferring:
                    if (transferStatus.getState() == TransferStatus.State.Connecting) {
                        mState.setText(R.string.adapter_transfer_connecting);
                    } else {
                        mState.setText(mContext.getString(R.string.adapter_transfer_transferring,
                                transferStatus.getProgress()));
                    }
                    mState.setTextColor(ContextCompat.getColor(mContext, android.R.color.darker_gray));
                    mStop.setVisibility(View.VISIBLE);
                    mStop.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent stopIntent = new Intent(mContext, TransferService.class)
                                    .setAction(ACTION_STOP_TRANSFER)
                                    .putExtra(EXTRA_TRANSFER, transferStatus.getId());
                            mContext.startService(stopIntent);
                        }
                    });
                    break;
                case Succeeded:
                    mState.setText(R.string.adapter_transfer_succeeded);
                    mState.setTextColor(ContextCompat.getColor(mContext, R.color.md_teal_500));
                    mStop.setVisibility(View.INVISIBLE);
                    break;
                case Failed:
                    mState.setText(mContext.getString(R.string.adapter_transfer_failed,
                            transferStatus.getError()));
                    mState.setTextColor(ContextCompat.getColor(mContext, R.color.md_red_500));
                    mStop.setVisibility(View.INVISIBLE);
                    break;
            }
        }
    }

    public class HeaderViewHolder extends RecyclerView.ViewHolder {

        private TextView status;
        private Button action;

        public HeaderViewHolder(View itemView) {
            super(itemView);
            int color = Utils.getStatusBarColor(SettingsActivity.getPrimaryColor());
            View background = itemView.findViewById(R.id.background);
            if (!isSpecialDevice()) {
                background.setBackgroundColor(color);
            }
            CircleImage iconBackground = itemView.findViewById(R.id.icon_mime_background);
            iconBackground.setBackgroundColor(SettingsActivity.getPrimaryColor());
            ImageView icon = itemView.findViewById(android.R.id.icon);
            icon.setImageDrawable(IconUtils.applyTint(itemView.getContext(), R.drawable.ic_root_transfer, Color.WHITE));
            status = (TextView) itemView.findViewById(R.id.status);
            action = (Button) itemView.findViewById(R.id.action);
            action.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(null != onItemClickListener){
                        onItemClickListener.onItemViewClick(HeaderViewHolder.this, action, getLayoutPosition());
                    }
                }
            });

        }

        public void setData(int position) {
            setStatus(TransferHelper.isServerRunning(DocumentsApplication.getInstance().getApplicationContext()));
        }

        public void setStatus(boolean running){
            Context context = status.getContext();
            if(running){
                status.setTextColor(SettingsActivity.getAccentColor());
                status.setText(context.getString(R.string.ftp_status_running));
                action.setText(R.string.stop_ftp);
            } else {
                status.setTextColor(ContextCompat.getColor(context, R.color.item_doc_grid_overlay_disabled));
                status.setText(context.getString(R.string.ftp_status_not_running));
                action.setText(R.string.start_ftp);
            }
        }
    }
}