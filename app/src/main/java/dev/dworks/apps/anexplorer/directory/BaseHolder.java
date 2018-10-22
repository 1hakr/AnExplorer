package dev.dworks.apps.anexplorer.directory;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.widget.ImageView;

public class BaseHolder extends MultiChoiceHelper.ViewHolder  {

    static final float DISABLED_ALPHA = 0.3f;
    public static final int CHECK_ANIMATION_DURATION = 100;

    public BaseHolder(Context context, ViewGroup parent, int layout) {
        this(context, inflateLayout(context, parent, layout));
    }

    public BaseHolder(Context context, View item) {
        super(item);
    }

    public BaseHolder(View itemView) {
        super(itemView);
    }

    public void setData(Cursor cursor, int position) {

    }

    public void setData(String message, int icon) {

    }

    protected static <V extends View> V inflateLayout(Context context, ViewGroup parent, int layout) {
        final LayoutInflater inflater = LayoutInflater.from(context);
        return (V) inflater.inflate(layout, parent, false);
    }

    protected static ViewPropertyAnimator fade(ImageView view, float alpha) {
        return view.animate().setDuration(CHECK_ANIMATION_DURATION).alpha(alpha);
    }
}
