package dev.dworks.apps.anexplorer.adapter;

import android.app.Activity;
import android.database.Cursor;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.misc.CrashReportingManager;
import dev.dworks.apps.anexplorer.misc.IconHelper;
import dev.dworks.apps.anexplorer.misc.IconUtils;
import dev.dworks.apps.anexplorer.misc.Utils;
import dev.dworks.apps.anexplorer.model.DocumentInfo;
import dev.dworks.apps.anexplorer.model.RootInfo;
import dev.dworks.apps.anexplorer.setting.SettingsActivity;
import dev.dworks.apps.anexplorer.ui.CircleImage;
import dev.dworks.apps.anexplorer.ui.NumberProgressBar;

public class HomeAdapter extends RecyclerView.Adapter<HomeAdapter.ViewHolder> {
    public static final int TYPE_MAIN = 1;
    public static final int TYPE_SHORTCUT = 2;
    public static final int TYPE_RECENT = 3;

    private final int mDefaultColor;
    private Activity mContext;
    private OnItemClickListener onItemClickListener;
    private ArrayList<CommonInfo> mData = new ArrayList<>();
    private Cursor recentCursor;
    private final IconHelper mIconHelper;

    public HomeAdapter(Activity context, ArrayList<CommonInfo> data, IconHelper iconHelper){
        mContext = context;
        mData = data;
        mDefaultColor = SettingsActivity.getPrimaryColor();
        mIconHelper = iconHelper;
    }

    public void setData(ArrayList<CommonInfo> data) {
        if (data == mData) {
            return;
        }

        mData = data;
        notifyDataSetChanged();
    }

    public void setRecentData(Cursor cursor) {
        recentCursor = cursor;
        notifyDataSetChanged();
    }

    public int getRecentSize() {
        return recentCursor != null && recentCursor.getCount() != 0 ? 1 : 0;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.setData(position);
    }

