package dev.dworks.apps.anexplorer;

import android.preference.PreferenceManager;

import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.analytics.tracking.android.Tracker;
import com.joshdholtz.sentry.Sentry;

import dev.dworks.apps.anexplorer.util.ExplorerOperations;
import dev.dworks.libs.actionbarplus.app.ActionBarApplication;

public class AnExplorer extends ActionBarApplication {

	private GoogleAnalytics googleAnalytics;
	public static Tracker tracker;
	@Override
	public void onCreate() {
		Integer themeType = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(this).getString("ThemePref", "2"));
		setTheme(ExplorerOperations.THEMES[themeType]);
		super.onCreate();
		Sentry.init(this, "https://adf863ae0013482a9e052d062d0326df:30ec3caf684c4606ac1a659b7e63ef66@app.getsentry.com/14229");
		googleAnalytics = GoogleAnalytics.getInstance(this);
		googleAnalytics.getTracker(getString(R.string.ga_trackingId));
		tracker = googleAnalytics.getDefaultTracker();
	}
}
