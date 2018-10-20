package dev.dworks.apps.anexplorer.ui;

import android.content.Context;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.misc.IconUtils;
import dev.dworks.apps.anexplorer.model.RootInfo;
import dev.dworks.apps.anexplorer.setting.SettingsActivity;


public class HomeItem extends FrameLayout {

    private Context mContext;
    private ImageView icon;
    private TextView title;
    private TextView summary;
    private NumberProgressBar progress;
    private int color;
    private int accentColor;
    private ImageButton action;
    private View action_layout;
    private View card_view;
    private int mActionDrawable;

    public HomeItem(Context context) {
        super(context);
        init(context, null);
    }

    public HomeItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public HomeItem(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        mContext = context;
        color = SettingsActivity.getPrimaryColor();
        accentColor = SettingsActivity.getAccentColor();
        LayoutInflater.from(context).inflate(R.layout.item_home, this, true);
        card_view = findViewById(R.id.card_view);
        icon = (ImageView) findViewById(android.R.id.icon);
        title = (TextView) findViewById(android.R.id.title);
        summary = (TextView) findViewById(android.R.id.summary);
        progress = (NumberProgressBar) findViewById(android.R.id.progress);

        action_layout = findViewById(R.id.action_layout);
        action = (ImageButton) findViewById(R.id.action);
    }

    public void setInfo(RootInfo root) {

        icon.setImageDrawable(root.loadDrawerIcon(mContext));
        title.setText(root.title);


        // Show available space if no summary
        String summaryText = root.summary;
        if (TextUtils.isEmpty(summaryText) && root.availableBytes >= 0) {
            summaryText = mContext.getString(R.string.root_available_bytes,
                    Formatter.formatFileSize(mContext, root.availableBytes));
            try {
                Long current = 100 * root.availableBytes / root.totalBytes ;
                progress.setVisibility(View.VISIBLE);
                progress.setMax(100);
                progress.setProgress(100 - current.intValue());
                progress.setColor(color);
            }
            catch (Exception e){
                progress.setVisibility(View.GONE);
            }
        }
        else{
            progress.setVisibility(View.GONE);
        }

        summary.setText(summaryText);
        summary.setVisibility(TextUtils.isEmpty(summaryText) ? View.GONE : View.VISIBLE);
    }

    public void setProgress(int value){
        progress.setProgress(value);
    }

    public int getProgress(){
        return progress.getProgress();
    }

    public void setAction(int drawableId, OnClickListener listener){
        mActionDrawable = drawableId;
        action_layout.setVisibility(View.VISIBLE);
        action.setImageDrawable(IconUtils.applyTint(mContext, mActionDrawable, accentColor));
        action.setOnClickListener(listener);
    }

    public void setCardListener(OnClickListener listener){
        card_view.setOnClickListener(listener);
    }

    public void updateColor(){
        color = SettingsActivity.getPrimaryColor();
        accentColor = SettingsActivity.getAccentColor();
        progress.setColor(color);
        action.setImageDrawable(IconUtils.applyTint(mContext, mActionDrawable, accentColor));
    }
}