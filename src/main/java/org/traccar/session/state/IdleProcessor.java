/*
 * Copyright 2022 - 2025 Anton Tananaev (anton@traccar.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.session.state;

import org.traccar.model.Event;
import org.traccar.model.Position;
import org.traccar.helper.model.AttributeUtil;

public final class IdleProcessor {

    private IdleProcessor() {
    }

    public static void updateState(
            IdleState state, Position position, long minimalIdleDuration,
            AttributeUtil.CacheProvider attributeProvider) {

        state.setEvent(null);

        boolean motion = position.getBoolean(Position.KEY_MOTION);
        boolean ignition = position.getBoolean(Position.KEY_IGNITION);

        // Idle condition: not moving AND ignition is ON
        boolean currentlyIdle = !motion && ignition;

        boolean previouslyIdle = state.getIdleState();

        if (previouslyIdle == currentlyIdle) {
            // State hasn't changed, check if we should generate event
            if (state.getIdleTime() != null && !previouslyIdle) {
                // Was idling, now not idling anymore - check duration
                long oldTime = state.getIdleTime().getTime();
                long newTime = position.getFixTime().getTime();
                long duration = newTime - oldTime;

                if (duration >= minimalIdleDuration) {
                    // Idle period ended, generate idle end event
                    Event event = new Event(Event.TYPE_DEVICE_IDLE_END, position.getDeviceId());
                    event.setPositionId(state.getIdlePositionId());
                    event.setEventTime(state.getIdleTime());
                    event.set("duration", duration);

                    state.setIdlePositionId(0);
                    state.setIdleTime(null);
                    state.setEvent(event);
                }
            } else if (state.getIdleTime() != null && previouslyIdle) {
                // Still idling - no event yet, waiting for end
                // Event will be generated when idle state changes
            }
        } else {
            // State changed
            state.setIdleState(currentlyIdle);

            if (currentlyIdle) {
                // Started idling
                state.setIdlePositionId(position.getId());
                state.setIdleTime(position.getFixTime());
                // Don't generate event yet - wait for minimum duration
            } else {
                // Stopped idling
                if (state.getIdleTime() != null) {
                    long oldTime = state.getIdleTime().getTime();
                    long newTime = position.getFixTime().getTime();
                    long duration = newTime - oldTime;

                    if (duration >= minimalIdleDuration) {
                        // Generate idle start event (for the period that just ended)
                        Event startEvent = new Event(Event.TYPE_DEVICE_IDLE_START, position.getDeviceId());
                        startEvent.setPositionId(state.getIdlePositionId());
                        startEvent.setEventTime(state.getIdleTime());
                        startEvent.set("duration", duration);
                        state.setEvent(startEvent);
                    }

                    state.setIdlePositionId(0);
                    state.setIdleTime(null);
                }
            }
        }
    }

}
