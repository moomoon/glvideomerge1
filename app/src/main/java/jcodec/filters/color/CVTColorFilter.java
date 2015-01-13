package jcodec.filters.color;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import jcodec.common.model.Picture;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * @author The JCodec project
 *
 */
public class CVTColorFilter {

    public void yuv42210BitTObgr24(Picture yuv, ByteBuffer rgb32) {
        IntBuffer y = IntBuffer.wrap(yuv.getPlaneData(0));
        IntBuffer cb = IntBuffer.wrap(yuv.getPlaneData(1));
        IntBuffer cr = IntBuffer.wrap(yuv.getPlaneData(2));

        while (y.hasRemaining()) {
            int c1 = y.get() - 64;
            int c2 = y.get() - 64;
            int d = cb.get() - 512;
            int e = cr.get() - 512;

            rgb32.put(blue(d, c1));
            rgb32.put(green(d, e, c1));
            rgb32.put(red(e, c1));

            rgb32.put(blue(d, c2));
            rgb32.put(green(d, e, c2));
            rgb32.put(red(e, c2));
        }
    }

    private static byte blue(int d, int c) {
        int blue = (1192 * c + 2064 * d + 512) >> 10;
        blue = blue < 0 ? 0 : (blue > 1023 ? 1023 : blue);
        return (byte)((blue >> 2) & 0xff);
    }

    private static byte green(int d, int e, int c) {
        int green = (1192 * c - 400 * d - 832 * e + 512) >> 10;
        green = green < 0 ? 0 : (green > 1023 ? 1023 : green);
        return (byte)((green >> 2) & 0xff);
    }

    private static byte red(int e, int c) {
        int red = (1192 * c + 1636 * e + 512) >> 10;
        red = red < 0 ? 0 : (red > 1023 ? 1023 : red);
        return (byte)((red >> 2) & 0xff);
    }

}
