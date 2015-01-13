package jcodec.containers.mkv;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import jcodec.containers.mkv.ebml.BinaryElement;
import jcodec.containers.mkv.ebml.Element;
import jcodec.containers.mkv.ebml.MasterElement;
import jcodec.containers.mkv.ebml.UnsignedIntegerElement;

import static jcodec.containers.mkv.Type.Seek;
import static jcodec.containers.mkv.Type.SeekHead;
import static jcodec.containers.mkv.Type.SeekID;
import static jcodec.containers.mkv.Type.SeekPosition;
import static jcodec.containers.mkv.ebml.Element.getEbmlSize;
import static jcodec.containers.mkv.ebml.UnsignedIntegerElement.getMinByteSizeUnsigned;

public class SeekHeadIndexer {
    List<SeekMock> a = new ArrayList<SeekMock>();
    long currentDataOffset = 0;


    public void add(Element e) {
        SeekMock z = SeekMock.make(e);
        z.dataOffset = currentDataOffset;
        z.seekPointerSize = getMinByteSizeUnsigned(z.dataOffset);
        currentDataOffset += z.size;
//        System.out.println("Added id:"+Reader.printAsHex(z.id)+" offset:"+z.dataOffset+" size:"+z.size+" seekpointer size:"+z.seekPointerSize);
        a.add(z);
    }

    public MasterElement indexSeekHead(){
        int seekHeadSize = computeSeekHeadSize();

        MasterElement seekHead = Type.createElementByType(SeekHead);
        for(SeekMock z : a){
            MasterElement seek = Type.createElementByType(Seek);

            BinaryElement seekId = Type.createElementByType(SeekID);
            seekId.setData(z.id);
            seek.addChildElement(seekId);

            UnsignedIntegerElement seekPosition = Type.createElementByType(Type.SeekPosition);
            seekPosition.set(z.dataOffset+seekHeadSize);
            if (seekPosition.getData().length != z.seekPointerSize)
                System.err.println("estimated size of seekPosition differs from the one actually used. ElementId: "+Reader.printAsHex(z.id)+" "+seekPosition.getData().length+" vs "+z.seekPointerSize);
            seek.addChildElement(seekPosition);

            seekHead.addChildElement(seek);
        }
        ByteBuffer mux = seekHead.mux();
        if (mux.limit() != seekHeadSize)
            System.err.println("estimated size of seekHead differs from the one actually used. "+mux.limit()+" vs "+seekHeadSize);

        return seekHead;
    }

    public int computeSeekHeadSize() {
        int seekHeadSize = estimateSize();
        boolean reindex = false;
        do {
            reindex = false;
            for (SeekMock z : a) {
                int minSize = getMinByteSizeUnsigned(z.dataOffset + seekHeadSize);
                if (minSize > z.seekPointerSize) {
                    System.out.println("Size "+seekHeadSize+" seems too small for element "+Reader.printAsHex(z.id)+" increasing size by one.");
                    z.seekPointerSize +=1;
                    seekHeadSize += 1;
                    reindex = true;
                    break;
                } else if (minSize < z.seekPointerSize) {
                    throw new RuntimeException("Downsizing the index is not well thought through.");
                }
            }
        } while (reindex);
        return seekHeadSize;
    }

    int estimateSize() {
        int s = Type.SeekHead.id.length + 1;
        s += estimeteSeekSize(a.get(0).id.length, 1);
        for (int i = 1; i < a.size(); i++) {
            s += estimeteSeekSize(a.get(i).id.length, a.get(i).seekPointerSize);
        }
        return s;
    }

    public static int estimeteSeekSize(int idLength, int offsetSizeInBytes) {
        int seekIdSize = SeekID.id.length + getEbmlSize(idLength) + idLength;
        int seekPositionSize = SeekPosition.id.length + getEbmlSize(offsetSizeInBytes) + offsetSizeInBytes;
        int seekSize = Seek.id.length + getEbmlSize(seekIdSize + seekPositionSize) + seekIdSize + seekPositionSize;
        return seekSize;
    }

    public static class SeekMock {
        public long dataOffset;
        byte[] id;
        int size;
        int seekPointerSize;

        public static SeekMock make(Element e) {
            SeekMock z = new SeekMock();
            z.id = e.id;
            z.size = (int) e.getSize();
            return z;
        }
    }
}