package com.steelkiwi.cropiwa.image;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;

/**
 * @author yarolegovich
 * 25.02.2017.
 */
public class CropArea {

    private final Rect imageRect;
    private final Rect cropRect;

    public CropArea(Rect imageRect, Rect cropRect) {
        this.imageRect = imageRect;
        this.cropRect = cropRect;
    }

    public static CropArea create(RectF coordinateSystem, RectF imageRect, RectF cropRect) {
        return new CropArea(
                moveRectToCoordinateSystem(coordinateSystem, imageRect),
                moveRectToCoordinateSystem(coordinateSystem, cropRect));
    }

    private static Rect moveRectToCoordinateSystem(RectF system, RectF rect) {
        float originX = system.left, originY = system.top;
        return new Rect(
                Math.round(rect.left - originX), Math.round(rect.top - originY),
                Math.round(rect.right - originX), Math.round(rect.bottom - originY));
    }

    public Bitmap applyCropTo(Bitmap bitmap) {
        Integer x = findRealCoordinate(bitmap.getWidth(), cropRect.left, imageRect.width());
        Integer y = findRealCoordinate(bitmap.getHeight(), cropRect.top, imageRect.height());
        if (x < 0 || y < 0 || bitmap.getWidth() > imageRect.width() ||bitmap.getHeight() > imageRect.height()) {
//            throw new NullPointerException("Failed to load bitmap");
            return null;
        }else{
            Bitmap immutableCropped = Bitmap.createBitmap(bitmap,
                    x,
                    y,
                    findRealCoordinate(bitmap.getWidth(), cropRect.width(), imageRect.width()),
                    findRealCoordinate(bitmap.getHeight(), cropRect.height(), imageRect.height()));
            return immutableCropped.copy(immutableCropped.getConfig(), true);

        }
    }


    private int findRealCoordinate(int imageRealSize, int cropCoordinate, float cropImageSize) {
        return Math.round((imageRealSize * cropCoordinate) / cropImageSize);
    }

}
