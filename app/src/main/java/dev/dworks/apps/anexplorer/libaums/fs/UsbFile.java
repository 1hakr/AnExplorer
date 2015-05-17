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

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * This class represents either an file or an directory. This can be determined
 * by {@link #isDirectory()}. Not all methods make sense for both cases. For
 * example if representing a file methods like {@link #createDirectory(String)}
 * or {@link #createFile(String)} do not make sense and will throw an exception.
 * On the other hand methods like {@link #read(long, ByteBuffer)} or
 * {@link #write(long, ByteBuffer)} do not make sense for directories and will
 * throw an exception!
 * 
 * @author mjahnen
 * 
 */
public interface UsbFile extends Closeable {
	/**
	 * 
	 * @return True if representing a directory.
	 */
	public boolean isDirectory();

	/**
	 * 
	 * @return The name of the file or directory.
	 */
	public String getName();

	/**
	 * Set a new name for this file or directory.
	 * 
	 * @param newName
	 *            The new name.
	 * @throws IOException
	 *             If new name is already assigned or writing to the file system
	 *             fails.
	 */
	public void setName(String newName) throws IOException;

	/**
	 * Returns the time this directory or file was created.
	 * 
	 * @return Time in milliseconds since January 1 00:00:00, 1970 UTC
	 */
	public long createdAt();
	
	/**
	 * Returns the time this directory or file was last modified.
	 * 
	 * @return Time in milliseconds since January 1 00:00:00, 1970 UTC
	 */
	public long lastModified();
	
	/**
	 * Returns the time this directory or file was last accessed.
	 * 
	 * @return Time in milliseconds since January 1 00:00:00, 1970 UTC
	 */
	public long lastAccessed();

	/**
	 * Returns the parent directory for the file or directory or null if this is
	 * the root directory.
	 * 
	 * @return The parent directory or null.
	 */
	public UsbFile getParent();

	/**
	 * Lists all files in the directory. Throws an exception if called on a
	 * file.
	 * 
	 * @return String array containing all names in the directory.
	 * @throws IOException
	 *             If reading fails
	 */
	public String[] list() throws IOException;

	/**
	 * Lists all files in the directory. Throws an exception if called on a
	 * file.
	 * 
	 * @return UsbFile array containing all files or directories in the
	 *         directory.
	 * @throws IOException
	 *             If reading fails
	 */
	public UsbFile[] listFiles() throws IOException;

	/**
	 * Returns the file length or throws an exception if called on a directory.
	 * 
	 * @return File length in bytes.
	 */
	public long getLength();

	/**
	 * Sets the new file length. This can sometimes be more efficient if all
	 * needed place for a file is allocated on the disk at once and before
	 * writing to it.
	 * <p>
	 * If the space is not allocated before writing the space must be exceeded
	 * every time a new write occurs. This can sometimes be less efficient.
	 * 
	 * @param newLength
	 *            The file length in bytes.
	 * @throws IOException
	 *             If requesting the needed space fails.
	 */
	public void setLength(long newLength) throws IOException;

	/**
	 * Reads from a file or throws an exception if called on a directory.
	 * 
	 * @param offset
	 *            The offset in bytes where reading in the file should be begin.
	 * @param destination
	 *            Buffer the data shall be transferred to.
	 * @throws IOException
	 *             If reading fails.
	 */
	public void read(long offset, ByteBuffer destination) throws IOException;

	/**
	 * Writes to a file or throws an exception if called on a directory.
	 * 
	 * @param offset
	 *            The offset in bytes where writing in the file should be begin.
	 * @param source
	 *            Buffer which contains the data which shall be transferred.
	 * @throws IOException
	 *             If writing fails.
	 */
	public void write(long offset, ByteBuffer source) throws IOException;

	/**
	 * Forces a write. Every change to the file is then committed to the disk.
	 * Throws an exception if called on directories.
	 * 
	 * @throws IOException
	 *             If flushing fails.
	 */
	public void flush() throws IOException;

	/**
	 * Closes and flushes the file. It is essential to close a file after making
	 * changes to it! Throws an exception if called on directories.
	 * 
	 * @throws IOException
	 *             If closing fails.
	 */
	@Override
	public void close() throws IOException;

	/**
	 * This methods creates a new directory with the given name and returns it.
	 * 
	 * @param name
	 *            The name of the new directory.
	 * @return The newly created directory.
	 * @throws IOException
	 *             If writing to the disk fails or a item with the same name
	 *             already exists.
	 */
	public UsbFile createDirectory(String name) throws IOException;

	/**
	 * This methods creates a new file with the given name and returns it.
	 * 
	 * @param name
	 *            The name of the new file.
	 * @return The newly created file.
	 * @throws IOException
	 *             If writing to the disk fails or a item with the same name
	 *             already exists.
	 */
	public UsbFile createFile(String name) throws IOException;

	/**
	 * This methods moves THIS item to the destination directory. Make sure that
	 * the destination is a directory, otherwise an exception will be thrown.
	 * Make also sure that both items are on the same logical device (disk,
	 * partition, file system). Moving between different file systems is
	 * currently not supported. If you want to do this, you have to manually
	 * copy the content and delete the old item.
	 * 
	 * @param destination
	 *            The directory where this item should be moved.
	 * @throws IOException
	 *             If writing fails, or the operation cannot be done (eg. item
	 *             already exists in the destination directory)
	 */
	public void moveTo(UsbFile destination) throws IOException;

	/**
	 * Deletes this file or directory from the parent directory.
	 * 
	 * @throws IOException
	 *             If operation fails due to write errors.
	 */
	public void delete() throws IOException;

	public String getPath();
}
