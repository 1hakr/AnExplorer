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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import android.content.IntentSender;
import android.content.pm.PackageManager;
import dev.dworks.apps.anexplorer.DocumentsApplication;

public class PackageManagerUtils {

	@SuppressWarnings("unused")
	public static boolean deleteApplicationCacheFiles(PackageManager pm, String packageName) {
		Class<?> iPackageDataObserverClass = null;
		boolean deleted = false;
		try {
			Method method = null;
			iPackageDataObserverClass = Class.forName("android.content.pm.IPackageDataObserver");
			Object iPackageDataObserverObject = Proxy.newProxyInstance(
					DocumentsApplication.class.getClassLoader(),
					new Class[] { iPackageDataObserverClass },
					new InvocationHandler() {
						@Override
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
							return null;
						}
					});
			method = pm.getClass().getMethod("freeStorage", long.class,  IntentSender.class);
			if (method != null) {
				long desiredFreeStorage = 8 * 1024 * 1024 * 1024; 
				method.invoke(pm, desiredFreeStorage, null);
				deleted = true;
			}
		} catch (Exception e) {
			Throwable t = e.getCause();
			e.printStackTrace();
		}
		
		return deleted;
	}
}
