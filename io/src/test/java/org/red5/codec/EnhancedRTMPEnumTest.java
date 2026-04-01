package org.red5.codec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.EnumSet;

import org.junit.Test;
import org.red5.io.CapsExMask;
import org.red5.io.FourCcInfoMask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Comprehensive tests for Enhanced RTMP enum and support classes.
 */
public class EnhancedRTMPEnumTest {

    private static final Logger log = LoggerFactory.getLogger(EnhancedRTMPEnumTest.class);

    // -------------------------------------------------------------------------
    // VideoFrameType
    // -------------------------------------------------------------------------

    @Test
    public void testVideoFrameTypeKnownValues() {
        assertEquals(VideoFrameType.RESERVED, VideoFrameType.valueOf(0));
        assertEquals(VideoFrameType.KEYFRAME, VideoFrameType.valueOf(1));
        assertEquals(VideoFrameType.INTERFRAME, VideoFrameType.valueOf(2));
        assertEquals(VideoFrameType.DISPOSABLE, VideoFrameType.valueOf(3));
        assertEquals(VideoFrameType.GENERATED_KEYFRAME, VideoFrameType.valueOf(4));
        assertEquals(VideoFrameType.COMMAND_FRAME, VideoFrameType.valueOf(5));
        assertEquals(VideoFrameType.DELTA, VideoFrameType.valueOf(0x0f));
    }

    @Test
    public void testVideoFrameTypeGetValue() {
        assertEquals((byte) 0x00, VideoFrameType.RESERVED.getValue());
        assertEquals((byte) 0x01, VideoFrameType.KEYFRAME.getValue());
        assertEquals((byte) 0x02, VideoFrameType.INTERFRAME.getValue());
        assertEquals((byte) 0x03, VideoFrameType.DISPOSABLE.getValue());
        assertEquals((byte) 0x04, VideoFrameType.GENERATED_KEYFRAME.getValue());
        assertEquals((byte) 0x05, VideoFrameType.COMMAND_FRAME.getValue());
        assertEquals((byte) 0x0f, VideoFrameType.DELTA.getValue());
    }

    @Test
    public void testVideoFrameTypeUnknownReturnsNull() {
        assertNull(VideoFrameType.valueOf(6));
        assertNull(VideoFrameType.valueOf(0x0e));
        assertNull(VideoFrameType.valueOf(99));
        assertNull(VideoFrameType.valueOf(-1));
    }

    @Test
    public void testVideoFrameTypeCount() {
        assertEquals(7, VideoFrameType.values().length);
    }

    // -------------------------------------------------------------------------
    // VideoCommand
    // -------------------------------------------------------------------------

    @Test
    public void testVideoCommandKnownValues() {
        assertEquals(VideoCommand.START_SEEK, VideoCommand.valueOf(0));
        assertEquals(VideoCommand.END_SEEK, VideoCommand.valueOf(1));
    }

    @Test
    public void testVideoCommandGetValue() {
        assertEquals((byte) 0, VideoCommand.START_SEEK.getValue());
        assertEquals((byte) 0x01, VideoCommand.END_SEEK.getValue());
    }

    @Test
    public void testVideoCommandUnknownReturnsNull() {
        assertNull(VideoCommand.valueOf(2));
        assertNull(VideoCommand.valueOf(-1));
        assertNull(VideoCommand.valueOf(255));
    }

    @Test
    public void testVideoCommandCount() {
        assertEquals(2, VideoCommand.values().length);
    }

    // -------------------------------------------------------------------------
    // VideoPacketType
    // -------------------------------------------------------------------------

    @Test
    public void testVideoPacketTypeKnownValues() {
        assertEquals(VideoPacketType.SequenceStart, VideoPacketType.valueOf(0));
        assertEquals(VideoPacketType.CodedFrames, VideoPacketType.valueOf(1));
        assertEquals(VideoPacketType.SequenceEnd, VideoPacketType.valueOf(2));
        assertEquals(VideoPacketType.CodedFramesX, VideoPacketType.valueOf(3));
        assertEquals(VideoPacketType.Metadata, VideoPacketType.valueOf(4));
        assertEquals(VideoPacketType.MPEG2TSSequenceStart, VideoPacketType.valueOf(5));
        assertEquals(VideoPacketType.Multitrack, VideoPacketType.valueOf(6));
        assertEquals(VideoPacketType.ModEx, VideoPacketType.valueOf(7));
    }

