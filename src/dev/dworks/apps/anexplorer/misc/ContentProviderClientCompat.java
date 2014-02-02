package dev.dworks.apps.anexplorer.misc;

import java.lang.reflect.Method;

import android.content.ContentProviderClient;

public class ContentProviderClientCompat {
	
	public static void setDetectNotResponding(ContentProviderClient client, long anrTimeout){
		if(Utils.hasKitKat()){
			try {
				Method method = client.getClass().getMethod("setDetectNotResponding", long.class);
				if (method != null) {
					method.invoke(client, anrTimeout);
				}	
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void releaseQuietly(ContentProviderClient client) {
		if (client != null) {
			try {
				client.release();
			} catch (Exception ignored) {
			}
		}
	}
}
