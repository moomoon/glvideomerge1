package jcodec.containers.mp4.boxes;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * Creates MP4 file out of a set of samples
 * 
 * @author The JCodec project
 *
 */
public class DataInfoBox extends NodeBox {
    
    public static String fourcc() {
        return "dinf";
    }

    public DataInfoBox() {
        super(new Header(fourcc()));
    }

    public DataInfoBox(Header atom) {
        super(atom);
    }

    public DataRefBox getDref() {
        return findFirst(this, DataRefBox.class, "dref");
    }
}
