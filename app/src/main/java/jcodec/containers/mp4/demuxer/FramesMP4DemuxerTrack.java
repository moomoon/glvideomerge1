package jcodec.containers.mp4.demuxer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import jcodec.common.DemuxerTrackMeta;
import jcodec.common.SeekableByteChannel;
import jcodec.containers.mp4.MP4Packet;
import jcodec.containers.mp4.TrackType;
import jcodec.containers.mp4.boxes.Box;
import jcodec.containers.mp4.boxes.CompositionOffsetsBox;
import jcodec.containers.mp4.boxes.CompositionOffsetsBox.Entry;
import jcodec.containers.mp4.boxes.MovieBox;
import jcodec.containers.mp4.boxes.SampleSizesBox;
import jcodec.containers.mp4.boxes.SyncSamplesBox;
import jcodec.containers.mp4.boxes.TrakBox;

import static jcodec.common.DemuxerTrackMeta.Type.AUDIO;
import static jcodec.common.DemuxerTrackMeta.Type.OTHER;
import static jcodec.common.DemuxerTrackMeta.Type.VIDEO;
import static jcodec.containers.mp4.QTTimeUtil.mediaToEdited;
import static jcodec.containers.mp4.boxes.Box.findFirst;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Regular MP4 track containing frames
 * 
 * @author The JCodec project
 * 
 */
public class FramesMP4DemuxerTrack extends AbstractMP4DemuxerTrack {

    private int[] sizes;

    private long offInChunk;

    private int noInChunk;

    private int[] syncSamples;
    private int ssOff;

    private Entry[] compOffsets;
    private int cttsInd;
    private int cttsSubInd;

    private SeekableByteChannel input;

    private MovieBox movie;

    public FramesMP4DemuxerTrack(MovieBox mov, TrakBox trak, SeekableByteChannel input) {
        super(trak);
        this.input = input;
        this.movie = mov;

        SampleSizesBox stsz = findFirst(trak, SampleSizesBox.class, "mdia", "minf", "stbl", "stsz");
        SyncSamplesBox stss = Box.findFirst(trak, SyncSamplesBox.class, "mdia", "minf", "stbl", "stss");
        CompositionOffsetsBox ctts = Box.findFirst(trak, CompositionOffsetsBox.class, "mdia", "minf", "stbl", "ctts");
        compOffsets = ctts == null ? null : ctts.getEntries();
        if (stss != null) {
            syncSamples = stss.getSyncSamples();
        }
        sizes = stsz.getSizes();
    }

    public synchronized MP4Packet nextFrame() throws IOException {
        if (curFrame >= sizes.length)
            return null;
        int size = sizes[(int) curFrame];

        return nextFrame(ByteBuffer.allocate(size));
    }

    public synchronized MP4Packet nextFrame(ByteBuffer storage) throws IOException {

        if (curFrame >= sizes.length)
            return null;
        int size = sizes[(int) curFrame];

        if (storage != null && storage.remaining() < size) {
            throw new IllegalArgumentException("Buffer size is not enough to fit a packet");
        }

        long pktPos = chunkOffsets[stcoInd] + offInChunk;

        ByteBuffer result = readPacketData(input, storage, pktPos, size);

        if (result != null && result.remaining() < size)
            return null;

        int duration = timeToSamples[sttsInd].getSampleDuration();

        boolean sync = syncSamples == null;

        if (syncSamples != null && ssOff < syncSamples.length && (curFrame + 1) == syncSamples[ssOff]) {
            sync = true;
            ssOff++;
        }

        long realPts = pts;
        if (compOffsets != null) {
            realPts = pts + compOffsets[cttsInd].getOffset();
            cttsSubInd++;
            if (cttsInd < compOffsets.length - 1 && cttsSubInd == compOffsets[cttsInd].getCount()) {
                cttsInd++;
                cttsSubInd = 0;
            }
        }

        MP4Packet pkt = new MP4Packet(result, mediaToEdited(box, realPts, movie.getTimescale()), timescale, duration,
                curFrame, sync, null, realPts, sampleToChunks[stscInd].getEntry() - 1, pktPos, size);

        offInChunk += size;

        curFrame++;
        noInChunk++;
        if (noInChunk >= sampleToChunks[stscInd].getCount()) {
            noInChunk = 0;
            offInChunk = 0;

            nextChunk();
        }
        shiftPts(1);
        sttsSubInd++;

        return pkt;
    }

    protected void seekPointer(long frameNo) {
        if (compOffsets != null) {
            cttsSubInd = (int) frameNo;
            cttsInd = 0;
            while (cttsSubInd >= compOffsets[cttsInd].getCount()) {
                cttsSubInd -= compOffsets[cttsInd].getCount();
                cttsInd++;
            }
        }

        curFrame = (int) frameNo;
        stcoInd = 0;
        stscInd = 0;
        noInChunk = (int) frameNo;
        offInChunk = 0;

        while (noInChunk >= sampleToChunks[stscInd].getCount()) {
            noInChunk -= sampleToChunks[stscInd].getCount();

            nextChunk();
        }

        for (int i = 0; i < noInChunk; i++) {
            offInChunk += sizes[(int) frameNo - noInChunk + i];
        }

        if (syncSamples != null)
            for (ssOff = 0; ssOff < syncSamples.length && syncSamples[ssOff] < curFrame + 1; ssOff++)
                ;

    }

    public long getFrameCount() {
        return sizes.length;
    }

    @Override
    public DemuxerTrackMeta getMeta() {
        int[] copyOf = Arrays.copyOf(syncSamples, syncSamples.length);
        for (int i = 0; i < copyOf.length; i++)
            copyOf[i]--;

        TrackType type = getType();
        return new DemuxerTrackMeta(type == TrackType.VIDEO ? VIDEO : (type == TrackType.SOUND ? AUDIO : OTHER),
                copyOf, sizes.length, (double) duration / timescale, box.getCodedSize());
    }
}