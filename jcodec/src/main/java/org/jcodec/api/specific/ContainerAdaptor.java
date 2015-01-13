package jcodec.api.specific;

import jcodec.api.FrameGrab.MediaInfo;
import jcodec.common.model.Packet;
import jcodec.common.model.Picture;

public interface ContainerAdaptor {

    Picture decodeFrame(Packet packet, int[][] data);

    boolean canSeek(Packet data);
    
    int[][] allocatePicture();

	MediaInfo getMediaInfo();
    
}
