package dev.dworks.apps.anexplorer.misc;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;

import java.io.FileNotFoundException;
import java.lang.reflect.Method;

public class ContentProviderClientCompat {

	//TODO NonSdkApiUsedViolation
	public static void setDetectNotResponding(ContentProviderClient client, long anrTimeout){
		if(Utils.hasKitKat() && !Utils.hasPie()){
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

    public static ContentProviderClient acquireUnstableContentProviderClient(ContentResolver resolver, String authority){
		if(Utils.hasJellyBeanMR1()){
	    	return resolver.acquireUnstableContentProviderClient(authority);
		}
		else{
	        return resolver.acquireContentProviderClient(authority);
		}
	}
	
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

	public static AssetFileDescriptor buildAssetFileDescriptor(ParcelFileDescriptor fd, long startOffset,
														long length, Bundle extras){
		if(!Utils.hasKitKat()) {
			return new AssetFileDescriptor(fd, startOffset, length);
		}
		return new AssetFileDescriptor(fd, startOffset, length, extras);
	}

	public static AssetFileDescriptor openTypedAssetFileDescriptor(ContentProviderClient client,
			Uri uri, String mimeType, Bundle opts, CancellationSignal signal)
			throws FileNotFoundException, RemoteException {
		if(Utils.hasKitKat()) {
			return client.openTypedAssetFileDescriptor(uri, mimeType, opts, signal);
		} else {
			return client.openTypedAssetFileDescriptor(uri, mimeType, opts);
		}
	}
}
