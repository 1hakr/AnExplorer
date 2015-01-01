package dev.dworks.apps.anexplorer.misc;

import java.lang.reflect.Method;

import android.annotation.TargetApi;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

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
	
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static ContentProviderClient acquireUnstableContentProviderClient(ContentResolver resolver, String authority){
		if(Utils.hasJellyBeanMR1()){
	    	return resolver.acquireUnstableContentProviderClient(authority);
		}
		else{
	        return resolver.acquireContentProviderClient(authority);
		}
	}
	
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static Bundle call(ContentResolver resolver, ContentProviderClient client, Uri uri, String method, String arg, Bundle extras) throws Exception{
		if(Utils.hasJellyBeanMR1()){
	    	return client.call(method, arg, extras);
		}
		else{
	        return resolver.call(uri, method, arg, extras);
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
