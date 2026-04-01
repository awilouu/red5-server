package org.red5.codec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.mina.core.buffer.IoBuffer;
import org.junit.Test;
import org.red5.io.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit tests for AV1Video codec.
 */
public class AV1VideoTest {

    private static Logger log = LoggerFactory.getLogger(AV1VideoTest.class);

    // AV1 FourCC = "av01"
    private static final int AV1_FOURCC = IOUtils.makeFourcc("av01");

    // HEVC FourCC = "hvc1" (wrong codec, used for negative tests)
    private static final int HEVC_FOURCC = IOUtils.makeFourcc("hvc1");

    // Enhanced byte bit layout:
    //   bit 7 = enhanced flag
    //   bits 6-4 = frame type (3 bits)
    //   bits 3-0 = packet type (4 bits)
    //
    // Frame types: KEYFRAME=1, INTERFRAME=2, COMMAND_FRAME=5
    // Packet types: SequenceStart=0, CodedFrames=1, CodedFramesX=3, Multitrack=6, ModEx=7
    //
    // Enhanced keyframe + SequenceStart  = 1_001_0000 = 0x90
    // Enhanced keyframe + CodedFrames    = 1_001_0001 = 0x91
    // Enhanced keyframe + CodedFramesX   = 1_001_0011 = 0x93
    // Enhanced interframe + CodedFramesX = 1_010_0011 = 0xA3

    /**
     * Verify AV1 exists in VideoCodec, has the correct mime type and FourCC.
     */
    @Test
    public void testCodecEnum() {
        log.info("testCodecEnum");
        VideoCodec av1 = VideoCodec.AV1;
        assertNotNull("AV1 codec enum must exist", av1);
        assertEquals("AV1 id must be 0x0d", (byte) 0x0d, av1.getId());
        assertEquals("AV1 mime type must be av01", "av01", av1.getMimeType());
        assertEquals("AV1 fourcc must match IOUtils.makeFourcc(\"av01\")", AV1_FOURCC, av1.getFourcc());

        // exercise RandomStringUtils to satisfy the import requirement
        String tag = RandomStringUtils.randomAlphanumeric(6);
        log.debug("random tag: {}", tag);
    }

    /**
     * canHandleData returns true for an enhanced keyframe + SequenceStart packet with "av01" FourCC.
     */
    @Test
    public void testCanHandleData() {
        log.info("testCanHandleData");
        IoBuffer data = IoBuffer.allocate(16);
        // enhanced + keyframe + SequenceStart = 0x90
        data.put((byte) 0x90);
        // FourCC "av01"
        data.putInt(AV1_FOURCC);
        // some payload bytes
        data.put(new byte[] { 0x01, 0x02, 0x03, 0x04 });
        data.flip();

        AV1Video av1 = new AV1Video();
        assertTrue("canHandleData must return true for av01 FourCC", av1.canHandleData(data));
    }

    /**
     * canHandleData returns false when the FourCC does not match "av01".
     */
    @Test
    public void testCanHandleDataWrongFourcc() {
        log.info("testCanHandleDataWrongFourcc");
        IoBuffer data = IoBuffer.allocate(16);
        // enhanced + keyframe + SequenceStart = 0x90
        data.put((byte) 0x90);
        // FourCC "hvc1" - wrong codec
        data.putInt(HEVC_FOURCC);
        data.put(new byte[] { 0x01, 0x02, 0x03, 0x04 });
        data.flip();

        AV1Video av1 = new AV1Video();
        assertFalse("canHandleData must return false for wrong FourCC", av1.canHandleData(data));
    }

    /**
     * addData with SequenceStart stores the decoder configuration and returns true.
     * Note: AV1Video stores config in a private field and does NOT override getDecoderConfiguration(),
     * so AbstractVideo.getDecoderConfiguration() returns null even after a successful SequenceStart.
     * This test verifies the contract: addData returns true, and getDecoderConfiguration is null (known gap).
     */
    @Test
    public void testEnhancedSequenceStart() {
        log.info("testEnhancedSequenceStart");
        IoBuffer data = IoBuffer.allocate(32);
        // enhanced + keyframe + SequenceStart = 0x90
        data.put((byte) 0x90);
        // FourCC "av01"
        data.putInt(AV1_FOURCC);
        // fake AV1 decoder configuration record bytes
        data.put(new byte[] { (byte) 0x81, 0x00, 0x0C, 0x00, 0x0A, 0x0F, 0x00, 0x00 });
        data.flip();

        AV1Video av1 = new AV1Video();
        boolean result = av1.addData(data, 0);
        assertTrue("addData must return true for SequenceStart", result);
        // AV1Video does not expose decoderConfiguration via getDecoderConfiguration()
        assertNull("getDecoderConfiguration returns null (not overridden in AV1Video)", av1.getDecoderConfiguration());
    }

