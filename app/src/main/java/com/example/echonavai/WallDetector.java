package com.example.echonavai;

import android.graphics.Bitmap;
import android.graphics.Color;

public class WallDetector {

    public static boolean isWallAhead(Bitmap bitmap) {

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        int wallPixelCount = 0;
        int sampleCount = 0;

        for (int y = height/3; y < height*2/3; y += 10) {

            for (int x = width/3; x < width*2/3; x += 10) {

                int pixel = bitmap.getPixel(x,y);

                int r = Color.red(pixel);
                int g = Color.green(pixel);
                int b = Color.blue(pixel);

                int brightness = (r+g+b)/3;

                if (brightness < 120) {
                    wallPixelCount++;
                }

                sampleCount++;
            }
        }

        return wallPixelCount > sampleCount * 0.6;
    }
}