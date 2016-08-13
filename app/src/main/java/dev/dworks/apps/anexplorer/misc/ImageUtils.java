package dev.dworks.apps.anexplorer.misc;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;
import android.widget.ImageView;

import java.io.File;

public class ImageUtils {

    private static final String TAG = "ImageUtils";

    /**
     * Returns the largest power-of-two divisor for use in downscaling a bitmap
     * that will not result in the scaling past the desired dimensions.
     *
     * @param actualWidth Actual width of the bitmap
     * @param actualHeight Actual height of the bitmap
     * @param desiredWidth Desired width of the bitmap
     * @param desiredHeight Desired height of the bitmap
     */
    // Visible for testing.
    public static int findBestSampleSize(
            int actualWidth, int actualHeight, int desiredWidth, int desiredHeight) {
        double wr = (double) actualWidth / desiredWidth;
        double hr = (double) actualHeight / desiredHeight;
        double ratio = Math.min(wr, hr);
        float n = 1.0f;
        while ((n * 2) <= ratio) {
            n *= 2;
        }

        return (int) n;
    }

    public static Bitmap getThumbnail(String path, String mimeType, int mMaxWidth, int mMaxHeight) {
        try {
            final String typeOnly = mimeType.split("/")[0];
            if ("image".equals(typeOnly)) {
                return getImageThumbnail(path, mMaxWidth, mMaxHeight);
            } else if ("video".equals(typeOnly)) {
                return getVideoThumbnail(path, mMaxWidth, mMaxHeight);
            } else {
                return null;
            }
        } catch (OutOfMemoryError e) {
            return null;
        }
    }

    public static Bitmap getImageThumbnail(String path, int mMaxWidth, int mMaxHeight){
        Bitmap.Config mDecodeConfig = Bitmap.Config.RGB_565;
        ImageView.ScaleType mScaleType = ImageView.ScaleType.CENTER_CROP;

        File bitmapFile = new File(path);
        Bitmap bitmap = null;

        if (!bitmapFile.exists() || !bitmapFile.isFile()) {
            return bitmap;
        }

        BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
        decodeOptions.inInputShareable = true;
        decodeOptions.inPurgeable = true;
        decodeOptions.inPreferredConfig = mDecodeConfig;
        if (mMaxWidth == 0 && mMaxHeight == 0) {

            bitmap = BitmapFactory.decodeFile(bitmapFile.getAbsolutePath(), decodeOptions);
        } else {
            // If we have to resize this image, first get the natural bounds.
            decodeOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(bitmapFile.getAbsolutePath(), decodeOptions);
            int actualWidth = decodeOptions.outWidth;
            int actualHeight = decodeOptions.outHeight;

            // Then compute the dimensions we would ideally like to decode to.
            int desiredWidth = getResizedDimension(mMaxWidth, mMaxHeight,
                    actualWidth, actualHeight, mScaleType);
            int desiredHeight = getResizedDimension(mMaxHeight, mMaxWidth,
                    actualHeight, actualWidth, mScaleType);

            // Decode to the nearest power of two scaling factor.
            decodeOptions.inJustDecodeBounds = false;
            decodeOptions.inSampleSize = ImageUtils.findBestSampleSize(actualWidth, actualHeight, desiredWidth, desiredHeight);
            Bitmap tempBitmap = BitmapFactory.decodeFile(bitmapFile.getAbsolutePath(), decodeOptions);
            // If necessary, scale down to the maximal acceptable size.
            if (tempBitmap != null
                    && (tempBitmap.getWidth() > desiredWidth || tempBitmap.getHeight() > desiredHeight)) {
                bitmap = Bitmap.createScaledBitmap(tempBitmap, desiredWidth,
                        desiredHeight, true);
                tempBitmap.recycle();
            } else {
                bitmap = tempBitmap;
            }

        }
        return bitmap;
    }

