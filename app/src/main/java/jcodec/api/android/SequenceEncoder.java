package jcodec.api.android;

import android.graphics.Bitmap;

import java.io.File;
import java.io.IOException;

import jcodec.scale.BitmapUtil;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class SequenceEncoder extends jcodec.api.SequenceEncoder {

	public SequenceEncoder(File out) throws IOException {
		super(out);
	}

	public void encodeImage(Bitmap bi) throws IOException {
        encodeNativeFrame(BitmapUtil.fromBitmap(bi));
    }
}