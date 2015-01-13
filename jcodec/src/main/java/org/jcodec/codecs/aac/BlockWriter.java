package jcodec.codecs.aac;

import static jcodec.codecs.aac.BlockType.TYPE_END;

import jcodec.codecs.aac.blocks.Block;
import jcodec.common.io.BitWriter;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Writes blocks to form AAC frame
 * 
 * @author The JCodec project
 * 
 */
public class BlockWriter {
    
    public void nextBlock(BitWriter bits, Block block) {
        bits.writeNBit(block.getType().getCode(), 3);
        
        if (block.getType() == TYPE_END)
            return;
        
    }

}
