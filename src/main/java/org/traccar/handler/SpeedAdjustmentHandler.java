/*
 * Copyright 2024 Anton Tananaev (anton@traccar.org)
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
package org.traccar.handler;

import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.model.Device;
import org.traccar.model.Position;
import org.traccar.session.cache.CacheManager;

public class SpeedAdjustmentHandler extends BasePositionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpeedAdjustmentHandler.class);

    public static final String ATTRIBUTE_SPEED_ADJUSTMENT_FACTOR = "speedAdjustmentFactor";

    private final CacheManager cacheManager;

    @Inject
    public SpeedAdjustmentHandler(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public void onPosition(Position position, Callback callback) {
        Device device = cacheManager.getObject(Device.class, position.getDeviceId());
        if (device != null) {
            Double speedAdjustmentFactor = device.getDouble(ATTRIBUTE_SPEED_ADJUSTMENT_FACTOR);
            LOGGER.info("SpeedAdjustmentHandler - Device: {}, Original Speed: {}, Factor: {}",
                    device.getId(), position.getSpeed(), speedAdjustmentFactor);
            if (speedAdjustmentFactor != null && speedAdjustmentFactor > 0) {
                double currentSpeed = position.getSpeed();
                double adjustedSpeed = currentSpeed * speedAdjustmentFactor;
                position.setSpeed(adjustedSpeed);
                LOGGER.info("Speed adjusted from {} to {}", currentSpeed, adjustedSpeed);
            } else {
                LOGGER.info("No speed adjustment - factor is null or invalid");
            }
        } else {
            LOGGER.warn("Device not found in cache for deviceId: {}", position.getDeviceId());
        }
        callback.processed(false);
    }

}