    @Test
    public void testVideoPacketTypeGetPacketType() {
        assertEquals((byte) 0, VideoPacketType.SequenceStart.getPacketType());
        assertEquals((byte) 1, VideoPacketType.CodedFrames.getPacketType());
        assertEquals((byte) 2, VideoPacketType.SequenceEnd.getPacketType());
        assertEquals((byte) 3, VideoPacketType.CodedFramesX.getPacketType());
        assertEquals((byte) 4, VideoPacketType.Metadata.getPacketType());
        assertEquals((byte) 5, VideoPacketType.MPEG2TSSequenceStart.getPacketType());
        assertEquals((byte) 6, VideoPacketType.Multitrack.getPacketType());
        assertEquals((byte) 7, VideoPacketType.ModEx.getPacketType());
    }

    @Test
    public void testVideoPacketTypeUnknownReturnsNull() {
        assertNull(VideoPacketType.valueOf(8));
        assertNull(VideoPacketType.valueOf(-1));
        assertNull(VideoPacketType.valueOf(255));
    }

    @Test
    public void testVideoPacketTypeCount() {
        assertEquals(8, VideoPacketType.values().length);
    }

    // -------------------------------------------------------------------------
    // AudioPacketType
    // -------------------------------------------------------------------------

    @Test
    public void testAudioPacketTypeKnownValues() {
        assertEquals(AudioPacketType.SequenceStart, AudioPacketType.valueOf(0));
        assertEquals(AudioPacketType.CodedFrames, AudioPacketType.valueOf(1));
        assertEquals(AudioPacketType.SequenceEnd, AudioPacketType.valueOf(2));
        assertEquals(AudioPacketType.MultichannelConfig, AudioPacketType.valueOf(4));
        assertEquals(AudioPacketType.Multitrack, AudioPacketType.valueOf(5));
        assertEquals(AudioPacketType.ModEx, AudioPacketType.valueOf(7));
    }

    @Test
    public void testAudioPacketTypeGetPacketType() {
        assertEquals((byte) 0, AudioPacketType.SequenceStart.getPacketType());
        assertEquals((byte) 1, AudioPacketType.CodedFrames.getPacketType());
        assertEquals((byte) 2, AudioPacketType.SequenceEnd.getPacketType());
        assertEquals((byte) 4, AudioPacketType.MultichannelConfig.getPacketType());
        assertEquals((byte) 5, AudioPacketType.Multitrack.getPacketType());
        assertEquals((byte) 7, AudioPacketType.ModEx.getPacketType());
    }

    @Test
    public void testAudioPacketTypeGapsReturnNull() {
        // Values 3 and 6 are not defined
        assertNull(AudioPacketType.valueOf(3));
        assertNull(AudioPacketType.valueOf(6));
    }

    @Test
    public void testAudioPacketTypeUnknownReturnsNull() {
        assertNull(AudioPacketType.valueOf(8));
        assertNull(AudioPacketType.valueOf(-1));
        assertNull(AudioPacketType.valueOf(255));
    }

    @Test
    public void testAudioPacketTypeCount() {
        assertEquals(6, AudioPacketType.values().length);
    }

    // -------------------------------------------------------------------------
    // AvMultitrackType
    // -------------------------------------------------------------------------

    @Test
    public void testAvMultitrackTypeKnownValues() {
        assertEquals(AvMultitrackType.OneTrack, AvMultitrackType.valueOf(0));
        assertEquals(AvMultitrackType.ManyTracks, AvMultitrackType.valueOf(1));
        assertEquals(AvMultitrackType.ManyTracksManyCodecs, AvMultitrackType.valueOf(2));
    }

