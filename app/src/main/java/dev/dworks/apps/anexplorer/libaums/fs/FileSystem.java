/*
 * (C) Copyright 2014 mjahnen <jahnen@in.tum.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package dev.dworks.apps.anexplorer.libaums.fs;

/**
 * This class represents a file system.
 * 
 * @author mjahnen
 * 
 */
public interface FileSystem {

	/**
	 * This method returns the root directory of the file system.
	 * 
	 * @return The root directory of the file system.
	 */
	public UsbFile getRootDirectory();

	/**
	 * This method returns the name of the volume which is mostly saved in the
	 * file system.
	 * <p>
	 * In Windows the name of a volume is shown in the explorer before the drive
	 * letter.
	 * 
	 * @return
	 */
	public String getVolumeLabel();

}
