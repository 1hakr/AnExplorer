package dev.dworks.apps.anexplorer;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.viewpagerindicator.IconPagerAdapter;
import com.viewpagerindicator.UnderlinePageIndicator;

import dev.dworks.apps.anexplorer.util.ExplorerOperations;

public class TutorialActivity extends SherlockFragmentActivity {
    protected final static String[] TITLES = new String[] {
    	"Welcome",
    	"Overview",
    	"Login",
    	"Navigation",
    	"Features",
    	"Apps",
    	"Process",
    	"Hidden",
    	"Wallpaper",
    	"Start Exploring"};
    protected final static int[] PICTURES = new int[] {
    		R.drawable.logo_login,
    		R.drawable.tutorial_overview,
            R.drawable.tutorial_login,
            R.drawable.tutorial_navigation,
            R.drawable.tutorial_features,
            R.drawable.tutorial_apps,
            R.drawable.tutorial_process,
            R.drawable.tutorial_hidden,
            R.drawable.tutorial_wallpaper,
            R.drawable.dworks_dark,
    };

    protected final static int[] CONTENT = new int[] {
		R.string.tutorial_welcome,
		R.string.tutorial_overview,
		R.string.tutorial_login,
		R.string.tutorial_navigation,
		R.string.tutorial_features,
		R.string.tutorial_apps,
		R.string.tutorial_process,
		R.string.tutorial_hidden,
		R.string.tutorial_wallpaper,
		R.string.tutorial_explore
    };
    
    private TutorialFragmentAdapter mAdapter;
	private ViewPager mPager;

	@Override
    protected void onCreate(Bundle savedInstanceState) {
		setTheme(ExplorerOperations.THEMES[1]);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tutorial);

        getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.tutorialAB));
        mAdapter = new TutorialFragmentAdapter(getSupportFragmentManager());

        mPager = (ViewPager)findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);

        UnderlinePageIndicator indicator = (UnderlinePageIndicator)findViewById(R.id.indicator);
        indicator.setViewPager(mPager);
        indicator.setFades(false);
    }
	
	@Override
	public void onStart() {
		super.onStart();
		AnExplorer.tracker.sendView("TutorialActivity");
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.options_tutorial, menu);
		return super.onCreateOptionsMenu(menu);
	}
    
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int curPosition = mPager.getCurrentItem();
		switch (item.getItemId()) {
		case R.id.menu_prev:
			if(curPosition > 0){
				mPager.setCurrentItem(curPosition - 1);
			}
			break;

		case R.id.menu_next:
			if(curPosition < mAdapter.getCount()-1){
				mPager.setCurrentItem(curPosition + 1);
			}
			break;

		case R.id.menu_skip:
			finish();
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	protected void onDestroy() {
		SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(this);
		if(preference.getInt("tutorialPref", -1) == -1){
			Editor editor = preference.edit();
			editor.putInt("tutorialPref", 1);
			editor.commit();
		}
		super.onDestroy();
	}
	
    public static class TutorialFragmentAdapter extends FragmentPagerAdapter implements IconPagerAdapter {

        private int mCount = TITLES.length;
        public TutorialFragmentAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return TutorialFragment.newInstance(position);
        }

        @Override
        public int getCount() {
            return mCount;
        }

        @Override
        public CharSequence getPageTitle(int position) {
          return TITLES[position % TITLES.length];
        }

        @Override
        public int getIconResId(int index) {
          return PICTURES[index % PICTURES.length];
        }

        public void setCount(int count) {
            if (count > 0 && count <= 10) {
                mCount = count;
                notifyDataSetChanged();
            }
        }
    }
    
    public final static class TutorialFragment extends Fragment {
        private static final String KEY_CONTENT = "TestFragment:Content";

        public static TutorialFragment newInstance(int position) {
            TutorialFragment fragment = new TutorialFragment();
            fragment.position = position;
            return fragment;
        }

        private int position = 0;
        //private String mContent = "???";
		private View root;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            if ((savedInstanceState != null) && savedInstanceState.containsKey(KEY_CONTENT)) {
            	position = savedInstanceState.getInt(KEY_CONTENT);
            }
        }

		@Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        	root = inflater.inflate(R.layout.fragment_tutorial, container, false);
            initControls();
            return root;
        }

        private void initControls() {
			TextView title = (TextView) root.findViewById(R.id.title);
			TextView content = (TextView) root.findViewById(R.id.content);
			ImageView picture = (ImageView) root.findViewById(R.id.picture);
			title.setText(TITLES[position]);
			content.setText(CONTENT[position]);
			picture.setImageResource(PICTURES[position]);
			if(position == TITLES.length - 1){

			}
			else{
				
			}
		}

		@Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putInt(KEY_CONTENT, position);
        }
    }
}