package jcodec.common;

import android.graphics.Bitmap;

import jcodec.common.model.ColorSpace;
import jcodec.common.model.Picture;
import jcodec.scale.BitmapUtil;
import jcodec.scale.ColorUtil;
import jcodec.scale.Transform;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class AndroidUtil {

	public static Bitmap toBitmap(Picture pic) {
		Transform transform = ColorUtil.getTransform(pic.getColor(),
				ColorSpace.RGB);
		Picture rgb = Picture.create(pic.getWidth(), pic.getHeight(),
				ColorSpace.RGB, pic.getCrop());
		transform.transform(pic, rgb);
		return BitmapUtil.toBitmap(rgb);
	}
	
	public static void toBitmap(Picture pic, Bitmap out) {
		Transform transform = ColorUtil.getTransform(pic.getColor(),
				ColorSpace.RGB);
		Picture rgb = Picture.create(pic.getWidth(), pic.getHeight(),
				ColorSpace.RGB, pic.getCrop());
		transform.transform(pic, rgb);
		BitmapUtil.toBitmap(rgb, out);
	}
}