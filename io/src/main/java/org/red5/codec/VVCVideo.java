/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2023 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.codec;

import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.mina.core.buffer.IoBuffer;

/**
 * Red5 video codec for the VVC (H.266) video format. Stores DecoderConfigurationRecord and last keyframe.
 *
 * <p>The decoder configuration record format is specified in ISO/IEC 14496-15:2024, 11.2.4.2
 * (VVCDecoderConfigurationRecord).</p>
 *
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class VVCVideo extends SimpleVideo implements IEnhancedRTMPVideoCodec {

    /**
     * Video decoder configuration record to start the sequence. See ISO/IEC 14496-15:2024, 11.2.4.2 for the description of
     * VVCDecoderConfigurationRecord.
     */
    private FrameData decoderConfiguration;

    {
        codec = VideoCodec.VVC;
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
    public void handleNonEnhanced(VideoFrameType type, IoBuffer data, int timestamp) {
        // get the codecs frame type
        data.rewind();
        byte vvcType = data.get();
        // reset back to the beginning after we got the vvc type
        data.rewind();

        if (isDebug) {
            log.debug("VVC type: {}", vvcType);
        }
        switch (vvcType) {
            case 1: // keyframe
                // get the time stamp and compare with the current value
                if (timestamp != keyframeTimestamp) {
                    // new keyframe
                    keyframeTimestamp = timestamp;
                    // if its a new keyframe, clear keyframe and interframe collections
                    softReset();
                }
                // store keyframe
                keyframes.add(new FrameData(data));
                break;
            case 0: // configuration
                if (isDebug) {
                    log.debug("Decoder configuration");
                }
                // Store VVCDecoderConfigurationRecord data
                if (decoderConfiguration == null) {
                    decoderConfiguration = new FrameData(data);
                } else {
                    decoderConfiguration.setData(data);
                }
                // new configuration, clear keyframe and interframe collections
                softReset();
                break;
            default:
                if (bufferInterframes) {
                    if (isDebug) {
                        log.debug("Interframe - VVC type: {}", vvcType);
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
                break;
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("incomplete-switch")
    public void handleFrame(VideoPacketType packetType, VideoFrameType frameType, IoBuffer data, int timestamp) {
        switch (packetType) {
            case SequenceStart:
                if (frameType == VideoFrameType.KEYFRAME) {
                    if (isDebug) {
                        log.debug("Decoder configuration");
                    }
                    // Store VVCDecoderConfigurationRecord data
                    if (decoderConfiguration == null) {
                        decoderConfiguration = new FrameData(data);
                    } else {
                        decoderConfiguration.setData(data);
                    }
                    // new sequence, clear keyframe and interframe collections
                    softReset();
                }
                break;
            case CodedFramesX: // pass coded data without comp time offset
                switch (frameType) {
                    case KEYFRAME: // keyframe
                        if (isDebug) {
                            log.debug("Keyframe - keyframeTimestamp: {}", keyframeTimestamp);
                        }
                        // get the time stamp and compare with the current value
                        if (timestamp != keyframeTimestamp) {
                            // new keyframe
                            keyframeTimestamp = timestamp;
                            // if its a new keyframe, clear keyframe and interframe collections
                            softReset();
                        }
                        // store keyframe
                        keyframes.add(new FrameData(data));
                        break;
                    case INTERFRAME:
                        if (bufferInterframes) {
                            if (isDebug) {
                                log.debug("Interframe - timestamp: {}", timestamp);
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
                        break;
                }
                break;
            case CodedFrames: // pass coded data with SI24 composition time offset
                if (data.remaining() < 3) {
                    return;
                }
                int compTimeOffset = (data.get() << 16 | data.get() << 8 | data.get());
                switch (frameType) {
                    case KEYFRAME: // keyframe
                        if (isDebug) {
                            log.debug("Keyframe - keyframeTimestamp: {} compTimeOffset: {}", keyframeTimestamp, compTimeOffset);
                        }
                        keyframes.add(new FrameData(data, compTimeOffset));
                        break;
                    case INTERFRAME:
                        if (bufferInterframes) {
                            if (isDebug) {
                                log.debug("Interframe - compTimeOffset: {}", compTimeOffset);
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
                        break;
                }
                break;
            default:
                // not handled
                break;
        }
        data.rewind();
    }

    /** {@inheritDoc} */
    @Override
    public IoBuffer getDecoderConfiguration() {
        return decoderConfiguration != null ? decoderConfiguration.getFrame() : null;
    }

}
