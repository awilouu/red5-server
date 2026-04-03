/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2023 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.codec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.mina.core.buffer.IoBuffer;
import org.junit.Test;
import org.red5.io.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VVCVideoTest {

    private static Logger log = LoggerFactory.getLogger(VVCVideoTest.class);

    @Test
    public void testVVCCodecEnum() {
        log.info("testVVCCodecEnum");
        VideoCodec vvc = VideoCodec.VVC;
        assertNotNull(vvc);
        assertEquals("vvc1", vvc.getMimeType());
        assertEquals(IOUtils.makeFourcc("vvc1"), vvc.getFourcc());
        log.info("testVVCCodecEnum end\n");
    }

    @Test
    public void testVVCVideoInstance() {
        log.info("testVVCVideoInstance");
        VVCVideo video = new VVCVideo();
        assertNotNull(video);
        assertEquals(VideoCodec.VVC, video.getCodec());
        assertFalse(video.isEnhanced());
        assertTrue(video.canDropFrames());
        log.info("testVVCVideoInstance end\n");
    }

    @Test
    public void testCanHandleDataEnhanced() {
        log.info("testCanHandleDataEnhanced");
        // Build enhanced keyframe IoBuffer: 0x90 flag + "vvc1" FourCC
        // 0x90 = 1001 0000 => enhanced bit set (1), frame type = KEYFRAME (001), packet type = SequenceStart (0000)
        IoBuffer data = IoBuffer.allocate(9);
        data.put((byte) 0x90);
        data.put((byte) 'v');
        data.put((byte) 'v');
        data.put((byte) 'c');
        data.put((byte) '1');
        data.putInt(0);
        data.flip();

        VVCVideo video = new VVCVideo();
        assertTrue(video.canHandleData(data));
        assertTrue(video.isEnhanced());
        log.info("testCanHandleDataEnhanced end\n");
    }

    @Test
    public void testCanHandleDataWrongFourcc() {
        log.info("testCanHandleDataWrongFourcc");
        // Build enhanced frame with "hvc1" FourCC - should not be handled by VVCVideo
        IoBuffer data = IoBuffer.allocate(9);
        data.put((byte) 0x90);
        data.put((byte) 'h');
        data.put((byte) 'v');
        data.put((byte) 'c');
        data.put((byte) '1');
        data.putInt(0);
        data.flip();

        VVCVideo video = new VVCVideo();
        assertFalse(video.canHandleData(data));
        log.info("testCanHandleDataWrongFourcc end\n");
    }

    @Test
    public void testEnhancedSequenceStart() {
        log.info("testEnhancedSequenceStart");
        // 0x90 = enhanced bit + KEYFRAME (001) + SequenceStart (0000)
        byte[] fakeConfig = RandomStringUtils.secure().next(16).getBytes();
        IoBuffer data = IoBuffer.allocate(5 + fakeConfig.length);
        data.put((byte) 0x90);
        data.put((byte) 'v');
        data.put((byte) 'v');
        data.put((byte) 'c');
        data.put((byte) '1');
        data.put(fakeConfig);
        data.flip();

        VVCVideo video = new VVCVideo();
        assertTrue(video.canHandleData(data));
        data.rewind();
        assertTrue(video.addData(data, 1000));
        assertNotNull(video.getDecoderConfiguration());
        log.info("testEnhancedSequenceStart end\n");
    }

    @Test
    public void testEnhancedCodedFramesX() {
        log.info("testEnhancedCodedFramesX");
        // 0x93 = enhanced bit (1) + KEYFRAME (001) + CodedFramesX (0011)
        byte[] fakeData = RandomStringUtils.secure().next(16).getBytes();
        IoBuffer data = IoBuffer.allocate(5 + fakeData.length);
        data.put((byte) 0x93);
        data.put((byte) 'v');
        data.put((byte) 'v');
        data.put((byte) 'c');
        data.put((byte) '1');
        data.put(fakeData);
        data.flip();

        VVCVideo video = new VVCVideo();
        assertTrue(video.canHandleData(data));
        data.rewind();
        assertTrue(video.addData(data, 2000));
        log.info("testEnhancedCodedFramesX end\n");
    }

    @Test
    public void testEnhancedCodedFramesWithCompTimeOffset() {
        log.info("testEnhancedCodedFramesWithCompTimeOffset");
        // 0x91 = enhanced bit (1) + KEYFRAME (001) + CodedFrames (0001)
        // CodedFrames includes a 3-byte SI24 composition time offset before the payload
        byte[] fakeData = RandomStringUtils.secure().next(16).getBytes();
        IoBuffer data = IoBuffer.allocate(5 + 3 + fakeData.length);
        data.put((byte) 0x91);
        data.put((byte) 'v');
        data.put((byte) 'v');
        data.put((byte) 'c');
        data.put((byte) '1');
        // SI24 composition time offset = 33 ms
        data.put((byte) 0x00);
        data.put((byte) 0x00);
        data.put((byte) 0x21);
        data.put(fakeData);
        data.flip();

        VVCVideo video = new VVCVideo();
        assertTrue(video.canHandleData(data));
        data.rewind();
        assertTrue(video.addData(data, 3000));
        log.info("testEnhancedCodedFramesWithCompTimeOffset end\n");
    }

    @Test
    public void testFourCcLookup() {
        log.info("testFourCcLookup");
        int fourcc = IOUtils.makeFourcc("vvc1");
        assertEquals(VideoCodec.VVC, VideoCodec.valueOfByFourCc(fourcc));
        log.info("testFourCcLookup end\n");
    }

}