    @Test
    public void testAvMultitrackTypeGetMultitrackType() {
        assertEquals((byte) 0, AvMultitrackType.OneTrack.getMultitrackType());
        assertEquals((byte) 1, AvMultitrackType.ManyTracks.getMultitrackType());
        assertEquals((byte) 2, AvMultitrackType.ManyTracksManyCodecs.getMultitrackType());
    }

    @Test
    public void testAvMultitrackTypeUnknownReturnsNull() {
        assertNull(AvMultitrackType.valueOf(3));
        assertNull(AvMultitrackType.valueOf(-1));
        assertNull(AvMultitrackType.valueOf(99));
    }

    @Test
    public void testAvMultitrackTypeCount() {
        assertEquals(3, AvMultitrackType.values().length);
    }

    // -------------------------------------------------------------------------
    // AudioChannelOrder
    // -------------------------------------------------------------------------

    @Test
    public void testAudioChannelOrderKnownValues() {
        assertEquals(AudioChannelOrder.Unspecified, AudioChannelOrder.valueOf(0));
        assertEquals(AudioChannelOrder.Native, AudioChannelOrder.valueOf(1));
        assertEquals(AudioChannelOrder.Custom, AudioChannelOrder.valueOf(2));
    }

    @Test
    public void testAudioChannelOrderGetOrder() {
        assertEquals(0, AudioChannelOrder.Unspecified.getOrder());
        assertEquals(1, AudioChannelOrder.Native.getOrder());
        assertEquals(2, AudioChannelOrder.Custom.getOrder());
    }

    @Test
    public void testAudioChannelOrderUnknownReturnsUnspecified() {
        // Unlike other enums, unknown values return Unspecified, not null
        assertEquals(AudioChannelOrder.Unspecified, AudioChannelOrder.valueOf(3));
        assertEquals(AudioChannelOrder.Unspecified, AudioChannelOrder.valueOf(-1));
        assertEquals(AudioChannelOrder.Unspecified, AudioChannelOrder.valueOf(99));
    }

    @Test
    public void testAudioChannelOrderCount() {
        assertEquals(3, AudioChannelOrder.values().length);
    }

    // -------------------------------------------------------------------------
    // AudioChannel
    // -------------------------------------------------------------------------

    @Test
    public void testAudioChannelStandardChannels() {
        assertEquals(AudioChannel.FrontLeft, AudioChannel.fromChannel(0));
        assertEquals(AudioChannel.FrontRight, AudioChannel.fromChannel(1));
        assertEquals(AudioChannel.FrontCenter, AudioChannel.fromChannel(2));
        assertEquals(AudioChannel.LowFrequency1, AudioChannel.fromChannel(3));
        assertEquals(AudioChannel.BackLeft, AudioChannel.fromChannel(4));
        assertEquals(AudioChannel.BackRight, AudioChannel.fromChannel(5));
        assertEquals(AudioChannel.FrontLeftCenter, AudioChannel.fromChannel(6));
        assertEquals(AudioChannel.FrontRightCenter, AudioChannel.fromChannel(7));
        assertEquals(AudioChannel.BackCenter, AudioChannel.fromChannel(8));
        assertEquals(AudioChannel.SideLeft, AudioChannel.fromChannel(9));
        assertEquals(AudioChannel.SideRight, AudioChannel.fromChannel(10));
        assertEquals(AudioChannel.TopCenter, AudioChannel.fromChannel(11));
        assertEquals(AudioChannel.TopFrontLeft, AudioChannel.fromChannel(12));
        assertEquals(AudioChannel.TopFrontCenter, AudioChannel.fromChannel(13));
        assertEquals(AudioChannel.TopFrontRight, AudioChannel.fromChannel(14));
        assertEquals(AudioChannel.TopBackLeft, AudioChannel.fromChannel(15));
        assertEquals(AudioChannel.TopBackCenter, AudioChannel.fromChannel(16));
        assertEquals(AudioChannel.TopBackRight, AudioChannel.fromChannel(17));
    }

