/*
 * Copyright 2016 - 2025 Anton Tananaev (anton@traccar.org)
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
package org.traccar.handler.events;

import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.config.Keys;
import org.traccar.helper.model.AttributeUtil;
import org.traccar.helper.model.PositionUtil;
import org.traccar.model.Device;
import org.traccar.model.Position;
import org.traccar.session.cache.CacheManager;
import org.traccar.session.state.IdleProcessor;
import org.traccar.session.state.IdleState;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

public class IdleEventHandler extends BaseEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(IdleEventHandler.class);

    private final CacheManager cacheManager;
    private final Storage storage;

    @Inject
    public IdleEventHandler(CacheManager cacheManager, Storage storage) {
        this.cacheManager = cacheManager;
        this.storage = storage;
    }

    @Override
    public void onPosition(Position position, Callback callback) {

        long deviceId = position.getDeviceId();
        Device device = cacheManager.getObject(Device.class, deviceId);
        if (device == null || !PositionUtil.isLatest(cacheManager, position)) {
            return;
        }

        boolean processInvalid = AttributeUtil.lookup(
                cacheManager, Keys.EVENT_MOTION_PROCESS_INVALID_POSITIONS, deviceId);
        if (!processInvalid && !position.getValid()) {
            return;
        }

        // Get minimal idle duration configuration
        var attributeProvider = new AttributeUtil.CacheProvider(cacheManager, deviceId);
        long minimalIdleDuration = AttributeUtil.lookup(
                attributeProvider, Keys.REPORT_IDLE_MINIMAL_DURATION) * 1000;

        IdleState state = IdleState.fromDevice(device);
        IdleProcessor.updateState(state, position, minimalIdleDuration, attributeProvider);

        if (state.isChanged()) {
            state.toDevice(device);
            try {
                storage.updateObject(device, new Request(
                        new Columns.Include("idleState", "idleTime", "idlePositionId"),
                        new Condition.Equals("id", device.getId())));
            } catch (StorageException e) {
                LOGGER.warn("Update device idle state error", e);
            }
        }

        if (state.getEvent() != null) {
            callback.eventDetected(state.getEvent());
        }
    }

}
