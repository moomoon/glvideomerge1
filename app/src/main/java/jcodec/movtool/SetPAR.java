package jcodec.movtool;

import java.io.File;

import jcodec.common.model.Rational;
import jcodec.common.model.Size;
import jcodec.containers.mp4.boxes.Box;
import jcodec.containers.mp4.boxes.MovieBox;
import jcodec.containers.mp4.boxes.NodeBox;
import jcodec.containers.mp4.boxes.SampleDescriptionBox;
import jcodec.containers.mp4.boxes.TrakBox;
import jcodec.containers.mp4.boxes.VideoSampleEntry;

public class SetPAR {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Syntax: setpasp <movie> <num:den>");
            System.exit(-1);
        }
        final Rational newPAR = Rational.parse(args[1]);

        new InplaceEdit() {
            protected void apply(MovieBox mov) {
                TrakBox vt = mov.getVideoTrack();
                vt.setPAR(newPAR);
                Box box = NodeBox.findFirst(vt, SampleDescriptionBox.class, "mdia", "minf", "stbl", "stsd").getBoxes()
                        .get(0);
                if (box != null && (box instanceof VideoSampleEntry)) {
                    VideoSampleEntry vs = (VideoSampleEntry) box;
                    int codedWidth = (int) vs.getWidth();
                    int codedHeight = (int) vs.getHeight();
                    int displayWidth = codedWidth * newPAR.getNum() / newPAR.getDen();

                    vt.getTrackHeader().setWidth(displayWidth);

                    Box tapt = Box.findFirst(vt, "tapt");
                    if (tapt != null) {
                        vt.setAperture(new Size(codedWidth, codedHeight), new Size(displayWidth, codedHeight));
                    }
                }
            }
        }.save(new File(args[0]));
    }
}
