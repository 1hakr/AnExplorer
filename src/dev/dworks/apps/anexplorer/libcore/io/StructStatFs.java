/*
 * Copyright (C) 2011 The Android Open Source Project
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

package dev.dworks.apps.anexplorer.libcore.io;

/**
 * File information returned by fstatfs(2) and statfs(2).
 *
 * TODO: this should be {@code struct statvfs}, but Bionic doesn't support that yet.
 * @hide until the TODO is fixed.
 */
public final class StructStatFs {
    /** File system block size (used for block counts). */
    public final long f_bsize; /*unsigned long*/

    /** Total block count. */
    public final long f_blocks; /*fsblkcnt_t*/

    /** Free block count. */
    public final long f_bfree; /*fsblkcnt_t*/

    /** Free block count available to non-root. */
    public final long f_bavail; /*fsblkcnt_t*/

    /** Total file (inode) count. */
    public final long f_files; /*fsfilcnt_t*/

    /** Free file (inode) count. */
    public final long f_ffree; /*fsfilcnt_t*/

    /** Maximum filename length. */
    public final long f_namemax; /*unsigned long*/

    /** Fundamental file system block size. */
    public final long f_frsize; /*unsigned long*/

    StructStatFs(long f_bsize, long f_blocks, long f_bfree, long f_bavail,
            long f_files, long f_ffree, long f_namemax, long f_frsize) {
        this.f_bsize = f_bsize;
        this.f_blocks = f_blocks;
        this.f_bfree = f_bfree;
        this.f_bavail = f_bavail;
        this.f_files = f_files;
        this.f_ffree = f_ffree;
        this.f_namemax = f_namemax;
        this.f_frsize = f_frsize;
    }
}