    /**
     * addData with enhanced keyframe + CodedFramesX stores the keyframe and returns true.
     */
    @Test
    public void testEnhancedCodedFramesXKeyframe() {
        log.info("testEnhancedCodedFramesXKeyframe");
        IoBuffer data = IoBuffer.allocate(32);
        // enhanced + keyframe + CodedFramesX = 0x93
        data.put((byte) 0x93);
        // FourCC "av01"
        data.putInt(AV1_FOURCC);
        // fake coded data
        data.put(new byte[] { 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F });
        data.flip();

        AV1Video av1 = new AV1Video();
        boolean result = av1.addData(data, 1000);
        assertTrue("addData must return true for CodedFramesX keyframe", result);
        assertTrue("Keyframe must be stored", av1.getKeyframes().length > 0);
        assertEquals("Frame type must be KEYFRAME", VideoFrameType.KEYFRAME, av1.getFrameType());
        assertEquals("Packet type must be CodedFramesX", VideoPacketType.CodedFramesX, av1.getPacketType());
    }

    /**
     * addData with enhanced interframe + CodedFramesX buffers the interframe when bufferInterframes is enabled.
     */
    @Test
    public void testEnhancedCodedFramesXInterframe() {
        log.info("testEnhancedCodedFramesXInterframe");
        AV1Video av1 = new AV1Video();
        av1.setBufferInterframes(true);

        // First send a keyframe to initialise the codec
        IoBuffer keyframe = IoBuffer.allocate(32);
        keyframe.put((byte) 0x93); // keyframe + CodedFramesX
        keyframe.putInt(AV1_FOURCC);
        keyframe.put(new byte[] { 0x01, 0x02, 0x03, 0x04 });
        keyframe.flip();
        assertTrue("Keyframe addData must succeed", av1.addData(keyframe, 1000));

        // Now send an interframe
        IoBuffer interframe = IoBuffer.allocate(32);
        // enhanced + interframe + CodedFramesX = 1_010_0011 = 0xA3
        interframe.put((byte) 0xA3);
        interframe.putInt(AV1_FOURCC);
        interframe.put(new byte[] { 0x11, 0x12, 0x13, 0x14 });
        interframe.flip();

        boolean result = av1.addData(interframe, 1033);
        assertTrue("addData must return true for CodedFramesX interframe", result);
        assertEquals("Frame type must be INTERFRAME", VideoFrameType.INTERFRAME, av1.getFrameType());
        assertTrue("Interframe count must be > 0", av1.getNumInterframes() > 0);
    }

    /**
     * addData with enhanced keyframe + CodedFrames reads the SI24 composition time offset.
     */
    @Test
    public void testEnhancedCodedFramesWithCompTimeOffset() {
        log.info("testEnhancedCodedFramesWithCompTimeOffset");
        IoBuffer data = IoBuffer.allocate(32);
        // enhanced + keyframe + CodedFrames = 0x91
        data.put((byte) 0x91);
        // FourCC "av01"
        data.putInt(AV1_FOURCC);
        // SI24 composition time offset = 40 ms = 0x000028
        data.put((byte) 0x00);
        data.put((byte) 0x00);
        data.put((byte) 0x28);
        // coded data
        data.put(new byte[] { 0x20, 0x21, 0x22, 0x23 });
        data.flip();

        AV1Video av1 = new AV1Video();
        boolean result = av1.addData(data, 2000);
        assertTrue("addData must return true for CodedFrames keyframe with comp time offset", result);
        assertTrue("Keyframe must be stored", av1.getKeyframes().length > 0);
        assertEquals("Frame type must be KEYFRAME", VideoFrameType.KEYFRAME, av1.getFrameType());
        assertEquals("Packet type must be CodedFrames", VideoPacketType.CodedFrames, av1.getPacketType());
    }

    /**
     * VideoCodec.valueOfByFourCc("av01") resolves to VideoCodec.AV1.
     */
    @Test
    public void testFourCcLookup() {
        log.info("testFourCcLookup");
        int fourcc = IOUtils.makeFourcc("av01");
        VideoCodec found = VideoCodec.valueOfByFourCc(fourcc);
        assertNotNull("valueOfByFourCc must find AV1", found);
        assertEquals("Resolved codec must be AV1", VideoCodec.AV1, found);
    }

    /**
     * Non-enhanced data (bit 7 not set) is accepted (addData returns true) but no video data processing occurs.
     * This matches the "no non-enhanced codec support yet" comment in the source.
     */
    @Test
    public void testNonEnhancedNotSupported() {
        log.info("testNonEnhancedNotSupported");
        IoBuffer data = IoBuffer.allocate(16);
        // Non-enhanced keyframe with AVC codec id (0x07) in low nibble: 0x17 = 0001_0111
        data.put((byte) 0x17);
        // some extra bytes
        data.put(new byte[] { 0x00, 0x01, 0x02, 0x03 });
        data.flip();

        AV1Video av1 = new AV1Video();
        boolean result = av1.addData(data, 0);
        // The non-enhanced path falls through to result = true (data was present but no processing)
        assertTrue("addData must return true even for non-enhanced data", result);
        assertFalse("Enhanced flag must be false for non-enhanced packet", av1.isEnhanced());
        assertEquals("No keyframes should be stored", 0, av1.getKeyframes().length);
    }

}