    @Override
    public int getItemCount() {
        return mData.size() + getRecentSize();
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).type;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case TYPE_MAIN: {
                View itemView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_home, parent, false);
                return new MainViewHolder(itemView);
            }
            case TYPE_SHORTCUT: {
                View itemView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_shortcuts, parent, false);
                return new ShortcutViewHolder(itemView);
            }
            case TYPE_RECENT: {
                View itemView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_gallery, parent, false);
                return new GalleryViewHolder(itemView);
            }
        }
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_shortcuts, parent, false);
        return new ShortcutViewHolder(itemView);
    }

    public void setOnItemClickListener(OnItemClickListener listener){
        onItemClickListener = listener;
    }

    public OnItemClickListener getOnItemClickListener(){
        return onItemClickListener;
    }

    public interface OnItemClickListener{
        void onItemClick(ViewHolder item, View view, int position);
        void onItemLongClick(ViewHolder item, View view, int position);
        void onItemViewClick(ViewHolder item, View view, int position);
    }

    public abstract class ViewHolder extends RecyclerView.ViewHolder {
        protected final CircleImage iconBackground;
        protected final ImageView icon;
        protected final TextView title;
        protected TextView summary;
        protected NumberProgressBar progress;
        protected ImageButton action;
        protected View action_layout;
        protected View card_view;
        protected final ImageView iconMime;
        protected final ImageView iconThumb;
        protected final View iconMimeBackground;
        public CommonInfo commonInfo;

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
            icon = (ImageView) v.findViewById(android.R.id.icon);
            iconBackground = (CircleImage) v.findViewById(R.id.icon_background);
            title = (TextView) v.findViewById(android.R.id.title);

            card_view = v.findViewById(R.id.card_view);
            summary = (TextView) v.findViewById(android.R.id.summary);
            progress = (NumberProgressBar) v.findViewById(android.R.id.progress);
            action_layout = v.findViewById(R.id.action_layout);
            action = (ImageButton) v.findViewById(R.id.action);

            iconMime = (ImageView) v.findViewById(R.id.icon_mime);
            iconThumb = (ImageView) v.findViewById(R.id.icon_thumb);
            iconMimeBackground = v.findViewById(R.id.icon_mime_background);
        }

        public abstract void setData(int position);
    }

    public class MainViewHolder extends ViewHolder {
        private final int accentColor;
        private final int color;

        public MainViewHolder(View v) {
            super(v);
            action.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(null != onItemClickListener) {
                        onItemClickListener.onItemViewClick(MainViewHolder.this, action, getLayoutPosition());
                    }
                }
            });
            accentColor = SettingsActivity.getAccentColor();
            color = SettingsActivity.getPrimaryColor();
        }

        @Override
        public void setData(int position){
            commonInfo = getItem(position);
            icon.setImageDrawable(commonInfo.rootInfo.loadDrawerIcon(mContext));
            title.setText(commonInfo.rootInfo.title);
            int drawableId = -1;
            if (commonInfo.rootInfo.isAppProcess()){
                drawableId = R.drawable.ic_clean;
            } else if (commonInfo.rootInfo.isStorage() && !commonInfo.rootInfo.isSecondaryStorage()) {
                drawableId = R.drawable.ic_analyze;
            }
            if(drawableId != -1) {
                action.setImageDrawable(IconUtils.applyTint(mContext, drawableId, accentColor));
                action_layout.setVisibility(View.VISIBLE);
            } else {
                action.setImageDrawable(null);
                action_layout.setVisibility(View.GONE);
            }

            if (commonInfo.rootInfo.availableBytes >= 0) {
                try {
                    Long current = 100 * commonInfo.rootInfo.availableBytes / commonInfo.rootInfo.totalBytes ;
                    progress.setVisibility(View.VISIBLE);
                    progress.setMax(100);
                    progress.setProgress(100 - current.intValue());
                    progress.setColor(color);
                    animateProgress(progress, commonInfo.rootInfo);
                }
                catch (Exception e){
                    progress.setVisibility(View.GONE);
                }
            }
            else{
                progress.setVisibility(View.GONE);
            }
        }
    }

    public class ShortcutViewHolder extends ViewHolder {

        public ShortcutViewHolder(View v) {
            super(v);
        }

        @Override
        public void setData(int position){
            commonInfo = getItem(position);
            if(null == commonInfo || null == commonInfo.rootInfo){
                return;
            }
            iconBackground.setColor(ContextCompat.getColor(mContext, commonInfo.rootInfo.derivedColor));
            icon.setImageDrawable(commonInfo.rootInfo.loadShortcutIcon(mContext));
            title.setText(commonInfo.rootInfo.title);
        }
    }

    public class GalleryViewHolder extends ViewHolder {
        private final RecyclerView recyclerview;
        private TextView recents;
        private RecentsAdapter adapter;

        public GalleryViewHolder(View v) {
            super(v);

            recyclerview = (RecyclerView) v.findViewById(R.id.recyclerview);
            recents = v.findViewById(R.id.recents);
            recents.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(null != onItemClickListener) {
                        onItemClickListener.onItemViewClick(GalleryViewHolder.this, recents, getLayoutPosition());
                    }
                }
            });
        }

        @Override
        public void setData(int position){
            commonInfo = CommonInfo.from(recentCursor);
            adapter = new RecentsAdapter(mContext, recentCursor, mIconHelper);
            adapter.setOnItemClickListener(new RecentsAdapter.OnItemClickListener() {
                @Override
                public void onItemClick(RecentsAdapter.ViewHolder item, int position) {
                    if(null != onItemClickListener) {
                        onItemClickListener.onItemClick(GalleryViewHolder.this, recyclerview, position);
                    }
                }
            });
            recyclerview.setAdapter(adapter);
        }

        public DocumentInfo getItem(int position){
           return DocumentInfo.fromDirectoryCursor(adapter.getItem(position));
        }
    }

    public CommonInfo getItem(int position){
        if(position < mData.size()){
            return mData.get(position);
        } else {
            return CommonInfo.from(recentCursor);
        }
    }

    private void animateProgress(final NumberProgressBar item, RootInfo root){
        try {
            final double percent = (((root.totalBytes - root.availableBytes) / (double) root.totalBytes) * 100);
            final Timer timer = new Timer();
            item.setProgress(0);
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if(Utils.isActivityAlive(mContext)){
                        mContext.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (item.getProgress() >= (int) percent) {
                                    timer.cancel();
                                } else {
                                    item.setProgress(item.getProgress() + 1);
                                }
                            }
                        });
                    }
                }
            }, 50, 20);
        }
        catch (Exception e){
            item.setVisibility(View.GONE);
            CrashReportingManager.logException(e);
        }
    }
}