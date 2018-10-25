package dev.dworks.apps.anexplorer.misc;

import android.system.Os;

import java.io.FileDescriptor;
import java.lang.reflect.Method;

/** Very hackish */
public final class OsCompat {
    public static int SEEK_CUR;
    public static int SEEK_END;
    public static int SEEK_SET;

    private static Method sLseek;
    private static Object sOs;

    static {
        try {
            Class<?> classLibcore = Class.forName("libcore.io.Libcore");
            Class<?> classOsConstants;
            try {
                classOsConstants = Class.forName("android.system.OsConstants");
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                classOsConstants = Class.forName("libcore.io.OsConstants");
            }

            // Get os object
            sOs = classLibcore.getDeclaredField("os").get(null);

            // Get lseek method
            sLseek = sOs.getClass().getDeclaredMethod(
                    "lseek", FileDescriptor.class, long.class, int.class);

            // Get lseek constants
            SEEK_CUR = classOsConstants.getDeclaredField("SEEK_CUR").getInt(null);
            SEEK_END = classOsConstants.getDeclaredField("SEEK_END").getInt(null);
            SEEK_SET = classOsConstants.getDeclaredField("SEEK_SET").getInt(null);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    public static class ExecutionFailedException extends Exception {
        public ExecutionFailedException(String detailMessage) {
            super(detailMessage);
        }

        public ExecutionFailedException(String detailMessage, Throwable throwable) {
            super(detailMessage, throwable);
        }

        public ExecutionFailedException(Throwable throwable) {
            super(throwable);
        }
    }

    public static Long lseek(FileDescriptor fd, long offset, int whence) throws
            ExecutionFailedException {
        try {
            if(Utils.hasMarshmallow()){
                return Os.lseek(fd, offset, whence);
            }
            return (Long) sLseek.invoke(sOs, fd, offset, whence);
        } catch (Exception e) {
            throw new ExecutionFailedException(e);
        }
    }
}