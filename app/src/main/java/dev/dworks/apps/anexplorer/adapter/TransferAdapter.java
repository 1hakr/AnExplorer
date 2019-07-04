package dev.dworks.apps.anexplorer.adapter;

import android.content.Context;
import android.content.Intent;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.common.ArrayRecyclerAdapter;
import dev.dworks.apps.anexplorer.service.TransferService;
import dev.dworks.apps.anexplorer.setting.SettingsActivity;
import dev.dworks.apps.anexplorer.transfer.model.TransferStatus;
import dev.dworks.apps.anexplorer.ui.NumberProgressBar;
import dev.dworks.apps.anexplorer.adapter.TransferAdapter.ViewHolder;

import static dev.dworks.apps.anexplorer.transfer.TransferHelper.ACTION_STOP_TRANSFER;
import static dev.dworks.apps.anexplorer.transfer.TransferHelper.EXTRA_TRANSFER;

public class TransferAdapter extends ArrayRecyclerAdapter<TransferStatus, ViewHolder> {

    private Context mContext;
    private OnItemClickListener onItemClickListener;

    public TransferAdapter(Context context) {
        mContext = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transfer, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.setData(position);
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

    @Override
    public long getItemId(int position) {
        return get(position).getId();
    }

    public void update(TransferStatus transferStatus) {
        int index = indexOf(transferStatus);
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

            iconMime = (ImageView) itemView.findViewById(R.id.icon_mime);
            iconMimeBackground = itemView.findViewById(R.id.icon_mime_background);
            mDevice = itemView.findViewById(android.R.id.title);
            mState = itemView.findViewById(R.id.state);
            mProgress = itemView.findViewById(android.R.id.progress);
            mBytes = itemView.findViewById(android.R.id.summary);
            mStop = itemView.findViewById(R.id.action);

            // This never changes
            // mStop.setIcon(R.drawable.ic_action_stop);
            mStop.setText(R.string.adapter_transfer_stop);
        }

        public void setData(int position) {

            TransferStatus transferStatus = get(position);
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
            mDevice.setText(transferStatus.getRemoteDeviceName() + transferStatus.getId());
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
}