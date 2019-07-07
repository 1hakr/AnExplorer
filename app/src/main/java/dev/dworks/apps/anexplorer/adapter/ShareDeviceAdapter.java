package dev.dworks.apps.anexplorer.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.common.ArrayListAdapter;
import dev.dworks.apps.anexplorer.transfer.model.Device;

import static dev.dworks.apps.anexplorer.DocumentsApplication.isTelevision;

public class ShareDeviceAdapter extends ArrayListAdapter<Device, ArrayListAdapter.ViewHolder> {

    private static final String TAG = "ShareDeviceAdapter";

    public ShareDeviceAdapter(@NonNull Context context) {
        super(context, R.layout.item_share_device);
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_share_device, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ArrayListAdapter.ViewHolder viewHolder, int position) {
        viewHolder.setData(position);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getName().hashCode();
    }

    public void update(Device device) {
        int index = getPosition(device);
        if (index < 0) {
            add(device);
        } else {
            add(index, device);
        }
    }

    public class ViewHolder extends ArrayListAdapter.ViewHolder {
        private final TextView name;
        private final TextView address;
        private final ImageView icon;
        private int mPosition;

        public ViewHolder(View itemView) {
            super(itemView);
            if(isTelevision()) {
                itemView.setFocusable(true);
            }
            icon = (ImageView) itemView.findViewById(android.R.id.icon);
            name = (TextView) itemView.findViewById(android.R.id.title);
            address = (TextView) itemView.findViewById(android.R.id.summary);
        }

        @Override
        public void setData(int position) {
            mPosition = position;
            Device device = getItem(position);
            name.setText(device.getName());
            address.setText(device.getHost().getHostAddress());
        }
    }
}