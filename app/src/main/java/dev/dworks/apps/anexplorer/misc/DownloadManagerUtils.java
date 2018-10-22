/*
 * Copyright (C) 2014 Hari Krishna Dulipudi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.dworks.apps.anexplorer.misc;

import android.app.DownloadManager;
import android.app.DownloadManager.Query;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class DownloadManagerUtils {

    public static void setAccessAllDownloads(DownloadManager downloadManager) {
    	try {
            Method setAccessAllDownloads = DownloadManager.class.getMethod("setAccessAllDownloads", boolean.class);
			setAccessAllDownloads.invoke(downloadManager, true);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
    }
    
    public static void setOnlyIncludeVisibleInDownloadsUi(Query query){
       	try {
            Method setOnlyIncludeVisibleInDownloadsUi = Query.class.getMethod("setOnlyIncludeVisibleInDownloadsUi", boolean.class);
			setOnlyIncludeVisibleInDownloadsUi.invoke(query, true);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
    }
}