package dev.dworks.apps.anexplorer.ui;
import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

/**
 * 
 */

/**
 * @author HaKr
 *
 */
public class MyPrerference extends Preference {


	private int mIcon = 0; 

	public MyPrerference(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public MyPrerference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
        /*TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.IconPreference, defStyle, 0);
        mIcon = a.getResourceId(R.styleable.IconPreference_iconPref, R.drawable.gplus32);
        a.recycle();*/
        mIcon = attrs.getAttributeResourceValue(2, 0);        
	}	
	
	@Override
	protected void onBindView(View view) {	
		super.onBindView(view);		
        ImageView imageView = (ImageView) view.findViewById(android.R.id.icon);
        if (imageView != null) {
            if (mIcon != 0) {
                imageView.setImageResource(mIcon);
            } else {
                imageView.setVisibility(View.GONE);
            }
        }        
	}

}
