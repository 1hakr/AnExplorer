package dev.dworks.apps.anexplorer.misc;

import android.content.ContentProviderClient;

public class ContentProviderClientCompat {
	public static void releaseQuietly(ContentProviderClient client) {
		if (client != null) {
			try {
				client.release();
			} catch (Exception ignored) {
			}
		}
	}
}
