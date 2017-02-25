package dev.dworks.apps.anexplorer.adapter;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.model.RootInfo;
import dev.dworks.apps.anexplorer.setting.SettingsActivity;
import dev.dworks.apps.anexplorer.ui.CircleImage;

public class ShortcutsAdapter extends RecyclerView.Adapter<ShortcutsAdapter.ViewHolder> {

    private final int mDefaultColor;
    private Context mContext;
    private OnItemClickListener onItemClickListener;
    private ArrayList<RootInfo> mData;

    public ShortcutsAdapter(Context context, ArrayList<RootInfo> data){
        mContext = context;
        mData = data;
        mDefaultColor = SettingsActivity.getPrimaryColor();
    }

    public void setData(ArrayList<RootInfo> data) {
        if (data == mData) {
            return;
        }

        mData = data;
        if (mData != null) {
            notifyDataSetChanged();
        } else {
            notifyDataSetChanged();
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.setData(position);
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_shortcuts, parent, false);
        return new ViewHolder(itemView);
    }

    public void setOnItemClickListener(OnItemClickListener listener){
        onItemClickListener = listener;
    }

    public OnItemClickListener getOnItemClickListener(){
        return onItemClickListener;
    }

    public interface OnItemClickListener{
        void onItemClick(ViewHolder item, int position);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final CircleImage iconBackground;
        private final ImageView icon;
        private final TextView title;
        private RootInfo mRoot;
        private int mPosition;

        public ViewHolder(View v) {
            super(v);
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onItemClickListener.onItemClick(ViewHolder.this, getLayoutPosition());
                }
            });
            icon = (ImageView) v.findViewById(android.R.id.icon);
            iconBackground = (CircleImage) v.findViewById(R.id.icon_background);
            title = (TextView) v.findViewById(android.R.id.title);
        }

        public void setData(int position){
            mPosition = position;
            mRoot = mData.get(position);
            if(null != mRoot) {
                iconBackground.setColor(ContextCompat.getColor(mContext, mRoot.derivedColor));
                icon.setImageDrawable(mRoot.loadShortcutIcon(mContext));
                title.setText(mRoot.title);
            }
        }
    }

    public RootInfo getItem(int position){
        return mData.get(position);
    }
}