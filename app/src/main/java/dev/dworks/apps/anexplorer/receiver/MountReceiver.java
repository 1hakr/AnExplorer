/*
 * Copyright (C) 2013 The Android Open Source Project
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

package dev.dworks.apps.anexplorer.receiver;

import android.content.BroadcastReceiver;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;

import dev.dworks.apps.anexplorer.misc.ContentProviderClientCompat;
import dev.dworks.apps.anexplorer.provider.ExternalStorageProvider;

public class MountReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals("android.intent.action.MEDIA_MOUNTED")
                || action.equals("android.intent.action.MEDIA_UNMOUNTED")){
            final ContentProviderClient client = ContentProviderClientCompat.acquireUnstableContentProviderClient(context.getContentResolver(),
                    ExternalStorageProvider.AUTHORITY);
            try {
                ((ExternalStorageProvider) client.getLocalContentProvider()).updateVolumes();
            } finally {
                ContentProviderClientCompat.releaseQuietly(client);
            }
        }
	}
}
