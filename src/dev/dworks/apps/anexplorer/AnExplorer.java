package dev.dworks.apps.anexplorer;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;

import android.preference.PreferenceManager;

import dev.dworks.libs.actionbarplus.SherlockApplication;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.analytics.tracking.android.Tracker;

import dev.dworks.apps.anexplorer.util.ExplorerOperations;

@ReportsCrashes(formKey = "dHh0Z3hQV1BvMUY2MzZYak01WlJ6RGc6MA")
public class AnExplorer extends SherlockApplication {

	private GoogleAnalytics googleAnalytics;
	public static Tracker tracker;
	@Override
	public void onCreate() {
		Integer themeType = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(this).getString("ThemePref", "2"));
		setTheme(ExplorerOperations.THEMES[themeType]);
		super.onCreate();
		ACRA.init(this);
		googleAnalytics = GoogleAnalytics.getInstance(this);
		googleAnalytics.getTracker(getString(R.string.ga_trackingId));
		tracker = googleAnalytics.getDefaultTracker();
	}
}