    @Test
    public void testAudioChannel2222Channels() {
        assertEquals(AudioChannel.LowFrequency2, AudioChannel.fromChannel(18));
        assertEquals(AudioChannel.TopSideLeft, AudioChannel.fromChannel(19));
        assertEquals(AudioChannel.TopSideRight, AudioChannel.fromChannel(20));
        assertEquals(AudioChannel.BottomFrontCenter, AudioChannel.fromChannel(21));
        assertEquals(AudioChannel.BottomFrontLeft, AudioChannel.fromChannel(22));
        assertEquals(AudioChannel.BottomFrontRight, AudioChannel.fromChannel(23));
    }

    @Test
    public void testAudioChannelSpecialValues() {
        // AudioChannel stores values as byte; 0xfe and 0xff are stored as signed -2 and -1.
        // fromChannel(int) compares the stored byte (sign-extended) to the int argument,
        // so callers must pass the signed equivalent to match.
        assertEquals(AudioChannel.Unused, AudioChannel.fromChannel((byte) 0xfe)); // -2
        assertEquals(AudioChannel.Unknown, AudioChannel.fromChannel((byte) 0xff)); // -1
    }

    @Test
    public void testAudioChannelGetChannel() {
        assertEquals((byte) 0, AudioChannel.FrontLeft.getChannel());
        assertEquals((byte) 23, AudioChannel.BottomFrontRight.getChannel());
        assertEquals((byte) 0xfe, AudioChannel.Unused.getChannel());
        assertEquals((byte) 0xff, AudioChannel.Unknown.getChannel());
    }

    @Test
    public void testAudioChannelUnrecognizedReturnsUnknown() {
        // Values 24-253 (unsigned) correspond to signed bytes 24-127 then -128..-3;
        // none of these are defined, so fromChannel returns Unknown.
        assertEquals(AudioChannel.Unknown, AudioChannel.fromChannel(24));
        assertEquals(AudioChannel.Unknown, AudioChannel.fromChannel(100));
        // 0xfd unsigned == -3 signed, not a defined channel
        assertEquals(AudioChannel.Unknown, AudioChannel.fromChannel((byte) 0xfd));
    }

    @Test
    public void testAudioChannelCount() {
        // 24 standard + Unused + Unknown = 26
        assertEquals(26, AudioChannel.values().length);
    }

    // -------------------------------------------------------------------------
    // AudioChannelMask
    // -------------------------------------------------------------------------

    @Test
    public void testAudioChannelMaskGetMask() {
        assertEquals(0x000001, AudioChannelMask.FrontLeft.getMask());
        assertEquals(0x000002, AudioChannelMask.FrontRight.getMask());
        assertEquals(0x000004, AudioChannelMask.FrontCenter.getMask());
        assertEquals(0x000008, AudioChannelMask.LowFrequency1.getMask());
        assertEquals(0x000010, AudioChannelMask.BackLeft.getMask());
        assertEquals(0x000020, AudioChannelMask.BackRight.getMask());
        assertEquals(0x000040, AudioChannelMask.FrontLeftCenter.getMask());
        assertEquals(0x000080, AudioChannelMask.FrontRightCenter.getMask());
        assertEquals(0x000100, AudioChannelMask.BackCenter.getMask());
        assertEquals(0x000200, AudioChannelMask.SideLeft.getMask());
        assertEquals(0x000400, AudioChannelMask.SideRight.getMask());
        assertEquals(0x000800, AudioChannelMask.TopCenter.getMask());
        assertEquals(0x001000, AudioChannelMask.TopFrontLeft.getMask());
        assertEquals(0x002000, AudioChannelMask.TopFrontCenter.getMask());
        assertEquals(0x004000, AudioChannelMask.TopFrontRight.getMask());
        assertEquals(0x008000, AudioChannelMask.TopBackLeft.getMask());
        assertEquals(0x010000, AudioChannelMask.TopBackCenter.getMask());
        assertEquals(0x020000, AudioChannelMask.TopBackRight.getMask());
        assertEquals(0x040000, AudioChannelMask.LowFrequency2.getMask());
        assertEquals(0x080000, AudioChannelMask.TopSideLeft.getMask());
        assertEquals(0x100000, AudioChannelMask.TopSideRight.getMask());
        assertEquals(0x200000, AudioChannelMask.BottomFrontCenter.getMask());
        assertEquals(0x400000, AudioChannelMask.BottomFrontLeft.getMask());
        assertEquals(0x800000, AudioChannelMask.BottomFrontRight.getMask());
    }

