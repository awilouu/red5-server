package org.red5.codec;

import static org.junit.Assert.*;

import org.apache.mina.core.buffer.IoBuffer;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExtendedAudioTest {

    private static Logger log = LoggerFactory.getLogger(ExtendedAudioTest.class);

    // Helper: write a 4-byte ASCII FourCC into buffer
    private static void putFourCC(IoBuffer buf, String fourcc) {
        buf.put((byte) fourcc.charAt(0));
        buf.put((byte) fourcc.charAt(1));
        buf.put((byte) fourcc.charAt(2));
        buf.put((byte) fourcc.charAt(3));
    }

    // 1. canHandleData: valid ExHeader bytes return true
    @Test
    public void testCanHandleDataValid() {
        log.info("testCanHandleDataValid");
        // 0x90 = ExHeader(0x9) << 4 | SequenceStart(0)
        IoBuffer data = IoBuffer.allocate(4);
        data.put((byte) 0x90);
        data.put((byte) 0x00);
        data.put((byte) 0x00);
        data.put((byte) 0x00);
        data.flip();

        ExtendedAudio audio = new ExtendedAudio();
        assertTrue(audio.canHandleData(data));
        // position should be reset after the check
        assertEquals(0, data.position());

        // also test with 0x91 (CodedFrames)
        IoBuffer data2 = IoBuffer.allocate(4);
        data2.put((byte) 0x91);
        data2.put((byte) 0x00);
        data2.put((byte) 0x00);
        data2.put((byte) 0x00);
        data2.flip();
        assertTrue(audio.canHandleData(data2));
        log.info("testCanHandleDataValid end\n");
    }

    // 2. canHandleData: non-ExHeader byte returns false
    @Test
    public void testCanHandleDataInvalid() {
        log.info("testCanHandleDataInvalid");
        // 0xA0 = codec id 0xA (AAC) << 4
        IoBuffer data = IoBuffer.allocate(4);
        data.put((byte) 0xA0);
        data.put((byte) 0x00);
        data.put((byte) 0x00);
        data.put((byte) 0x00);
        data.flip();

        ExtendedAudio audio = new ExtendedAudio();
        assertFalse(audio.canHandleData(data));
        assertEquals(0, data.position());
        log.info("testCanHandleDataInvalid end\n");
    }

    // 3. addData with AAC SequenceStart
    // Non-multitrack path: FourCC is read at line 134 (before while loop) AND at line 174 (inside while loop else branch).
    // Buffer: [0x90] [mp4a] [mp4a] [fake-config-bytes...]
    @Test
    public void testAddDataAacSequenceStart() {
        log.info("testAddDataAacSequenceStart");
        IoBuffer data = IoBuffer.allocate(32);
        // first byte: ExHeader(9) << 4 | SequenceStart(0) = 0x90
        data.put((byte) 0x90);
        // FourCC for AAC read before while loop
        putFourCC(data, "mp4a");
        // FourCC for AAC read inside while loop (else branch)
        putFourCC(data, "mp4a");
        // fake AAC sequence config (2 bytes)
        data.put((byte) 0x12);
        data.put((byte) 0x10);
        data.flip();

        ExtendedAudio audio = new ExtendedAudio();
        assertTrue(audio.canHandleData(data));
        assertTrue(audio.addData(data));
        log.info("testAddDataAacSequenceStart end\n");
    }

    // 4. addData with Opus CodedFrames
    // Buffer: [0x91] [Opus] [Opus] [fake-audio-data...]
    @Test
    public void testAddDataOpusCodedFrames() {
        log.info("testAddDataOpusCodedFrames");
        IoBuffer data = IoBuffer.allocate(32);
        // first byte: ExHeader(9) << 4 | CodedFrames(1) = 0x91
        data.put((byte) 0x91);
        // FourCC read before while loop
        putFourCC(data, "Opus");
        // FourCC read inside while loop (else branch)
        putFourCC(data, "Opus");
        // fake encoded audio
        data.put((byte) 0x01);
        data.put((byte) 0x02);
        data.put((byte) 0x03);
        data.flip();

        ExtendedAudio audio = new ExtendedAudio();
        assertTrue(audio.canHandleData(data));
        assertTrue(audio.addData(data));
        log.info("testAddDataOpusCodedFrames end\n");
    }

    // 5. addData with FLAC SequenceStart
    // Buffer: [0x90] [fLaC] [fLaC] [fake-config...]
    @Test
    public void testAddDataFlacSequenceStart() {
        log.info("testAddDataFlacSequenceStart");
        IoBuffer data = IoBuffer.allocate(32);
        // first byte: ExHeader(9) << 4 | SequenceStart(0) = 0x90
        data.put((byte) 0x90);
        // FourCC read before while loop
        putFourCC(data, "fLaC");
        // FourCC read inside while loop
        putFourCC(data, "fLaC");
        // fake FLAC stream info block
        data.put((byte) 0x00);
        data.put((byte) 0x22);
        data.flip();

        ExtendedAudio audio = new ExtendedAudio();
        assertTrue(audio.canHandleData(data));
        assertTrue(audio.addData(data));
        log.info("testAddDataFlacSequenceStart end\n");
    }

    // 6. addData MultichannelConfig with Custom channel order
    // Buffer: [0x94] [mp4a] [mp4a] [Custom(2)] [channel_count(2)] [FrontLeft(0)] [FrontRight(1)]
    @Test
    public void testAddDataMultichannelConfigCustom() {
        log.info("testAddDataMultichannelConfigCustom");
        IoBuffer data = IoBuffer.allocate(32);
        // first byte: ExHeader(9) << 4 | MultichannelConfig(4) = 0x94
        data.put((byte) 0x94);
        // FourCC before while loop
        putFourCC(data, "mp4a");
        // FourCC inside while loop
        putFourCC(data, "mp4a");
        // AudioChannelOrder.Custom = 2
        data.put((byte) 2);
        // channel count = 2
        data.put((byte) 2);
        // channel map: FrontLeft(0), FrontRight(1)
        data.put((byte) 0);
        data.put((byte) 1);
        data.flip();

        ExtendedAudio audio = new ExtendedAudio();
        assertTrue(audio.canHandleData(data));
        assertTrue(audio.addData(data));
        log.info("testAddDataMultichannelConfigCustom end\n");
    }

    // 7. addData MultichannelConfig with Native channel order
    // Buffer: [0x94] [mp4a] [mp4a] [Native(1)] [channel_count(2)] [4-byte audioChannelFlags]
    @Test
    public void testAddDataMultichannelConfigNative() {
        log.info("testAddDataMultichannelConfigNative");
        IoBuffer data = IoBuffer.allocate(32);
        // first byte: ExHeader(9) << 4 | MultichannelConfig(4) = 0x94
        data.put((byte) 0x94);
        // FourCC before while loop
        putFourCC(data, "mp4a");
        // FourCC inside while loop
        putFourCC(data, "mp4a");
        // AudioChannelOrder.Native = 1
        data.put((byte) 1);
        // channel count = 2
        data.put((byte) 2);
        // 4-byte audioChannelFlags (front left + front right bits)
        data.put((byte) 0x00);
        data.put((byte) 0x00);
        data.put((byte) 0x00);
        data.put((byte) 0x03);
        data.flip();

        ExtendedAudio audio = new ExtendedAudio();
        assertTrue(audio.canHandleData(data));
        assertTrue(audio.addData(data));
        log.info("testAddDataMultichannelConfigNative end\n");
    }

    // 8. addData Multitrack OneTrack
    // Buffer: [0x95] [multitrack-byte: OneTrack(0)<<4 | CodedFrames(1) = 0x01] [mp4a] [trackId=0] [fake-audio...]
    // In multitrack path: FourCC read once before while loop (getTrackCodec at line 126).
    // Inside while loop multitrack branch: reads trackId (no trackSize for OneTrack), then switch on packetType.
    @Test
    public void testAddDataMultitrackOneTrack() {
        log.info("testAddDataMultitrackOneTrack");
        IoBuffer data = IoBuffer.allocate(32);
        // first byte: ExHeader(9) << 4 | Multitrack(5) = 0x95
        data.put((byte) 0x95);
        // multitrack byte: AvMultitrackType.OneTrack(0) << 4 | AudioPacketType.CodedFrames(1) = 0x01
        data.put((byte) 0x01);
        // FourCC for all tracks (OneTrack is not ManyTracksManyCodecs so FourCC is shared)
        putFourCC(data, "mp4a");
        // trackId = 0
        data.put((byte) 0);
        // fake coded audio data
        data.put((byte) 0xAA);
        data.put((byte) 0xBB);
        data.flip();

        ExtendedAudio audio = new ExtendedAudio();
        assertTrue(audio.canHandleData(data));
        assertTrue(audio.addData(data));
        log.info("testAddDataMultitrackOneTrack end\n");
    }

    // 9. addData Multitrack ManyTracks
    // Buffer: [0x95] [ManyTracks(1)<<4 | CodedFrames(1) = 0x11] [mp4a]
    //         [trackId=0] [UI24 trackSize=2] [0xAA 0xBB]
    //         [trackId=1] [UI24 trackSize=2] [0xCC 0xDD]
    // ManyTracks reads trackSize and skips ahead via data.skip(trackSize)
    @Test
    public void testAddDataMultitrackManyTracks() {
        log.info("testAddDataMultitrackManyTracks");
        IoBuffer data = IoBuffer.allocate(64);
        // first byte: ExHeader(9) << 4 | Multitrack(5) = 0x95
        data.put((byte) 0x95);
        // multitrack byte: AvMultitrackType.ManyTracks(1) << 4 | AudioPacketType.CodedFrames(1) = 0x11
        data.put((byte) 0x11);
        // shared FourCC (ManyTracks is not ManyTracksManyCodecs)
        putFourCC(data, "mp4a");
        // track 0
        data.put((byte) 0); // trackId = 0
        data.put((byte) 0x00); // trackSize UI24 high
        data.put((byte) 0x00); // trackSize UI24 mid
        data.put((byte) 0x02); // trackSize UI24 low = 2
        data.put((byte) 0xAA); // track data (will be skipped via data.skip)
        data.put((byte) 0xBB);
        // track 1
        data.put((byte) 1); // trackId = 1
        data.put((byte) 0x00);
        data.put((byte) 0x00);
        data.put((byte) 0x02); // trackSize = 2
        data.put((byte) 0xCC);
        data.put((byte) 0xDD);
        data.flip();

        ExtendedAudio audio = new ExtendedAudio();
        assertTrue(audio.canHandleData(data));
        assertTrue(audio.addData(data));
        log.info("testAddDataMultitrackManyTracks end\n");
    }

    // 10. addData with SequenceEnd
    // Buffer: [0x92] [mp4a] [mp4a]
    @Test
    public void testAddDataSequenceEnd() {
        log.info("testAddDataSequenceEnd");
        IoBuffer data = IoBuffer.allocate(16);
        // first byte: ExHeader(9) << 4 | SequenceEnd(2) = 0x92
        data.put((byte) 0x92);
        // FourCC before while loop
        putFourCC(data, "mp4a");
        // FourCC inside while loop (else branch)
        putFourCC(data, "mp4a");
        data.flip();

        ExtendedAudio audio = new ExtendedAudio();
        assertTrue(audio.canHandleData(data));
        assertTrue(audio.addData(data));
        log.info("testAddDataSequenceEnd end\n");
    }

    // 11. getTrackCodec(int trackId) returns correct codec after addData
    @Test
    public void testGetTrackCodecByTrackId() {
        log.info("testGetTrackCodecByTrackId");
        IoBuffer data = IoBuffer.allocate(32);
        // Multitrack OneTrack with AAC, trackId=0
        data.put((byte) 0x95);
        data.put((byte) 0x01); // OneTrack(0)<<4 | CodedFrames(1)
        putFourCC(data, "mp4a");
        data.put((byte) 0); // trackId
        data.put((byte) 0xAA);
        data.put((byte) 0xBB);
        data.flip();

        ExtendedAudio audio = new ExtendedAudio();
        assertTrue(audio.addData(data));
        IAudioStreamCodec tc = audio.getTrackCodec(0);
        assertNotNull(tc);
        log.info("testGetTrackCodecByTrackId end\n");
    }

    // 12. addData with unknown FourCC returns false
    @Test
    public void testAddDataUnknownFourCC() {
        log.info("testAddDataUnknownFourCC");
        IoBuffer data = IoBuffer.allocate(16);
        // first byte: ExHeader(9) << 4 | SequenceStart(0) = 0x90
        data.put((byte) 0x90);
        // Unknown FourCC "XXXX"
        putFourCC(data, "XXXX");
        data.flip();

        ExtendedAudio audio = new ExtendedAudio();
        assertTrue(audio.canHandleData(data));
        // getTrackCodec will return null for unknown FourCC, so addData returns false
        assertFalse(audio.addData(data));
        log.info("testAddDataUnknownFourCC end\n");
    }

    // 13. addData ModEx unwrap: 0x97 + ModEx data + unwrapped packet type CodedFrames
    // ModEx structure: [modExDataSize-1 (UI8)] [modExData bytes] [modExType<<4 | unwrappedPacketType]
    // After unwrap, gets CodedFrames(1), then reads FourCC twice (non-multitrack path)
    @Test
    public void testAddDataModExUnwrap() {
        log.info("testAddDataModExUnwrap");
        IoBuffer data = IoBuffer.allocate(64);
        // first byte: ExHeader(9) << 4 | ModEx(7) = 0x97
        data.put((byte) 0x97);
        // ModEx data size: UI8 + 1 = actual size; we use 3 bytes of ModEx data => put (3-1)=2
        data.put((byte) 2);
        // 3 bytes of TimestampOffsetNano ModEx data (UI24 nano offset)
        data.put((byte) 0x00);
        data.put((byte) 0x0F);
        data.put((byte) 0x42); // 0x000F42 = 3906 ns
        // next byte: ModExType(0=TimestampOffsetNano) << 4 | unwrapped PacketType(CodedFrames=1) = 0x01
        data.put((byte) 0x01);
        // Now non-multitrack CodedFrames: FourCC before while loop
        putFourCC(data, "Opus");
        // FourCC inside while loop (else branch)
        putFourCC(data, "Opus");
        // fake opus audio payload
        data.put((byte) 0x48);
        data.put((byte) 0x65);
        data.flip();

        ExtendedAudio audio = new ExtendedAudio();
        assertTrue(audio.canHandleData(data));
        assertTrue(audio.addData(data));
        log.info("testAddDataModExUnwrap end\n");
    }

}
