package dev.dworks.apps.anexplorer.directory;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Checkable;
import android.widget.ImageView;
import android.widget.TextView;

import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.common.RecyclerFragment.RecyclerItemClickListener.OnItemClickListener;
import dev.dworks.apps.anexplorer.misc.IconHelper;
import dev.dworks.apps.anexplorer.model.DocumentInfo;

import static dev.dworks.apps.anexplorer.DocumentsApplication.isSpecialDevice;
import static dev.dworks.apps.anexplorer.DocumentsApplication.isTelevision;

public abstract class DocumentHolder extends BaseHolder implements View.OnClickListener{

    protected final Context mContext;

    protected DocumentsAdapter.Environment mEnv;
    protected IconHelper mIconHelper;
    protected DocumentInfo mDoc;

    protected final ImageView iconMime;
    protected final ImageView iconThumb;
    protected final View iconMimeBackground;
    protected final TextView title;
    protected final ImageView icon1;
    protected final ImageView icon2;
    protected final TextView summary;
    protected final TextView date;
    protected final TextView size;
    protected final View popupButton;
    protected final View line1;
    protected final View line2;
    protected final View iconView;

    public DocumentHolder(Context context, ViewGroup parent, int layout,
                          OnItemClickListener onItemClickListener,
                              DocumentsAdapter.Environment environment) {
        this(context, inflateLayout(context, parent, layout));

        mEnv = environment;
        mIconHelper = mEnv.getIconHelper();
        multiChoiceHelper = mEnv.getMultiChoiceHelper();
        mDoc = new DocumentInfo();

        clickListener = onItemClickListener;
    }

    public DocumentHolder(Context context, View item) {
        super(item);
        mContext = context;

        iconMime = (ImageView) itemView.findViewById(R.id.icon_mime);
        iconThumb = (ImageView) itemView.findViewById(R.id.icon_thumb);
        iconMimeBackground = itemView.findViewById(R.id.icon_mime_background);
        title = (TextView) itemView.findViewById(android.R.id.title);
        icon1 = (ImageView) itemView.findViewById(android.R.id.icon1);
        icon2 = (ImageView) itemView.findViewById(android.R.id.icon2);
        summary = (TextView) itemView.findViewById(android.R.id.summary);
        date = (TextView) itemView.findViewById(R.id.date);
        size = (TextView) itemView.findViewById(R.id.size);
        popupButton = itemView.findViewById(R.id.button_popup);
        line1 = itemView.findViewById(R.id.line1);
        line2 = itemView.findViewById(R.id.line2);
        iconView = itemView.findViewById(android.R.id.icon);
        popupButton.setOnClickListener(this);
        popupButton.setVisibility(isSpecialDevice() ? View.INVISIBLE : View.VISIBLE);

        if (isTelevision()) {
            itemView.setOnFocusChangeListener(focusChangeListener);
        }
    }

    public void setData(Cursor cursor, int position) {
        super.setData(cursor, position);
        if (multiChoiceHelper != null) {
            updateCheckedState(position);
        }
    }

    public void setSelected(boolean selected, boolean animate) {
        itemView.setActivated(selected);
        itemView.setSelected(selected);
    }

    public void setEnabled(boolean enabled) {
        setEnabledRecursive(itemView, enabled);
    }

    void updateCheckedState(int position) {
        final boolean isChecked = multiChoiceHelper.isItemChecked(position);
        if (itemView instanceof Checkable) {
            ((Checkable) itemView).setChecked(isChecked);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            itemView.setActivated(isChecked);
            setSelected(isChecked, true);
        }
    }

    public boolean isMultiChoiceActive() {
        return (multiChoiceHelper != null) && (multiChoiceHelper.getCheckedItemCount() > 0);
    }

    @Override
    public void onClick(View v) {
        if(null != clickListener) {
            clickListener.onItemViewClick(v, getLayoutPosition());
        }
    }

    private static void setEnabledRecursive(View v, boolean enabled) {
        if (v == null)
            return;
        if (v.isEnabled() == enabled)
            return;
        v.setEnabled(enabled);

        if (v instanceof ViewGroup) {
            final ViewGroup vg = (ViewGroup) v;
            for (int i = vg.getChildCount() - 1; i >= 0; i--) {
                setEnabledRecursive(vg.getChildAt(i), enabled);
            }
        }
    }

    private View.OnFocusChangeListener focusChangeListener =new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (hasFocus) {
                focusIn(v);
            } else {
                focusOut(v);
            }
            ViewGroup parent = (ViewGroup) v.getParent();
            if (parent != null) {
                parent.requestLayout();
                parent.postInvalidate();
            }
        }
    };

    protected void focusOut(View v) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(v, "scaleX", 1.05f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(v, "scaleY", 1.05f, 1.0f);
        AnimatorSet set = new AnimatorSet();
        set.play(scaleX).with(scaleY);
        set.start();
    }

    protected void focusIn(View v) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(v, "scaleX", 1.0f, 1.05f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(v, "scaleY", 1.0f, 1.05f);
        AnimatorSet set = new AnimatorSet();
        set.play(scaleX).with(scaleY);
        set.start();
    }
}
