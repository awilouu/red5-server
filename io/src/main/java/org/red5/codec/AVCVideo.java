/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2023 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.codec;

import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.io.IoConstants;
import org.red5.util.ByteNibbler;

/**
 * Red5 video codec for the AVC (h264) video format. Stores DecoderConfigurationRecord and last keyframe.
 *
 * @author Tiago Jacobs (tiago@imdt.com.br)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class AVCVideo extends SimpleVideo {

    /**
     * Video decoder configuration record to start the sequence. See ISO/IEC 14496-15, 5.2.4.1 for the description of
     * AVCDecoderConfigurationRecord
     */
    private FrameData decoderConfiguration;

    {
        codec = VideoCodec.AVC;
    }

    /** {@inheritDoc} */
    @Override
    public boolean canDropFrames() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public void reset() {
        softReset();
    }

    /** {@inheritDoc} */
    @SuppressWarnings("incomplete-switch")
    @Override
    public boolean addData(IoBuffer data, int timestamp) {
        log.trace("{} addData timestamp: {} remaining: {}", codec.name(), timestamp, data.remaining());
        boolean result;

        if (data.position() > 0) {
            data.rewind();
        }
        if (!data.hasRemaining()) {
            return false;
        }

        data.mark();
        byte flg = data.get();
        enhanced = ByteNibbler.isBitSet(flg, 7);
        int ft = ((flg & 0b01110000) >> 4);
        frameType = VideoFrameType.valueOf(ft);

        if (enhanced) {
            result = EnhancedDataCase(data, timestamp, flg);
        }
        else { result = notEnhancedDataCase(data, timestamp, flg);}

        data.rewind();
        if (!result) {
            logRejection(data);
        }
        return result;
    }

    private boolean EnhancedDataCase(IoBuffer data, int timestamp, byte flg) {
        packetType = VideoPacketType.valueOf(flg & IoConstants.MASK_VIDEO_CODEC);
        int fourcc = readAndValidateFourcc(data);
        if (fourcc < 0) {
            data.reset();
            return false;
        }
        data.reset();
        if (isDebug) {
            log.debug("{} - frame type: {} packet type: {}", VideoCodec.valueOfByFourCc(fourcc), frameType, packetType);
        }
        packetCase(data, timestamp);
        return true;
    }

    /**
     * Reads and validates the fourcc field for enhanced packets.
     *
     * @return the fourcc value, or -1 if validation failed (buffer too short or codec mismatch)
     */
    private int readAndValidateFourcc(IoBuffer data) {
        if (frameType.getValue() >= 5 || packetType.getPacketType() >= 5) {
            // no fourcc needed for these packet types
            return codec.getFourcc();
        }
        if (data.remaining() < 4) {
            return -1;
        }
        int fourcc = data.getInt();
        return (codec.getFourcc() == fourcc) ? fourcc : -1;
    }

    private void packetCase(IoBuffer data, int timestamp) {
        switch (packetType) {
            case SequenceStart:
                caseSequenceStart(data);
                break;
            case CodedFramesX:
                caseCodedFrames(data, timestamp, false);
                break;
            case CodedFrames:
                caseCodedFrames(data, timestamp, true);
                break;
            default:
                // not handled
                break;
        }
    }

    private void caseSequenceStart(IoBuffer data) {
        if (frameType != VideoFrameType.KEYFRAME) {
            return;
        }
        if (isDebug) {
            log.debug("Decoder configuration");
        }
        storeDecoderConfiguration(data);
        softReset();
    }

    private void caseCodedFrames(IoBuffer data, int timestamp, boolean withCompTimeOffset) {
        int compTimeOffset = 0;
        if (withCompTimeOffset) {
            if (data.remaining() < 3) {
                return;
            }
            compTimeOffset = (data.get() << 16 | data.get() << 8 | data.get());
            data.reset();
        }
        switch (frameType) {
            case KEYFRAME:
                storeKeyframe(data, timestamp, compTimeOffset);
                break;
            case INTERFRAME:
                storeInterframe(data);
                break;
            default:
                break;
        }
    }

    private boolean notEnhancedDataCase(IoBuffer data, int timestamp, byte flg) {
        if ((flg & IoConstants.MASK_VIDEO_CODEC) != codec.getId()) {
            return false;
        }
        byte avcType = data.get();
        data.reset();
        if (isDebug) {
            log.debug("AVC type: {}", avcType);
        }
        notEnhancedAvcType(data, timestamp, flg, avcType);
        return true;
    }

    private void notEnhancedAvcType(IoBuffer data, int timestamp, byte flg, byte avcType) {
        switch (avcType) {
            case 1: // VCL video coding layer
                frameType = VideoFrameType.valueOf((flg & IoConstants.MASK_VIDEO_FRAMETYPE) >> 4);
                codedFrameOnType(data, timestamp, avcType);
                break;
            case 0: // configuration
                if (isDebug) {
                    log.debug("Decoder configuration");
                }
                storeDecoderConfiguration(data);
                softReset();
                break;
            default:
                break;
        }
    }

    private void codedFrameOnType(IoBuffer data, int timestamp, byte avcType) {
        switch (frameType) {
            case KEYFRAME:
                storeKeyframe(data, timestamp, 0);
                break;
            case INTERFRAME:
                if (bufferInterframes) {
                    if (isDebug) {
                        log.debug("Interframe - AVC type: {}", avcType);
                    }
                    storeInterframe(data);
                }
                break;
            default:
                break;
        }
    }

    private void storeKeyframe(IoBuffer data, int timestamp, int compTimeOffset) {
        if (isDebug) {
            log.debug("Keyframe - keyframeTimestamp: {}", keyframeTimestamp);
        }
        if (timestamp != keyframeTimestamp) {
            keyframeTimestamp = timestamp;
            softReset();
        }
        keyframes.add(compTimeOffset != 0 ? new FrameData(data, compTimeOffset) : new FrameData(data));
    }

    private void storeInterframe(IoBuffer data) {
        if (!bufferInterframes) {
            return;
        }
        if (isDebug) {
            log.debug("Interframe - timestamp buffering");
        }
        if (interframes == null) {
            interframes = new CopyOnWriteArrayList<>();
        }
        try {
            int lastInterframe = numInterframes.getAndIncrement();
            if (lastInterframe < interframes.size()) {
                interframes.get(lastInterframe).setData(data);
            } else {
                interframes.add(new FrameData(data));
            }
        } catch (Throwable e) {
            log.warn("Failed to buffer interframe", e);
        }
    }

    private void storeDecoderConfiguration(IoBuffer data) {
        if (decoderConfiguration == null) {
            decoderConfiguration = new FrameData(data);
        } else {
            decoderConfiguration.setData(data);
        }
    }

    private void logRejection(IoBuffer data) {
        byte[] peek = new byte[Math.min(8, data.remaining())];
        data.get(peek);
        data.rewind();
        log.warn("AVC rejected - first bytes: {} enhanced: {} frameType: {} packetType: {}",
                ByteNibbler.toHexString(peek), enhanced, frameType, packetType);
    }


    /** {@inheritDoc} */
    @Override
    public IoBuffer getDecoderConfiguration() {
        return decoderConfiguration != null ? decoderConfiguration.getFrame() : null;
    }

}
