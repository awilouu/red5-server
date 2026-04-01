package org.red5.codec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.mina.core.buffer.IoBuffer;
import org.junit.Test;
import org.red5.io.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModExTest {

    private static Logger log = LoggerFactory.getLogger(ModExTest.class);

    /**
     * Test that a video packet with ModEx wrapping a CodedFramesX keyframe is correctly unwrapped.
     * Wire format:
     *   byte 0: 1_001_0111 = 0x97 (enhanced + keyframe + ModEx)
     *   bytes 1-4: FourCC 'hvc1'
     *   byte 5: modExDataSize - 1 = 0x02 (so size = 3 bytes)
     *   bytes 6-8: modExData = UI24 nanosecond offset (0x000FA0 = 4000 ns)
     *   byte 9: upper nibble = TimestampOffsetNano (0), lower nibble = CodedFramesX (3) -> 0x03
     *   bytes 10+: coded video data
     */
    @Test
    public void testVideoModExTimestampNanoUnwrap() {
        log.info("testVideoModExTimestampNanoUnwrap");
        IoBuffer data = IoBuffer.allocate(32);
        // enhanced + keyframe + ModEx (0x07)
        data.put((byte) 0x97); // 1_001_0111
        // FourCC for HEVC
        data.put((byte) 'h');
        data.put((byte) 'v');
        data.put((byte) 'c');
        data.put((byte) '1');
        // ModEx: size-1 = 2 (means 3 bytes of modExData)
        data.put((byte) 0x02);
        // modExData: UI24 = 4000 nanoseconds
        data.put((byte) 0x00);
        data.put((byte) 0x0F);
        data.put((byte) 0xA0);
        // Next byte: upper nibble = TimestampOffsetNano (0), lower nibble = CodedFramesX (3)
        data.put((byte) 0x03);
        // Fake coded data
        data.put(new byte[] { 0x01, 0x02, 0x03, 0x04 });
        data.flip();

        HEVCVideo video = new HEVCVideo();
        // addData should unwrap ModEx and handle the inner CodedFramesX
        assertTrue(video.addData(data, 5000));
        // After unwrap, the effective packet type should be CodedFramesX
        assertEquals(VideoPacketType.CodedFramesX, video.getPacketType());
    }

    /**
     * Test ModEx with large data size (256 triggers UI16 read).
     */
    @Test
    public void testVideoModExLargeDataSize() {
        log.info("testVideoModExLargeDataSize");
        IoBuffer data = IoBuffer.allocate(300);
        // enhanced + keyframe + ModEx
        data.put((byte) 0x97);
        // FourCC for HEVC
        data.put((byte) 'h');
        data.put((byte) 'v');
        data.put((byte) 'c');
        data.put((byte) '1');
        // ModEx: UI8 = 0xFF -> size = 256, triggers UI16 read
        data.put((byte) 0xFF);
        // UI16 = 0x0002 -> actual size = 3
        data.put((byte) 0x00);
        data.put((byte) 0x02);
        // modExData: 3 bytes (UI24 nano offset)
        data.put((byte) 0x00);
        data.put((byte) 0x00);
        data.put((byte) 0x64); // 100 ns
        // Next byte: TimestampOffsetNano (0) + SequenceStart (0) = 0x00
        data.put((byte) 0x00);
        // Fake config data
        data.put(new byte[] { 0x01, 0x02, 0x03, 0x04 });
        data.flip();

        HEVCVideo video = new HEVCVideo();
        assertTrue(video.addData(data, 0));
        assertEquals(VideoPacketType.SequenceStart, video.getPacketType());
    }

    /**
     * Test an enhanced command frame (COMMAND_FRAME frameType + any non-Metadata packetType).
     * Command frames do NOT carry a FourCC — the byte immediately after the flag byte is the UI8 VideoCommand.
     *
     * Flag byte composition:
     *   bit 7      = 1 (enhanced)
     *   bits 6-4   = 101 (COMMAND_FRAME = 5)
     *   bits 3-0   = 0000 (SequenceStart = 0, any non-Metadata type works)
     *   => 1_101_0000 = 0xD0
     *
     * Followed by UI8 command: 0x00 = START_SEEK, 0x01 = END_SEEK
     */
    @Test
    public void testVideoCommandFrame() {
        log.info("testVideoCommandFrame");
        IoBuffer data = IoBuffer.allocate(8);
        // enhanced + COMMAND_FRAME + SequenceStart = 0xD0
        data.put((byte) 0xD0);
        // VideoCommand: START_SEEK = 0
        data.put((byte) 0x00);
        data.flip();

        HEVCVideo video = new HEVCVideo();
        // Command frames have no FourCC — addData must return true
        boolean result = video.addData(data, 0);
        assertTrue("addData must return true for a command frame", result);
        // Frame type should be COMMAND_FRAME
        assertEquals("Frame type must be COMMAND_FRAME", VideoFrameType.COMMAND_FRAME, video.getFrameType());

        // Also test END_SEEK command
        IoBuffer data2 = IoBuffer.allocate(8);
        data2.put((byte) 0xD0);
        data2.put((byte) 0x01); // END_SEEK
        data2.flip();

        HEVCVideo video2 = new HEVCVideo();
        assertTrue("addData must return true for END_SEEK command frame", video2.addData(data2, 0));
    }

    /**
     * Test multitrack video with OneTrack mode.
     *
     * Wire format:
     *   byte 0: 1_001_0110 = 0x96 (enhanced + keyframe + Multitrack=6)
     *   byte 1: upper nibble = OneTrack(0), lower nibble = CodedFramesX(3) -> 0x03
     *   bytes 2-5: FourCC "hvc1"
     *   byte 6: trackId = 0
     *   bytes 7+: coded data (no size field for OneTrack)
     */
    @Test
    public void testMultitrackVideoOneTrack() {
        log.info("testMultitrackVideoOneTrack");
        int hvcFourcc = IOUtils.makeFourcc("hvc1");

        IoBuffer data = IoBuffer.allocate(32);
        // enhanced + keyframe + Multitrack = 0x96
        data.put((byte) 0x96);
        // OneTrack(0) << 4 | CodedFramesX(3) = 0x03
        data.put((byte) 0x03);
        // FourCC "hvc1"
        data.putInt(hvcFourcc);
        // trackId = 0
        data.put((byte) 0x00);
        // coded video data
        data.put(new byte[] { 0x10, 0x20, 0x30, 0x40 });
        data.flip();

        HEVCVideo video = new HEVCVideo();
        boolean result = video.addData(data, 3000);
        assertTrue("addData must return true for OneTrack multitrack", result);
        assertTrue("Multitrack codec must report enhanced", video.isEnhanced());
        assertNotNull("Track codec must be resolved", video.getTrackCodec(0));
    }

    /**
     * Test multitrack video with ManyTracks mode (same FourCC for all tracks).
     *
     * Wire format:
     *   byte 0: 0x96 (enhanced + keyframe + Multitrack)
     *   byte 1: ManyTracks(1) << 4 | CodedFramesX(3) = 0x13
     *   bytes 2-5: FourCC "hvc1" (shared across tracks)
     *   Track 0:
     *     byte 6: trackId = 0
     *     bytes 7-9: UI24 trackSize = size of track 0 data (e.g., 4 bytes -> 0x000004)
     *     bytes 10-13: track 0 coded data
     *   Track 1:
     *     byte 14: trackId = 1
     *     bytes 15-17: UI24 trackSize = size of track 1 data (e.g., 4 bytes -> 0x000004)
     *     bytes 18-21: track 1 coded data
     */
    @Test
    public void testMultitrackVideoManyTracks() {
        log.info("testMultitrackVideoManyTracks");
        int hvcFourcc = IOUtils.makeFourcc("hvc1");

        byte[] track0Data = { 0x01, 0x02, 0x03, 0x04 };
        byte[] track1Data = { 0x11, 0x12, 0x13, 0x14 };

        IoBuffer data = IoBuffer.allocate(64);
        // enhanced + keyframe + Multitrack = 0x96
        data.put((byte) 0x96);
        // ManyTracks(1) << 4 | CodedFramesX(3) = 0x13
        data.put((byte) 0x13);
        // FourCC shared across all tracks
        data.putInt(hvcFourcc);
        // Track 0
        data.put((byte) 0x00); // trackId 0
        // UI24 size of track 0 data
        data.put((byte) 0x00);
        data.put((byte) 0x00);
        data.put((byte) track0Data.length);
        data.put(track0Data);
        // Track 1
        data.put((byte) 0x01); // trackId 1
        // UI24 size of track 1 data
        data.put((byte) 0x00);
        data.put((byte) 0x00);
        data.put((byte) track1Data.length);
        data.put(track1Data);
        data.flip();

        HEVCVideo video = new HEVCVideo();
        boolean result = video.addData(data, 4000);
        assertTrue("addData must return true for ManyTracks multitrack", result);
        assertTrue("Multitrack codec must report enhanced", video.isEnhanced());
    }

    /**
     * Test cascaded ModEx wrapping: two nested ModEx layers before the actual packet type.
     *
     * Wire format:
     *   byte 0: 0x97 (enhanced + keyframe + ModEx=7)
     *   bytes 1-4: FourCC "hvc1"
     *   -- First ModEx layer --
     *   byte 5: modExDataSize - 1 = 0x02 (size = 3 bytes)
     *   bytes 6-8: UI24 nano offset = 1000 ns = 0x0003E8
     *   byte 9: upper nibble = TimestampOffsetNano(0), lower nibble = ModEx(7) -> 0x07
     *   -- Second ModEx layer --
     *   byte 10: modExDataSize - 1 = 0x02 (size = 3 bytes)
     *   bytes 11-13: UI24 nano offset = 2000 ns = 0x0007D0
     *   byte 14: upper nibble = TimestampOffsetNano(0), lower nibble = CodedFramesX(3) -> 0x03
     *   -- Coded data --
     *   bytes 15+: fake coded video data
     */
    @Test
    public void testCascadedModEx() {
        log.info("testCascadedModEx");
        IoBuffer data = IoBuffer.allocate(64);
        // enhanced + keyframe + ModEx = 0x97
        data.put((byte) 0x97);
        // FourCC "hvc1"
        data.put((byte) 'h');
        data.put((byte) 'v');
        data.put((byte) 'c');
        data.put((byte) '1');
        // First ModEx: size-1 = 2 (3 bytes of data)
        data.put((byte) 0x02);
        // UI24 nano offset = 1000 = 0x0003E8
        data.put((byte) 0x00);
        data.put((byte) 0x03);
        data.put((byte) 0xE8);
        // Next byte: TimestampOffsetNano(0) | ModEx(7) -> 0x07  (still wrapped)
        data.put((byte) 0x07);
        // Second ModEx: size-1 = 2 (3 bytes of data)
        data.put((byte) 0x02);
        // UI24 nano offset = 2000 = 0x0007D0
        data.put((byte) 0x00);
        data.put((byte) 0x07);
        data.put((byte) 0xD0);
        // Next byte: TimestampOffsetNano(0) | CodedFramesX(3) -> 0x03
        data.put((byte) 0x03);
        // Fake coded video data
        data.put(new byte[] { 0xA, 0xB, 0xC, 0xD });
        data.flip();

        HEVCVideo video = new HEVCVideo();
        boolean result = video.addData(data, 6000);
        assertTrue("addData must return true for cascaded ModEx", result);
        // After unwrapping both ModEx layers, effective packet type is CodedFramesX
        assertEquals("Final packet type after cascaded ModEx unwrap must be CodedFramesX", VideoPacketType.CodedFramesX, video.getPacketType());
        assertEquals("Frame type must be KEYFRAME", VideoFrameType.KEYFRAME, video.getFrameType());
    }

}