    @Test
    public void testAudioChannelMaskUniquePowerOfTwo() {
        // Each mask must be a unique power of two (single bit set)
        int combined = 0;
        for (AudioChannelMask m : AudioChannelMask.values()) {
            int mask = m.getMask();
            assertTrue("Mask should be a power of two: " + mask, mask > 0 && (mask & (mask - 1)) == 0);
            assertTrue("Duplicate mask value detected: " + mask, (combined & mask) == 0);
            combined |= mask;
        }
    }

    @Test
    public void testAudioChannelMaskBitwiseAnd() {
        int stereoMask = AudioChannelMask.FrontLeft.getMask() | AudioChannelMask.FrontRight.getMask();
        assertTrue((stereoMask & AudioChannelMask.FrontLeft.getMask()) != 0);
        assertTrue((stereoMask & AudioChannelMask.FrontRight.getMask()) != 0);
        assertTrue((stereoMask & AudioChannelMask.FrontCenter.getMask()) == 0);
    }

    @Test
    public void testAudioChannelMaskCount() {
        assertEquals(24, AudioChannelMask.values().length);
    }

    // -------------------------------------------------------------------------
    // CapsExMask
    // -------------------------------------------------------------------------

    @Test
    public void testCapsExMaskGetMask() {
        assertEquals((byte) 0x01, CapsExMask.Reconnect.getMask());
        assertEquals((byte) 0x02, CapsExMask.Multitrack.getMask());
        assertEquals((byte) 0x04, CapsExMask.ModEx.getMask());
        assertEquals((byte) 0x08, CapsExMask.TimestampNanoOffset.getMask());
    }

    @Test
    public void testCapsExMaskFromMaskSingleValues() {
        EnumSet<CapsExMask> reconnect = CapsExMask.fromMask((byte) 0x01);
        assertTrue(reconnect.contains(CapsExMask.Reconnect));
        assertEquals(1, reconnect.size());

        EnumSet<CapsExMask> multitrack = CapsExMask.fromMask((byte) 0x02);
        assertTrue(multitrack.contains(CapsExMask.Multitrack));
        assertEquals(1, multitrack.size());

        EnumSet<CapsExMask> modex = CapsExMask.fromMask((byte) 0x04);
        assertTrue(modex.contains(CapsExMask.ModEx));
        assertEquals(1, modex.size());

        EnumSet<CapsExMask> ts = CapsExMask.fromMask((byte) 0x08);
        assertTrue(ts.contains(CapsExMask.TimestampNanoOffset));
        assertEquals(1, ts.size());
    }

    @Test
    public void testCapsExMaskFromMaskCombined() {
        EnumSet<CapsExMask> set = CapsExMask.fromMask((byte) 0x03);
        assertTrue(set.contains(CapsExMask.Reconnect));
        assertTrue(set.contains(CapsExMask.Multitrack));
        assertEquals(2, set.size());

        EnumSet<CapsExMask> all = CapsExMask.fromMask((byte) 0x0f);
        assertEquals(4, all.size());
        assertTrue(all.contains(CapsExMask.Reconnect));
        assertTrue(all.contains(CapsExMask.Multitrack));
        assertTrue(all.contains(CapsExMask.ModEx));
        assertTrue(all.contains(CapsExMask.TimestampNanoOffset));
    }

    @Test
    public void testCapsExMaskFromMaskZero() {
        EnumSet<CapsExMask> empty = CapsExMask.fromMask((byte) 0x00);
        assertTrue(empty.isEmpty());
    }

    @Test
    public void testCapsExMaskToMask() {
        EnumSet<CapsExMask> set = EnumSet.of(CapsExMask.Reconnect, CapsExMask.ModEx);
        byte mask = CapsExMask.toMask(set);
        assertEquals((byte) 0x05, mask);
    }

    @Test
    public void testCapsExMaskToMaskEmpty() {
        byte mask = CapsExMask.toMask(EnumSet.noneOf(CapsExMask.class));
        assertEquals((byte) 0x00, mask);
    }

