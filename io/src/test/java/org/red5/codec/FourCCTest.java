package org.red5.codec;

import static org.junit.Assert.assertEquals;

import java.nio.ByteOrder;

import org.apache.mina.core.buffer.IoBuffer;
import org.junit.Test;
import org.red5.io.utils.IOUtils;

public class FourCCTest {

    @Test
    public void test() {
        VideoCodec[] values = VideoCodec.values();
        for (VideoCodec codec : values) {
            int fourcc = codec.getFourcc();
            System.out.println(codec.name() + " fourcc: " + fourcc + " mimeType: " + codec.getMimeType() + " fourcc from mime: " + IOUtils.makeFourcc(codec.getMimeType()));
            IoBuffer buffer = IoBuffer.allocate(4);
            buffer.order(ByteOrder.BIG_ENDIAN); // native order is BIG_ENDIAN in linux
            buffer.putInt(fourcc);
            buffer.flip();
            System.out.println("fourcc bytes: " + buffer.getHexDump());
        }
        AudioCodec[] audioValues = AudioCodec.values();
        for (AudioCodec codec : audioValues) {
            int fourcc = codec.getFourcc();
            System.out.println(codec.name() + " fourcc: " + fourcc + " mimeType: " + codec.getMimeType() + " fourcc from mime: " + IOUtils.makeFourcc(codec.getMimeType()));
            IoBuffer buffer = IoBuffer.allocate(4);
            buffer.order(ByteOrder.BIG_ENDIAN); // native order is BIG_ENDIAN in linux
            buffer.putInt(fourcc);
            buffer.flip();
            System.out.println("fourcc bytes: " + buffer.getHexDump());
        }
    }

    @Test
    public void testVVCFourCC() {
        int fourcc = IOUtils.makeFourcc("vvc1");
        assertEquals(VideoCodec.VVC.getFourcc(), fourcc);
    }

}
