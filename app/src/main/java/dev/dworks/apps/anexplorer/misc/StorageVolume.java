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

import java.io.File;

import android.text.TextUtils;

public final class StorageVolume {

    private final String label;
    private final String path;
    public boolean isEmulated;
    public boolean isPrimary;
    public int uuid;

    public StorageVolume(String label, String path) {
        this.label = label;
        this.path = path;
    }

    public String getLabel() {
        return label;
    }

    public String getPath() {
        return path;
    }

	public File getPathFile() {
		return new File(path);
	}

	public String getUuid() {
		return uuid+"";
	}
}