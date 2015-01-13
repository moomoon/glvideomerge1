package jcodec.codecs.aac;

import jcodec.codecs.aac.blocks.Block;
import jcodec.common.io.BitReader;

import static jcodec.codecs.aac.BlockType.TYPE_END;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Reads blocks of AAC frame
 * 
 * @author The JCodec project
 * 
 */
public class BlockReader {

    public Block nextBlock(BitReader bits) {
        BlockType type = BlockType.fromCode(bits.readNBit(3));
        if (type == TYPE_END)
            return null;

        int id = (int) bits.readNBit(4);

        return null;
    }
}
