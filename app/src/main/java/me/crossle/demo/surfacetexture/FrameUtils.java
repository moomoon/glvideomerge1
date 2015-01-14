package me.crossle.demo.surfacetexture;

/**
 * Created by leapin on 2015/1/13.
 */
public class FrameUtils {
    private static void encodeYUV420SPInternal(byte[] yuv420sp, int[] argb, int width, int height, boolean flipped) {
        final int frameSize = width * height;

        int yIndex = 0;
        int uvIndex = frameSize;

        int a, R, G, B, Y, U, V;
        int index = flipped ? frameSize - width : 0;
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {

//                a = (argb[index] & 0xff000000) >> 24; // a is not used obviously
                R = (argb[index] & 0xff0000) >> 16;
                G = (argb[index] & 0xff00) >> 8;
                B = (argb[index] & 0xff) >> 0;

                // well known RGB to YUV algorithm
                Y = ((66 * R + 129 * G + 25 * B + 128) >> 8) + 16;

                // NV21 has a plane of Y and interleaved planes of VU each sampled by a factor of 2
                //    meaning for every 4 Y pixels there are 1 V and 1 U.  Note the sampling is every other
                //    pixel AND every other scanline.
                yuv420sp[yIndex++] = (byte) ((Y < 0) ? 0 : ((Y > 255) ? 255 : Y));
                if (j % 2 == 0 && index % 2 == 0) {
                    U = ((-38 * R - 74 * G + 112 * B + 128) >> 8) + 128;
                    V = ((112 * R - 94 * G - 18 * B + 128) >> 8) + 128;
                    yuv420sp[uvIndex++] = (byte) ((V < 0) ? 0 : ((V > 255) ? 255 : V));
                    yuv420sp[uvIndex++] = (byte) ((U < 0) ? 0 : ((U > 255) ? 255 : U));
                }
                index++;
                if (flipped && index % width == 0) {
                    index -= width;
                    index -= width;
                }

            }
        }
    }

    public static void encodeYUV420SPFlipped(byte[] yuv420, int[] argb, int width, int height) {
        encodeYUV420SPInternal(yuv420, argb, width, height, true);
    }

    public static void encodeYUV420SP(byte[] yuv420, int[] argb, int width, int height) {
        encodeYUV420SPInternal(yuv420, argb, width, height, false);
    }
}
