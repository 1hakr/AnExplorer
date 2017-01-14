package dev.dworks.apps.anexplorer.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.model.RootInfo;


public class HomeItemSmall extends FrameLayout {
    private Context mContext;
    private ImageView icon;
    private TextView title;
    private View card_view;

    public HomeItemSmall(Context context) {
        super(context);
        init(context, null);
    }

    public HomeItemSmall(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public HomeItemSmall(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        mContext = context;
        LayoutInflater.from(context).inflate(R.layout.item_home_small, this, true);
        card_view = findViewById(R.id.card_view);
        icon = (ImageView) findViewById(android.R.id.icon);
        title = (TextView) findViewById(android.R.id.title);
    }

    public void setInfo(RootInfo root) {
        icon.setImageDrawable(root.loadDrawerIcon(mContext));
        title.setText(root.title);
    }

    public void setCardListener(OnClickListener listener){
        card_view.setOnClickListener(listener);
    }
}