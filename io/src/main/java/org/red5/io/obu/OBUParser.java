package org.red5.io.obu;

import static org.red5.io.obu.OBPConstants.OBU_FRAME_TYPE_BITSHIFT;
import static org.red5.io.obu.OBPConstants.OBU_FRAME_TYPE_MASK;
import static org.red5.io.obu.OBUType.FRAME;
import static org.red5.io.obu.OBUType.FRAME_HEADER;
import static org.red5.io.obu.OBUType.METADATA;
import static org.red5.io.obu.OBUType.PADDING;
import static org.red5.io.obu.OBUType.REDUNDANT_FRAME_HEADER;
import static org.red5.io.obu.OBUType.SEQUENCE_HEADER;
import static org.red5.io.obu.OBUType.TEMPORAL_DELIMITER;
import static org.red5.io.obu.OBUType.TILE_GROUP;
import static org.red5.io.obu.OBUType.TILE_LIST;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.red5.io.utils.HexDump;
import org.red5.io.utils.LEB128;
import org.red5.io.utils.LEB128.LEB128Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parsers OBU providing headers and extract relevant data. Logic is derived from the C code in the obuparser project.
 *
 * @author Paul Gregoire
 */
public class OBUParser {

    private static final Logger log = LoggerFactory.getLogger(OBUParser.class);

    private static final Set<Integer> VALID_OBU_TYPES = Set.of(SEQUENCE_HEADER.getValue(), TEMPORAL_DELIMITER.getValue(), FRAME_HEADER.getValue(), TILE_GROUP.getValue(), METADATA.getValue(), FRAME.getValue(), REDUNDANT_FRAME_HEADER.getValue(), TILE_LIST.getValue(), PADDING.getValue());

    /** Constant <code>OBU_START_FRAGMENT_BIT=(byte) 0b1000_0000</code> */
    public static final byte OBU_START_FRAGMENT_BIT = (byte) 0b1000_0000; // 0b1'0000'000

    /** Constant <code>OBU_END_FRAGMENT_BIT=0b0100_0000</code> */
    public static final byte OBU_END_FRAGMENT_BIT = 0b0100_0000;

    /** Constant <code>OBU_START_SEQUENCE_BIT=0b0000_1000</code> */
    public static final byte OBU_START_SEQUENCE_BIT = 0b0000_1000;

    /** Constant <code>OBU_COUNT_MASK=0b0011_0000</code> */
    public static final byte OBU_COUNT_MASK = 0b0011_0000;

    /** Constant <code>OBU_TYPE_MASK=0b0111_1000</code> */
    public static final byte OBU_TYPE_MASK = 0b0111_1000;

    /** Constant <code>OBU_SIZE_PRESENT_BIT=0b0000_0010</code> */
    public static final byte OBU_SIZE_PRESENT_BIT = 0b0000_0010; // 0b0'0000'010

    /** Constant <code>OBU_EXT_BIT=0b0000_0100</code> */
    public static final byte OBU_EXT_BIT = 0b0000_0100; // 0b0'0000'100

    /** Constant <code>OBU_EXT_S1T1_BIT=0b0010_1000</code> */
    public static final byte OBU_EXT_S1T1_BIT = 0b0010_1000; // 0b001'01'000

    /** Constant <code>OBU_TYPE_SHIFT=3</code> */
    public static final byte OBU_TYPE_SHIFT = 3;

    // constexpr uint8_t kAv1ObuTypeSequenceHeader = 1 << 3;
    // constexpr uint8_t kAv1ObuTypeTemporalDelimiter = 2 << 3;
    // constexpr uint8_t kAv1ObuTypeFrameHeader = 3 << 3;
    // constexpr uint8_t kAv1ObuTypeTileGroup = 4 << 3;
    // constexpr uint8_t kAv1ObuTypeMetadata = 5 << 3;
    // constexpr uint8_t kAv1ObuTypeFrame = 6 << 3;
    // constexpr uint8_t kAv1ObuTypeTileList = 8 << 3;
    // constexpr uint8_t kAv1ObuExtensionPresentBit = 0b0'0000'100;
    // constexpr uint8_t kAv1ObuSizePresentBit = 0b0'0000'010;
    // constexpr uint8_t kAv1ObuExtensionS1T1 = 0b001'01'000;

