package dev.dworks.apps.anexplorer;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;

import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.analytics.tracking.android.Tracker;

import android.app.Application;

@ReportsCrashes(formKey = "dHh0Z3hQV1BvMUY2MzZYak01WlJ6RGc6MA")
public class AnExplorer extends Application {

	private GoogleAnalytics googleAnalytics;
	public static Tracker tracker;
	@Override
	public void onCreate() {
		super.onCreate();
		ACRA.init(this);
		googleAnalytics = GoogleAnalytics.getInstance(this);
		googleAnalytics.getTracker(getString(R.string.ga_trackingId));
		tracker = googleAnalytics.getDefaultTracker();
	}
}