    @Test
    public void testCapsExMaskToMaskAll() {
        byte mask = CapsExMask.toMask(CapsExMask.all());
        assertEquals((byte) 0x0f, mask);
    }

    @Test
    public void testCapsExMaskRoundTrip() {
        for (CapsExMask m : CapsExMask.values()) {
            EnumSet<CapsExMask> single = EnumSet.of(m);
            byte encoded = CapsExMask.toMask(single);
            EnumSet<CapsExMask> decoded = CapsExMask.fromMask(encoded);
            assertEquals("Round-trip failed for " + m, single, decoded);
        }

        EnumSet<CapsExMask> all = CapsExMask.all();
        assertEquals(all, CapsExMask.fromMask(CapsExMask.toMask(all)));
    }

    @Test
    public void testCapsExMaskAllReturnsComplete() {
        EnumSet<CapsExMask> all = CapsExMask.all();
        assertEquals(4, all.size());
        assertTrue(all.containsAll(EnumSet.allOf(CapsExMask.class)));
    }

    // -------------------------------------------------------------------------
    // FourCcInfoMask
    // -------------------------------------------------------------------------

    @Test
    public void testFourCcInfoMaskGetMask() {
        assertEquals((byte) 0x01, FourCcInfoMask.CanDecode.getMask());
        assertEquals((byte) 0x02, FourCcInfoMask.CanEncode.getMask());
        assertEquals((byte) 0x04, FourCcInfoMask.CanForward.getMask());
    }

    @Test
    public void testFourCcInfoMaskFromMaskSingleValues() {
        EnumSet<FourCcInfoMask> decode = FourCcInfoMask.fromMask((byte) 0x01);
        assertTrue(decode.contains(FourCcInfoMask.CanDecode));
        assertEquals(1, decode.size());

        EnumSet<FourCcInfoMask> encode = FourCcInfoMask.fromMask((byte) 0x02);
        assertTrue(encode.contains(FourCcInfoMask.CanEncode));
        assertEquals(1, encode.size());

        EnumSet<FourCcInfoMask> forward = FourCcInfoMask.fromMask((byte) 0x04);
        assertTrue(forward.contains(FourCcInfoMask.CanForward));
        assertEquals(1, forward.size());
    }

    @Test
    public void testFourCcInfoMaskFromMaskCombined() {
        EnumSet<FourCcInfoMask> decodeEncode = FourCcInfoMask.fromMask((byte) 0x03);
        assertTrue(decodeEncode.contains(FourCcInfoMask.CanDecode));
        assertTrue(decodeEncode.contains(FourCcInfoMask.CanEncode));
        assertEquals(2, decodeEncode.size());

        EnumSet<FourCcInfoMask> all = FourCcInfoMask.fromMask((byte) 0x07);
        assertEquals(3, all.size());
        assertTrue(all.contains(FourCcInfoMask.CanDecode));
        assertTrue(all.contains(FourCcInfoMask.CanEncode));
        assertTrue(all.contains(FourCcInfoMask.CanForward));
    }

    @Test
    public void testFourCcInfoMaskFromMaskZero() {
        EnumSet<FourCcInfoMask> empty = FourCcInfoMask.fromMask((byte) 0x00);
        assertTrue(empty.isEmpty());
    }

    @Test
    public void testFourCcInfoMaskToMask() {
        EnumSet<FourCcInfoMask> set = EnumSet.of(FourCcInfoMask.CanDecode, FourCcInfoMask.CanForward);
        byte mask = FourCcInfoMask.toMask(set);
        assertEquals((byte) 0x05, mask);
    }

    @Test
    public void testFourCcInfoMaskToMaskEmpty() {
        byte mask = FourCcInfoMask.toMask(EnumSet.noneOf(FourCcInfoMask.class));
        assertEquals((byte) 0x00, mask);
    }

    @Test
    public void testFourCcInfoMaskToMaskAll() {
        byte mask = FourCcInfoMask.toMask(FourCcInfoMask.all());
        assertEquals((byte) 0x07, mask);
    }