    public static Bitmap getVideoThumbnail(String path, int mMaxWidth, int mMaxHeight){
        Bitmap.Config mDecodeConfig = Bitmap.Config.RGB_565;
        ImageView.ScaleType mScaleType = ImageView.ScaleType.CENTER_CROP;

        File bitmapFile = new File(path);
        Bitmap bitmap = null;

        if (!bitmapFile.exists() || !bitmapFile.isFile()) {
            return bitmap;
        }

        BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
        decodeOptions.inInputShareable = true;
        decodeOptions.inPurgeable = true;
        decodeOptions.inPreferredConfig = mDecodeConfig;
        if (mMaxWidth == 0 && mMaxHeight == 0) {

            bitmap = getVideoFrame(bitmapFile.getAbsolutePath());
        } else {
            // If we have to resize this image, first get the natural bounds.
            decodeOptions.inJustDecodeBounds = true;
            //BitmapFactory.decodeFile(bitmapFile.getAbsolutePath(), decodeOptions);
            int actualWidth = decodeOptions.outWidth;
            int actualHeight = decodeOptions.outHeight;

            // Then compute the dimensions we would ideally like to decode to.
            int desiredWidth = getResizedDimension(mMaxWidth, mMaxHeight,
                    actualWidth, actualHeight, mScaleType);
            int desiredHeight = getResizedDimension(mMaxHeight, mMaxWidth,
                    actualHeight, actualWidth, mScaleType);

            // Decode to the nearest power of two scaling factor.
            decodeOptions.inJustDecodeBounds = false;
            decodeOptions.inSampleSize = ImageUtils.findBestSampleSize(actualWidth, actualHeight, desiredWidth, desiredHeight);
            Bitmap tempBitmap = getVideoFrame(bitmapFile.getAbsolutePath());
            // If necessary, scale down to the maximal acceptable size.
            if (tempBitmap != null
                    && (tempBitmap.getWidth() > desiredWidth || tempBitmap.getHeight() > desiredHeight)) {
                bitmap = Bitmap.createScaledBitmap(tempBitmap, desiredWidth,
                        desiredHeight, true);
                tempBitmap.recycle();
            } else {
                bitmap = tempBitmap;
            }
        }
        return bitmap;
    }

    private static Bitmap getVideoFrame(String path) {
        return ThumbnailUtils.createVideoThumbnail(path, MediaStore.Images.Thumbnails.MINI_KIND);
    }

    /**
     * Scales one side of a rectangle to fit aspect ratio.
     *
     * @param maxPrimary Maximum size of the primary dimension (i.e. width for
     *        max width), or zero to maintain aspect ratio with secondary
     *        dimension
     * @param maxSecondary Maximum size of the secondary dimension, or zero to
     *        maintain aspect ratio with primary dimension
     * @param actualPrimary Actual size of the primary dimension
     * @param actualSecondary Actual size of the secondary dimension
     * @param scaleType The ScaleType used to calculate the needed image size.
     */
    private static int getResizedDimension(int maxPrimary, int maxSecondary, int actualPrimary,
                                           int actualSecondary, ImageView.ScaleType scaleType) {
        // If no dominant value at all, just return the actual.
        if ((maxPrimary == 0) && (maxSecondary == 0)) {
            return actualPrimary;
        }
        // If ScaleType.FIT_XY fill the whole rectangle, ignore ratio.
        if (scaleType == ImageView.ScaleType.FIT_XY) {
            if (maxPrimary == 0) {
                return actualPrimary;
            }
            return maxPrimary;
        }
        // If primary is unspecified, scale primary to match secondary's scaling ratio.
        if (maxPrimary == 0) {
            double ratio = (double) maxSecondary / (double) actualSecondary;
            return (int) (actualPrimary * ratio);
        }
        if (maxSecondary == 0) {
            return maxPrimary;
        }
        double ratio = (double) actualSecondary / (double) actualPrimary;
        int resized = maxPrimary;
        // If ScaleType.CENTER_CROP fill the whole rectangle, preserve aspect ratio.
        if (scaleType == ImageView.ScaleType.CENTER_CROP) {
            if ((resized * ratio) < maxSecondary) {
                resized = (int) (maxSecondary / ratio);
            }
            return resized;
        }
        if ((resized * ratio) > maxSecondary) {
            resized = (int) (maxSecondary / ratio);
        }
        return resized;
    }
}