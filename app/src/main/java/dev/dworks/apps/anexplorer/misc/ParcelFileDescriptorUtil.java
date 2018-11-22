/*
 * Copyright © unknown year Mark Murphy
 *             2014-2015 Jan Seeger
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

import android.os.ParcelFileDescriptor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import dev.dworks.apps.anexplorer.libcore.io.IoUtils;

/**
 * ParcelFileDescriptor Utility class.
 * Based on CommonsWare's ParcelFileDescriptorUtil.
 */
public class ParcelFileDescriptorUtil {
    public static ParcelFileDescriptor pipeFrom(InputStream inputStream)
            throws IOException {
        final ParcelFileDescriptor[] pipe = ParcelFileDescriptorUtil.createPipe();
        final OutputStream output = new ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]);
        new TransferThread(inputStream, output).start();

        return pipe[0];
    }

    @SuppressWarnings("unused")
    public static ParcelFileDescriptor pipeTo(OutputStream outputStream)
            throws IOException {
        final ParcelFileDescriptor[] pipe = ParcelFileDescriptorUtil.createPipe();
        final InputStream input = new ParcelFileDescriptor.AutoCloseInputStream(pipe[0]);

        new TransferThread(input, outputStream).start();

        return pipe[1];
    }

    static class TransferThread extends Thread {
        final InputStream mIn;
        final OutputStream mOut;

        TransferThread(InputStream in, OutputStream out) {
            super("ParcelFileDescriptor Transfer Thread");
            mIn = in;
            mOut = out;
            setDaemon(true);
        }

        @Override
        public void run() {
            try {
                IoUtils.copy(mIn, mOut);
            } catch (Exception e) {
                //Log.e("TransferThread", "writing failed");
                CrashReportingManager.logException(e);
            } finally {
                IoUtils.flushQuietly(mOut);
                IoUtils.closeQuietly(mIn);
                IoUtils.closeQuietly(mOut);
            }
        }
    }

    public static int parseMode(String mode) {
        final int modeBits;
        if ("r".equals(mode)) {
            modeBits = ParcelFileDescriptor.MODE_READ_ONLY;
        } else if ("w".equals(mode) || "wt".equals(mode)) {
            modeBits = ParcelFileDescriptor.MODE_WRITE_ONLY
                    | ParcelFileDescriptor.MODE_CREATE
                    | ParcelFileDescriptor.MODE_TRUNCATE;
        } else if ("wa".equals(mode)) {
            modeBits = ParcelFileDescriptor.MODE_WRITE_ONLY
                    | ParcelFileDescriptor.MODE_CREATE
                    | ParcelFileDescriptor.MODE_APPEND;
        } else if ("rw".equals(mode)) {
            modeBits = ParcelFileDescriptor.MODE_READ_WRITE
                    | ParcelFileDescriptor.MODE_CREATE;
        } else if ("rwt".equals(mode)) {
            modeBits = ParcelFileDescriptor.MODE_READ_WRITE
                    | ParcelFileDescriptor.MODE_CREATE
                    | ParcelFileDescriptor.MODE_TRUNCATE;
        } else {
            throw new IllegalArgumentException("Bad mode '" + mode + "'");
        }
        return modeBits;
    }

    public static ParcelFileDescriptor[] createPipe() throws IOException {
        if(Utils.hasKitKat()){
            return ParcelFileDescriptor.createReliablePipe();
        }
        return ParcelFileDescriptor.createPipe();
    }
}