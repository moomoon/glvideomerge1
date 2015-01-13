package jcodec.codecs.aac.blocks;

import jcodec.common.io.BitReader;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class BlockFil extends Block {

    public void parse(BitReader in) {
        int num = (int) in.readNBit(4);
        if (num == 15)
            num += (int) in.readNBit(8) - 1;
        if (num > 0)
            if (in.skip(8 * num) != 8 * num)
                throw new RuntimeException("Overread");
    }
}
