# Traccar Idle Detection Feature - Complete Documentation

## Table of Contents
1. [Overview](#overview)
2. [Features](#features)
3. [Architecture](#architecture)
4. [Configuration](#configuration)
5. [API Endpoints](#api-endpoints)
6. [Database Schema](#database-schema)
7. [Real-Time Events](#real-time-events)
8. [Use Cases](#use-cases)
9. [Troubleshooting](#troubleshooting)

---

## Overview

The Idle Detection feature identifies periods when vehicles are stationary with the engine running, allowing fleet managers to:
- Track fuel waste from unnecessary idling
- Monitor driver behavior
- Reduce emissions
- Optimize fleet efficiency

### Key Distinction: IDLE vs STOP

| Feature | IDLE | STOP |
|---------|------|------|
| **Motion** | FALSE (not moving) | FALSE (not moving) |
| **Ignition** | **TRUE** (engine ON) | Any state (ON or OFF) |
| **Fuel Impact** | High (engine running) | Low (engine off) |
| **Use Case** | Fuel waste monitoring | Parking/waiting analysis |

---

## Features

### 1. On-Demand Idle Reports
- Calculate idle periods from historical position data
- Export to JSON or Excel format
- Email reports directly
- Filter by device or device group
- Configurable date ranges

### 2. Real-Time Idle Events
- Generate events when idle starts (`deviceIdleStart`)
- Generate events when idle ends (`deviceIdleEnd`)
- Store events in database for notifications
- Fast report generation using stored events

### 3. Advanced Filtering Thresholds
- **Minimal Duration**: Filter out short idles (e.g., traffic lights)
- **Minimal Engine Hours**: Focus on extended engine runtime
- **Minimal Fuel Consumption**: Identify high fuel waste periods

### 4. Comprehensive Data
- Idle duration (milliseconds)
- Location (latitude/longitude)
- Geocoded address
- Engine hours during idle
- Fuel consumed during idle
- Odometer readings

---

## Architecture

### Component Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                         CLIENT REQUEST                           │
│           GET /api/reports/idle?deviceId=1&from=...              │
└──────────────────────┬──────────────────────────────────────────┘
                       ▼
┌─────────────────────────────────────────────────────────────────┐
│                   ReportResource.java                            │
│                    (API Endpoint)                                │
└──────────────────────┬──────────────────────────────────────────┘
                       ▼
┌─────────────────────────────────────────────────────────────────┐
│                  IdleReportProvider.java                         │
│                  (Report Generation)                             │
└──────────────────────┬──────────────────────────────────────────┘
                       ▼
┌─────────────────────────────────────────────────────────────────┐
│              ReportUtils.detectIdle()                            │
│              (Detection Algorithm)                               │
│                                                                  │
│  1. Load configuration thresholds                                │
│  2. Query tc_positions for device positions                      │
│  3. Analyze motion & ignition states                             │
│  4. Detect idle periods (motion=F, ignition=T)                   │
│  5. Apply duration/fuel/engine hour filters                      │
│  6. Calculate idle details (geocode, fuel, etc.)                 │
│  7. Return filtered idle periods                                 │
└──────────────────────┬──────────────────────────────────────────┘
                       ▼
┌─────────────────────────────────────────────────────────────────┐
│                   IdleReportItem[]                               │
│                   (JSON Response)                                │
└─────────────────────────────────────────────────────────────────┘
```

### Real-Time Event Flow

```
┌─────────────────────────────────────────────────────────────────┐
│               Position arrives from GPS device                    │
└──────────────────────┬──────────────────────────────────────────┘
                       ▼
┌─────────────────────────────────────────────────────────────────┐
│                  ProcessingHandler                               │
│            (Event processing pipeline)                           │
└──────────────────────┬──────────────────────────────────────────┘
                       ▼
┌─────────────────────────────────────────────────────────────────┐
│                IdleEventHandler                                  │
│                                                                  │
│  1. Load IdleState from Device                                   │
│  2. Call IdleProcessor.updateState()                             │
│  3. Detect idle state change                                     │
│  4. Check duration threshold                                     │
│  5. Generate Event (deviceIdleStart/deviceIdleEnd)               │
│  6. Update Device idle state                                     │
│  7. Trigger notifications                                        │
└──────────────────────┬──────────────────────────────────────────┘
                       ▼
┌─────────────────────────────────────────────────────────────────┐
│           Event stored in tc_events table                        │
│     Available for notifications & fast reports                   │
└─────────────────────────────────────────────────────────────────┘
```

---

## Configuration

### Global Configuration (traccar.xml)

Add these entries to configure idle detection globally:

```xml
<!-- Minimal idle duration (seconds) -->
<entry key="report.idle.minimalDuration">300</entry>

<!-- Minimal engine hours (seconds) - 0 to disable -->
<entry key="report.idle.minimalEngineHours">0</entry>

<!-- Minimal fuel consumption (liters) - 0 to disable -->
<entry key="report.idle.minimalFuel">0.0</entry>
```

### Configuration Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `report.idle.minimalDuration` | Long (seconds) | 300 (5 min) | Minimum idle duration to report |
| `report.idle.minimalEngineHours` | Long (seconds) | 0 (disabled) | Filter idles with less engine runtime |
| `report.idle.minimalFuel` | Double (liters) | 0.0 (disabled) | Filter idles with less fuel consumption |

### Device-Level Configuration

Override global settings for specific devices using device attributes:

1. Go to **Traccar Web UI** → **Settings** → **Devices**
2. Select device → **Attributes** tab
3. Add attributes:
   - Key: `report.idle.minimalDuration`, Value: `600` (10 minutes)
   - Key: `report.idle.minimalEngineHours`, Value: `1200` (20 minutes)
   - Key: `report.idle.minimalFuel`, Value: `0.5` (0.5 liters)

### Configuration Examples

#### Example 1: Filter Short Traffic Light Idles
```xml
<entry key="report.idle.minimalDuration">600</entry>  <!-- 10 minutes -->
```
**Result**: Only report idles ≥ 10 minutes

#### Example 2: Track Significant Fuel Waste
```xml
<entry key="report.idle.minimalDuration">300</entry>   <!-- 5 minutes -->
<entry key="report.idle.minimalFuel">0.3</entry>       <!-- 0.3 liters -->
```
**Result**: Report idles ≥ 5 min that consumed ≥ 0.3L fuel

#### Example 3: Extended Engine Runtime Monitoring
```xml
<entry key="report.idle.minimalDuration">300</entry>        <!-- 5 minutes -->
<entry key="report.idle.minimalEngineHours">1200</entry>    <!-- 20 minutes -->
```
**Result**: Report idles ≥ 5 min where engine ran ≥ 20 min

---

## API Endpoints

### 1. Get Idle Report (JSON)

**Endpoint**: `GET /api/reports/idle`

**Parameters**:
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `deviceId` | Long[] | Yes* | Device ID(s) to report |
| `groupId` | Long[] | Yes* | Group ID(s) to report |
| `from` | Date | Yes | Start date (ISO 8601) |
| `to` | Date | Yes | End date (ISO 8601) |

*At least one of `deviceId` or `groupId` is required

**Example Request**:
```bash
curl -X GET 'http://localhost:8082/api/reports/idle' \
  -H 'Authorization: Bearer YOUR_TOKEN' \
  -G \
  --data-urlencode 'deviceId=1' \
  --data-urlencode 'from=2025-11-01T00:00:00Z' \
  --data-urlencode 'to=2025-11-01T23:59:59Z'
```

**Example Response**:
```json
[
  {
    "deviceId": 1,
    "deviceName": "Vehicle ABC-123",
    "positionId": 12345,
    "latitude": 27.7172,
    "longitude": 85.3240,
    "address": "Thamel, Kathmandu 44600, Nepal",
    "startTime": "2025-11-01T08:10:00Z",
    "endTime": "2025-11-01T08:25:00Z",
    "duration": 900000,
    "engineHours": 900000,
    "startOdometer": 125678.5,
    "endOdometer": 125678.5,
    "spentFuel": 0.5
  }
]
```

### 2. Get Idle Report (Excel)

**Endpoint**: `GET /api/reports/idle/xlsx`

**Parameters**: Same as JSON endpoint

**Example Request**:
```bash
curl -X GET 'http://localhost:8082/api/reports/idle/xlsx' \
  -H 'Authorization: Bearer YOUR_TOKEN' \
  -G \
  --data-urlencode 'deviceId=1' \
  --data-urlencode 'from=2025-11-01T00:00:00Z' \
  --data-urlencode 'to=2025-11-01T23:59:59Z' \
  --output idle_report.xlsx
```

**Response**: Excel file (.xlsx)

### 3. Email Idle Report

**Endpoint**: `GET /api/reports/idle/mail`

**Parameters**: Same as JSON endpoint

**Example Request**:
```bash
curl -X GET 'http://localhost:8082/api/reports/idle/mail' \
  -H 'Authorization: Bearer YOUR_TOKEN' \
  -G \
  --data-urlencode 'deviceId=1' \
  --data-urlencode 'from=2025-11-01T00:00:00Z' \
  --data-urlencode 'to=2025-11-01T23:59:59Z'
```

**Response**:
```json
{
  "status": "success",
  "message": "Report sent to configured email"
}
```

---

## Database Schema

### tc_devices Table (New Columns)

```sql
ALTER TABLE tc_devices ADD COLUMN idlestate BOOLEAN DEFAULT FALSE;
ALTER TABLE tc_devices ADD COLUMN idlepositionid BIGINT;
ALTER TABLE tc_devices ADD COLUMN idletime TIMESTAMP;
```

| Column | Type | Description |
|--------|------|-------------|
| `idlestate` | BOOLEAN | Current idle state (true = idling) |
| `idlepositionid` | BIGINT | Position ID where idle started |
| `idletime` | TIMESTAMP | Time when idle started |

### tc_events Table (Event Types)

Idle events are stored with these types:

| Event Type | Description | Trigger |
|------------|-------------|---------|
| `deviceIdleStart` | Idle period started | Vehicle stopped moving, ignition ON, duration threshold met |
| `deviceIdleEnd` | Idle period ended | Vehicle starts moving or ignition turns OFF |

**Event Attributes**:
- `positionId`: Position where event occurred
- `eventTime`: When event was triggered
- `duration`: Idle duration (milliseconds)

---

## Real-Time Events

### How It Works

1. **Position Arrives**: GPS device sends position data
2. **IdleEventHandler Processes**:
   - Checks motion (from Position.KEY_MOTION)
   - Checks ignition (from Position.KEY_IGNITION)
   - Determines idle state: `!motion && ignition`
3. **State Change Detection**:
   - Idle started: Store position ID and time
   - Idle ended: Calculate duration
4. **Event Generation**:
   - If duration ≥ threshold → Generate event
   - Store in `tc_events` table
5. **Notifications**:
   - Trigger email/SMS/webhook notifications
   - Update dashboard/UI

### Configuring Notifications

1. Go to **Traccar Web UI** → **Settings** → **Notifications**
2. Create new notification:
   - Type: **Web**, **Mail**, or **SMS**
   - Event: **Device Idle Start** or **Device Idle End**
   - Select devices/groups
3. Configure message template

**Example Email Template**:
```
Vehicle {deviceName} has been idling at {address} for {duration} minutes.
Engine has been running unnecessarily.
Location: {latitude}, {longitude}
Time: {eventTime}
```

---

## Use Cases

### 1. Fuel Waste Reduction
**Problem**: Drivers leaving engines running during breaks

**Solution**:
```xml
<entry key="report.idle.minimalDuration">300</entry>
<entry key="report.idle.minimalFuel">0.3</entry>
```
- Report only idles that wasted ≥ 0.3L fuel
- Identify worst offenders
- Calculate monthly fuel waste cost

### 2. Driver Behavior Monitoring
**Problem**: Excessive idling habits

**Solution**:
- Daily idle reports per driver
- Real-time alerts for long idles
- Driver performance scorecards

### 3. Environmental Compliance
**Problem**: Reduce emissions from idling

**Solution**:
- Track total idle time per vehicle
- Monitor idle reduction progress
- Report to environmental authorities

### 4. Fleet Efficiency Optimization
**Problem**: Identify inefficient operations

**Solution**:
- Compare idle times across vehicles
- Find operational bottlenecks
- Optimize routes/schedules

---

## Troubleshooting

### Issue: No idle periods detected

**Possible Causes**:
1. **Thresholds too high**: Check `report.idle.minimalDuration`
2. **Missing ignition data**: Device not sending `Position.KEY_IGNITION`
3. **Motion detection issues**: Check `Position.KEY_MOTION` attribute

**Solution**:
```bash
# Check raw position data
SELECT fixTime, attributes
FROM tc_positions
WHERE deviceId = 1
ORDER BY fixTime DESC
LIMIT 10;

# Look for 'motion' and 'ignition' in attributes JSON
```

### Issue: Events not generating

**Possible Causes**:
1. **IdleEventHandler not registered**: Check `ProcessingHandler.java`
2. **Database migration not run**: Check `tc_devices` table for idle columns
3. **Invalid positions**: Device sending invalid GPS data

**Solution**:
```bash
# Check if idle columns exist
DESCRIBE tc_devices;

# Check for errors in logs
tail -f /opt/traccar/logs/tracker-server.log | grep -i idle
```

### Issue: Incorrect idle durations

**Possible Causes**:
1. **Time zone issues**: Device vs server time mismatch
2. **Position frequency**: Low GPS update frequency
3. **Motion detection sensitivity**: Too sensitive or not sensitive enough

**Solution**:
- Configure device time zone in device attributes
- Increase GPS reporting frequency
- Adjust motion detection parameters

### Issue: Excel report empty

**Possible Causes**:
1. **All idles filtered out**: Thresholds too strict
2. **Template file missing**: `idle.xlsx` not in templates/export/
3. **Permission issues**: User lacks report access

**Solution**:
```bash
# Check if template exists
ls -l /opt/traccar/templates/export/idle.xlsx

# Temporarily disable filters
<entry key="report.idle.minimalDuration">60</entry>  <!-- 1 minute -->
```

---

## Performance Considerations

### Small Fleet (< 50 vehicles)
- Default configuration works well
- On-demand calculation is fast
- No optimization needed

### Medium Fleet (50-500 vehicles)
- Consider using idle events for long date ranges
- Configure appropriate thresholds to reduce data
- Use device groups for reports

### Large Fleet (> 500 vehicles)
- **Recommended**: Use fast method with events
- Schedule reports during off-peak hours
- Use database indexes on `tc_positions.fixTime`
- Consider archiving old position data

### Optimization Tips

1. **Limit date ranges**: Query 1-7 days at a time
2. **Use appropriate thresholds**: Filter unnecessary data
3. **Index database**:
   ```sql
   CREATE INDEX idx_positions_device_time
   ON tc_positions(deviceId, fixTime);
   ```
4. **Archive old data**: Move old positions to archive table

---

## Migration Guide

### From No Idle Tracking

1. **Update Traccar**:
   ```bash
   cd /opt/traccar
   git pull
   ./gradlew build
   ```

2. **Run Database Migration**:
   - Liquibase will automatically apply `changelog-idle.xml`
   - Verify with: `DESCRIBE tc_devices;`

3. **Configure Thresholds**:
   - Add to `traccar.xml`
   - Restart Traccar: `sudo systemctl restart traccar`

4. **Test**:
   - Wait for positions to arrive
   - Check for idle events in `tc_events`
   - Generate test report

### From Custom Idle Implementation

1. **Data Migration**:
   - Export existing idle data
   - Map to new `IdleReportItem` format
   - Import if needed

2. **Configuration Mapping**:
   - Map old thresholds to new keys
   - Test thoroughly before production

3. **Notification Migration**:
   - Recreate notifications for new event types
   - Test notification delivery

---

## Support

For issues or questions:
- **GitHub**: https://github.com/traccar/traccar/issues
- **Forum**: https://www.traccar.org/forums/
- **Email**: support@traccar.org

---

## License

Copyright 2025 Anton Tananaev (anton@traccar.org)

Licensed under the Apache License, Version 2.0