    /*
     * obp_get_next_obu parses the next OBU header in a packet containing a set of one or more OBUs
     * (e.g. an IVF or ISOBMFF packet) and returns its location in the buffer, as well as all
     * relevant data from the header.
     *
     * Input:
     *     buf      - Input packet buffer.
     *     buf_size - Size of the input packet buffer.
     *     err      - An error buffer and buffer size to write any error messages into.
     *
     * Output:
     *     obu_type    - The type of OBU.
     *     offset      - The offset into the buffer where this OBU starts, excluding the OBU header.
     *     obu_size    - The size of the OBU, excluding the size of the OBU header.
     *     temporal_id - The temporal ID of the OBU.
     *     spatial_id  - The spatial ID of the OBU.
     *
     * Returns:
     *     0 on success, -1 on error.
     */
    /**
     * <p>getNextObu.</p>
     *
     * @param buf an array of {@link byte} objects
     * @param offset a int
     * @param bufSize a int
     * @return a {@link org.red5.io.obu.OBUInfo} object
     * @throws org.red5.io.obu.OBUParseException if any.
     */
    public static OBUInfo getNextObu(byte[] buf, int offset, int bufSize) throws OBUParseException {
        //local variable declaration
        int pos = offset;
        int obuType = (buf[pos] & OBU_FRAME_TYPE_MASK) >>> OBU_FRAME_TYPE_BITSHIFT;
        OBUInfo info = new OBUInfo(OBUType.fromValue(obuType), ByteBuffer.allocate(1180));
        boolean obuExtensionFlag = obuHasExtension(buf[pos]);
        boolean obuHasSizeField = obuHasSize(buf[pos]);

        // log of the method
        log.trace("getNextObu - buffer length: {} size: {} offset: {}", buf.length, bufSize, offset);

        //check good application conditions
        if (bufSize < 1) {
            throw new OBUParseException("Buffer is too small to contain an OBU");
        }
        if (buf.length < (offset + 1)) {
            throw new OBUParseException("Buffer is too small for given offset");
        }
        if (!isValidObu(obuType)) {
            log.warn("OBU header contains invalid OBU type: {} data: {}", obuType, HexDump.byteArrayToHexString(buf));
            throw new OBUParseException("OBU header contains invalid OBU type: " + obuType);
        }

        log.trace("OBU type: {} extension? {} size field? {}", info.obuType, obuExtensionFlag, obuHasSizeField);
        pos++; // move past the OBU header

        if (obuExtensionFlag) {
            if (bufSize < pos + 1) {
                throw new OBUParseException("Buffer is too small to contain an OBU extension header");
            }
            info.temporalId = (buf[pos] & 0xE0) >> 5;
            info.spatialId = (buf[pos] & 0x18) >> 3;
            log.trace("Temporal id: {} spatial id: {}", info.temporalId, info.spatialId);
            pos++; // move past the OBU extension header
        }
        if (obuHasSizeField) {
            byte[] lengthBytes = new byte[buf[pos] == 127 ? 2 : 1];
            System.arraycopy(buf, pos, lengthBytes, 0, lengthBytes.length);
            LEB128Result result = LEB128.decode(lengthBytes);
            pos += result.bytesRead;
            info.size = result.value;
            log.trace("OBU had size field: {}", info.size);
        } else {
            info.size = bufSize - pos;
        }
        log.trace("OBU size: {}", info.size);
        info.data = ByteBuffer.wrap(Arrays.copyOfRange(buf, pos, (pos + info.size)));
        if (info.size > bufSize - pos) {
            throw new OBUParseException("Invalid OBU size: larger than remaining buffer");
        }
        return info;
    }

    /**
     * Returns true if the given OBU type value is valid.
     *
     * @param type the OBU type value
     * @return true if the given OBU type value is valid
     */
    public static boolean isValidObu(int type) {
        return VALID_OBU_TYPES.contains(type);
    }

    /**
     * Returns true if the given OBU type is valid.
     *
     * @param type the OBU type
     * @return true if the given OBU type is valid
     */
    public static boolean isValidObu(OBUType type) {
        switch (type) {
            case SEQUENCE_HEADER:
                //case TEMPORAL_DELIMITER: // not meant for rtp transport
            case FRAME_HEADER:
            case TILE_GROUP:
            case METADATA:
            case FRAME:
            case REDUNDANT_FRAME_HEADER:
                //case TILE_LIST: // not meant for rtp transport
                //case PADDING: // not meant for rtp transport
                return true;
            default:
                return false;
        }
    }

    /**
     * Returns true if the given byte starts a fragment. This is denoted as Z in the spec: MUST be set to 1 if the
     * first OBU element is an OBU fragment that is a continuation of an OBU fragment from the previous packet, and
     * MUST be set to 0 otherwise.
     *
     * @param aggregationHeader a byte
     * @return true if the given byte starts a fragment
     */
    public static boolean startsWithFragment(byte aggregationHeader) {
        return (aggregationHeader & OBU_START_FRAGMENT_BIT) != 0;
    }

    /**
     * Returns true if the given byte ends a fragment. This is denoted as Y in the spec: MUST be set to 1 if the last
     * OBU element is an OBU fragment that will continue in the next packet, and MUST be set to 0 otherwise.
     *
     * @param aggregationHeader a byte
     * @return true if the given byte ends a fragment
     */
    public static boolean endsWithFragment(byte aggregationHeader) {
        return (aggregationHeader & OBU_END_FRAGMENT_BIT) != 0;
    }

    /**
     * Returns true if the given byte is the starts a new sequence. This denoted as N in the spec: MUST be set to 1 if
     * the packet is the first packet of a coded video sequence, and MUST be set to 0 otherwise.
     *
     * @param aggregationHeader a byte
     * @return true if the given byte starts a new sequence
     */
    public static boolean startsNewCodedVideoSequence(byte aggregationHeader) {
        return (aggregationHeader & OBU_START_SEQUENCE_BIT) != 0;
    }

    /**
     * Returns expected number of OBU's.
     *
     * @param aggregationHeader a byte
     * @return expected number of OBU's
     */
    public static int obuCount(byte aggregationHeader) {
        return (aggregationHeader & OBU_COUNT_MASK) >> 4;
    }

    /**
     * Returns the OBU type from the given byte.
     *
     * @param obuHeader a byte
     * @return the OBU type
     */
    public static int obuType(byte obuHeader) {
        return (obuHeader & OBU_TYPE_MASK) >>> 3;
    }

    /**
     * Returns whether or not the OBU has an extension.
     *
     * @param obuHeader a byte
     * @return true if the OBU has an extension
     */
    public static boolean obuHasExtension(byte obuHeader) {
        return (obuHeader & OBU_EXT_BIT) != 0;
    }

    /**
     * Returns whether or not the OBU has a size.
     *
     * @param obuHeader a byte
     * @return true if the OBU has a size
     */
    public static boolean obuHasSize(byte obuHeader) {
        return (obuHeader & OBU_SIZE_PRESENT_BIT) != 0;
    }

}
