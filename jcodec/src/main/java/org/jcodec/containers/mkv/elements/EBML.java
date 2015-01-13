package jcodec.containers.mkv.elements;

import java.util.Arrays;

import jcodec.containers.mkv.Type;
import jcodec.containers.mkv.ebml.Element;
import jcodec.containers.mkv.ebml.MasterElement;
import jcodec.containers.mkv.ebml.StringElement;

public class EBML extends MasterElement {

    public EBML(byte[] typeId) {
        super(typeId);
        assert Arrays.equals(Type.EBML.id, typeId);
    }

    @Override
    public void addChildElement(Element elem) {
        if (elem.isSameMatroskaType(Type.DocType)) {
            String DocType = ((StringElement) elem).get();
            if (DocType.compareTo("matroska") != 0 && DocType.compareTo("webm") != 0) {
                throw new java.lang.RuntimeException("Error: DocType is not matroska, \"" + ((StringElement) elem).get() + "\"");
            }
        }
        super.addChildElement(elem);
    }

    
}