    @Test
    public void testFourCcInfoMaskRoundTrip() {
        for (FourCcInfoMask m : FourCcInfoMask.values()) {
            EnumSet<FourCcInfoMask> single = EnumSet.of(m);
            byte encoded = FourCcInfoMask.toMask(single);
            EnumSet<FourCcInfoMask> decoded = FourCcInfoMask.fromMask(encoded);
            assertEquals("Round-trip failed for " + m, single, decoded);
        }

        EnumSet<FourCcInfoMask> all = FourCcInfoMask.all();
        assertEquals(all, FourCcInfoMask.fromMask(FourCcInfoMask.toMask(all)));
    }

    @Test
    public void testFourCcInfoMaskAllReturnsComplete() {
        EnumSet<FourCcInfoMask> all = FourCcInfoMask.all();
        assertEquals(3, all.size());
        assertTrue(all.containsAll(EnumSet.allOf(FourCcInfoMask.class)));
    }

    // -------------------------------------------------------------------------
    // VideoFunctionFlag
    // -------------------------------------------------------------------------

    @Test
    public void testVideoFunctionFlagConstants() {
        assertEquals(0x0001, VideoFunctionFlag.SUPPORT_VID_CLIENT_SEEK);
        assertEquals(0x0002, VideoFunctionFlag.SUPPORT_VID_CLIENT_HDR);
        assertEquals(0x0004, VideoFunctionFlag.SUPPORT_VID_CLIENT_VIDEO_PACKET_TYPE_METADATA);
        assertEquals(0x0008, VideoFunctionFlag.SUPPORT_VID_CLIENT_LARGE_SCALE_TILE);
    }

    @Test
    public void testVideoFunctionFlagOrCombinations() {
        int seekAndHdr = VideoFunctionFlag.SUPPORT_VID_CLIENT_SEEK | VideoFunctionFlag.SUPPORT_VID_CLIENT_HDR;
        assertEquals(0x0003, seekAndHdr);

        int allFlags = VideoFunctionFlag.SUPPORT_VID_CLIENT_SEEK | VideoFunctionFlag.SUPPORT_VID_CLIENT_HDR | VideoFunctionFlag.SUPPORT_VID_CLIENT_VIDEO_PACKET_TYPE_METADATA | VideoFunctionFlag.SUPPORT_VID_CLIENT_LARGE_SCALE_TILE;
        assertEquals(0x000f, allFlags);
    }

    @Test
    public void testVideoFunctionFlagBitwiseCheck() {
        int flags = VideoFunctionFlag.SUPPORT_VID_CLIENT_SEEK | VideoFunctionFlag.SUPPORT_VID_CLIENT_VIDEO_PACKET_TYPE_METADATA;

        assertTrue((flags & VideoFunctionFlag.SUPPORT_VID_CLIENT_SEEK) != 0);
        assertTrue((flags & VideoFunctionFlag.SUPPORT_VID_CLIENT_VIDEO_PACKET_TYPE_METADATA) != 0);
        // HDR and large-scale-tile should NOT be set
        assertEquals(0, flags & VideoFunctionFlag.SUPPORT_VID_CLIENT_HDR);
        assertEquals(0, flags & VideoFunctionFlag.SUPPORT_VID_CLIENT_LARGE_SCALE_TILE);
    }

    @Test
    public void testVideoFunctionFlagEachIsUniquePowerOfTwo() {
        int[] flags = { VideoFunctionFlag.SUPPORT_VID_CLIENT_SEEK, VideoFunctionFlag.SUPPORT_VID_CLIENT_HDR, VideoFunctionFlag.SUPPORT_VID_CLIENT_VIDEO_PACKET_TYPE_METADATA, VideoFunctionFlag.SUPPORT_VID_CLIENT_LARGE_SCALE_TILE };
        int combined = 0;
        for (int f : flags) {
            assertTrue("Flag should be positive: " + f, f > 0);
            assertTrue("Flag should be power of two: " + f, (f & (f - 1)) == 0);
            assertTrue("Duplicate flag bit detected: " + f, (combined & f) == 0);
            combined |= f;
        }
    }

}
