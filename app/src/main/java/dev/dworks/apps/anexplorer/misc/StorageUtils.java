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

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.text.TextUtils;

import org.w3c.dom.Text;

import java.io.File;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public final class StorageUtils {

    // partition types
    public static final int PARTITION_SYSTEM = 1;
    public static final int PARTITION_DATA = 2;
    public static final int PARTITION_CACHE = 3;
    public static final int PARTITION_RAM = 4;
    public static final int PARTITION_EXTERNAL = 5;
    public static final int PARTITION_EMMC = 6;
    public static final int PARTITION_ESTORAGE = 7;
    
    //private static final String TAG = "StorageUtils";
	private StorageManager mStorageManager;
	private ActivityManager activityManager;
    private Context mContext;
    public StorageUtils(Context context){
        mContext = context;
        mStorageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    }

    public List<StorageVolume> getStorageMounts() {
        List<StorageVolume> mounts = new ArrayList<StorageVolume>();
        boolean first = false;
        try {
			Method getVolumeList = StorageManager.class.getDeclaredMethod("getVolumeList");
			Object[] sv = (Object[])getVolumeList.invoke(mStorageManager);
			for (Object object : sv) {
				String filePath = getPath(object);
				boolean emulated = getEmulated(object);
				boolean primary = false;
                if(Utils.hasJellyBeanMR1()){
                    primary = getPrimary(object);
                }
                else{
                    if(!first){
                        first = true;
                        primary = true;
                    }
                }

                String description = Utils.hasKitKat() ? getUserLabel(object) : getDescription(object);
				StorageVolume mount = new StorageVolume(description, filePath);
				mount.isEmulated = emulated;
				mount.isPrimary = primary;
				mounts.add(mount);
			}
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		}
        return mounts;
    }

    private String getPath(Object object) throws IllegalAccessException, NoSuchFieldException {
        String path = "";
        Field mPath = object.getClass().getDeclaredField("mPath");
        mPath.setAccessible(true);
        Object pathObj = mPath.get(object);
        if(Utils.hasJellyBeanMR1()){
            path = ((File)pathObj).toString();
        }
        else{
            path = (String)pathObj;
        }
        return path;
    }

    private String getDescription(Object object) throws IllegalAccessException, NoSuchFieldException {
        String description = "";
        if(Utils.hasJellyBean()){
            Field mDescription = object.getClass().getDeclaredField("mDescriptionId");
            mDescription.setAccessible(true);
            int mDescriptionInt = mDescription.getInt(object);
            description = mContext.getResources().getString(mDescriptionInt);
        }
        else{
            Field mDescription = object.getClass().getDeclaredField("mDescription");
            mDescription.setAccessible(true);
            description = (String)mDescription.get(object);
        }
        return description;
    }

    private String getUserLabel(Object object) throws IllegalAccessException, NoSuchFieldException {
        String userLabel = "";
        if(Utils.hasKitKat()) {
            Field mDescription = object.getClass().getDeclaredField("mUserLabel");
            mDescription.setAccessible(true);
            userLabel = (String) mDescription.get(object);
            if(TextUtils.isEmpty(userLabel)){
                try{
                    userLabel = getDescription(object);
                }
                catch (Exception e){ }
            }
        }
        return userLabel;
    }

    private boolean getPrimary(Object object) throws IllegalAccessException, NoSuchFieldException {
        boolean primary = false;
        Field mPrimary = object.getClass().getDeclaredField("mPrimary");
        mPrimary.setAccessible(true);
        primary = mPrimary.getBoolean(object);
        return primary;
    }

    private boolean getEmulated(Object object) throws IllegalAccessException, NoSuchFieldException {
        boolean emulated = false;
        if(Utils.hasJellyBeanMR1()){
            Field mEmulated = object.getClass().getDeclaredField("mEmulated");
            mEmulated.setAccessible(true);
            emulated = mEmulated.getBoolean(object);
        }

        return emulated;
    }

    private boolean getRemovable(Object object) throws IllegalAccessException, NoSuchFieldException {
        boolean removable = false;
        if(Utils.hasJellyBeanMR1()){
            Field mRemovable = object.getClass().getDeclaredField("mRemovable");
            mRemovable.setAccessible(true);
            removable = mRemovable.getBoolean(object);
        }

        return removable;
    }
    
	public static long getExtStorageSize(String path, boolean isTotal){
		return getPartionSize(path, isTotal);		
	}

	public long getPartionSize(int type, boolean isTotal){
		Long size = 0L;
		
		switch (type) {
		case PARTITION_SYSTEM:
			size = getPartionSize(Environment.getRootDirectory().getPath(), isTotal);
			break;
		case PARTITION_DATA:
			size = getPartionSize(Environment.getDataDirectory().getPath(), isTotal);
			break;
		case PARTITION_CACHE:
			size = getPartionSize(Environment.getDownloadCacheDirectory().getPath(), isTotal);
			break;
		case PARTITION_EXTERNAL:
			size = getPartionSize(Environment.getExternalStorageDirectory().getPath(), isTotal);
			break;
		/*case PARTITION_EMMC:
			size = getPartionSize(DIR_EMMC, isTotal);
			break;*/
		case PARTITION_RAM:
			size = getSizeTotalRAM(isTotal);
			break;			
		}
		return size;
	}
	
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private long getSizeTotalRAM(boolean isTotal) {
		long sizeInBytes = 1000;
		MemoryInfo mi = new MemoryInfo();
		activityManager.getMemoryInfo(mi);
		if(isTotal) {
			try { 
				if(Utils.hasJellyBean()){
					long totalMegs = mi.totalMem;
					sizeInBytes = totalMegs;
				}
				else{
					RandomAccessFile reader = new RandomAccessFile("/proc/meminfo", "r");
					String load = reader.readLine();
					String[] totrm = load.split(" kB");
					String[] trm = totrm[0].split(" ");
					sizeInBytes=Long.parseLong(trm[trm.length-1]);
					sizeInBytes=sizeInBytes*1024;
					reader.close();	
				}
			} 
			catch (Exception e) { }
		}
		else{
			long availableMegs = mi.availMem;
			sizeInBytes = availableMegs;
		}		
		return sizeInBytes;
	}
	
	/**
	 * @param isTotal  The parameter for calculating total size
	 * @return return Total Size when isTotal is {@value true} else return Free Size of Internal memory(data folder) 
	 */
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @SuppressWarnings("deprecation")
	private static long getPartionSize(String path, boolean isTotal){
		StatFs stat = null;
		try {
			stat = new StatFs(path);	
		} catch (Exception e) { }
		if(null != stat){
            if(Utils.hasJellyBeanMR2()){
                final long blockSize = stat.getBlockSizeLong();
                final long availableBlocks = (isTotal ? (long)stat.getBlockCountLong() : (long)stat.getAvailableBlocksLong());
                return availableBlocks * blockSize;
            }
            else{
                final long blockSize = stat.getBlockSize();
                final long availableBlocks = (isTotal ? (long)stat.getBlockCount() : (long)stat.getAvailableBlocks());
                return availableBlocks * blockSize;
            }
		}
		else return 0L;
	}
}