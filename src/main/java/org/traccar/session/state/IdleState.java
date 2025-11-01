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

import org.traccar.model.Device;
import org.traccar.model.Event;

import java.util.Date;

public class IdleState {

    public static IdleState fromDevice(Device device) {
        IdleState state = new IdleState();
        state.idleState = device.getIdleState();
        state.idleTime = device.getIdleTime();
        state.idlePositionId = device.getIdlePositionId();
        return state;
    }

    public void toDevice(Device device) {
        device.setIdleState(idleState);
        device.setIdleTime(idleTime);
        device.setIdlePositionId(idlePositionId);
    }

    private boolean changed;

    public boolean isChanged() {
        return changed;
    }

    private boolean idleState;

    public boolean getIdleState() {
        return idleState;
    }

    public void setIdleState(boolean idleState) {
        this.idleState = idleState;
        changed = true;
    }

    private long idlePositionId;

    public long getIdlePositionId() {
        return idlePositionId;
    }

    public void setIdlePositionId(long idlePositionId) {
        this.idlePositionId = idlePositionId;
        changed = true;
    }

    private Date idleTime;

    public Date getIdleTime() {
        return idleTime;
    }

    public void setIdleTime(Date idleTime) {
        this.idleTime = idleTime;
        changed = true;
    }

    private Event event;

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

}
