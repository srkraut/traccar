# Traccar Idle Reports API Documentation

## Base URL

```
http://your-traccar-server:8082/api
```

## Authentication

All API requests require authentication using Bearer token or Basic Auth.

```bash
# Bearer Token
-H 'Authorization: Bearer YOUR_TOKEN'

# Basic Auth
-u 'username:password'
```

---

## Endpoints

### 1. GET /reports/idle

Get idle report data in JSON format.

#### Request

**Method**: `GET`

**URL**: `/api/reports/idle`

**Query Parameters**:

| Parameter | Type | Required | Description | Example |
|-----------|------|----------|-------------|---------|
| `deviceId` | Long[] | Yes* | One or more device IDs | `deviceId=1` or `deviceId=1&deviceId=2` |
| `groupId` | Long[] | Yes* | One or more group IDs | `groupId=5` |
| `from` | DateTime (ISO 8601) | Yes | Report start time | `2025-11-01T00:00:00Z` |
| `to` | DateTime (ISO 8601) | Yes | Report end time | `2025-11-01T23:59:59Z` |

*At least one of `deviceId` or `groupId` must be provided

#### Response

**Status Code**: `200 OK`

**Content-Type**: `application/json`

**Response Body**: Array of `IdleReportItem` objects

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

#### IdleReportItem Schema

| Field | Type | Description |
|-------|------|-------------|
| `deviceId` | Long | Device database ID |
| `deviceName` | String | Human-readable device name |
| `positionId` | Long | Position ID where idle started |
| `latitude` | Double | Latitude of idle location |
| `longitude` | Double | Longitude of idle location |
| `address` | String | Geocoded address (if geocoder enabled) |
| `startTime` | DateTime | When idle period started |
| `endTime` | DateTime | When idle period ended |
| `duration` | Long | Idle duration in milliseconds |
| `engineHours` | Long | Engine runtime during idle in milliseconds |
| `startOdometer` | Double | Odometer reading at idle start (km) |
| `endOdometer` | Double | Odometer reading at idle end (km) |
| `spentFuel` | Double | Fuel consumed during idle (liters) |

#### Examples

**Example 1: Single Device**
```bash
curl -X GET 'http://localhost:8082/api/reports/idle' \
  -H 'Authorization: Bearer YOUR_TOKEN' \
  -G \
  --data-urlencode 'deviceId=1' \
  --data-urlencode 'from=2025-11-01T00:00:00Z' \
  --data-urlencode 'to=2025-11-01T23:59:59Z'
```

**Example 2: Multiple Devices**
```bash
curl -X GET 'http://localhost:8082/api/reports/idle' \
  -H 'Authorization: Bearer YOUR_TOKEN' \
  -G \
  --data-urlencode 'deviceId=1' \
  --data-urlencode 'deviceId=2' \
  --data-urlencode 'deviceId=3' \
  --data-urlencode 'from=2025-11-01T00:00:00Z' \
  --data-urlencode 'to=2025-11-01T23:59:59Z'
```

**Example 3: Device Group**
```bash
curl -X GET 'http://localhost:8082/api/reports/idle' \
  -H 'Authorization: Bearer YOUR_TOKEN' \
  -G \
  --data-urlencode 'groupId=5' \
  --data-urlencode 'from=2025-11-01T00:00:00Z' \
  --data-urlencode 'to=2025-11-02T00:00:00Z'
```

**Example 4: Using Basic Auth**
```bash
curl -X GET 'http://localhost:8082/api/reports/idle' \
  -u 'admin:admin' \
  -G \
  --data-urlencode 'deviceId=1' \
  --data-urlencode 'from=2025-11-01T00:00:00Z' \
  --data-urlencode 'to=2025-11-01T23:59:59Z'
```

#### Error Responses

**401 Unauthorized** - Invalid or missing authentication
```json
{
  "error": "Unauthorized"
}
```

**403 Forbidden** - User doesn't have access to requested devices
```json
{
  "error": "Forbidden - No access to device"
}
```

**400 Bad Request** - Invalid parameters
```json
{
  "error": "Missing required parameter: from"
}
```

---

### 2. GET /reports/idle/xlsx

Get idle report as Excel file (.xlsx).

#### Request

**Method**: `GET`

**URL**: `/api/reports/idle/xlsx`

**Query Parameters**: Same as JSON endpoint

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `deviceId` | Long[] | Yes* | Device ID(s) |
| `groupId` | Long[] | Yes* | Group ID(s) |
| `from` | DateTime | Yes | Start time |
| `to` | DateTime | Yes | End time |

#### Response

**Status Code**: `200 OK`

**Content-Type**: `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`

**Content-Disposition**: `attachment; filename="idle_report.xlsx"`

**Response Body**: Excel file (binary)

#### Examples

**Example 1: Download Excel File**
```bash
curl -X GET 'http://localhost:8082/api/reports/idle/xlsx' \
  -H 'Authorization: Bearer YOUR_TOKEN' \
  -G \
  --data-urlencode 'deviceId=1' \
  --data-urlencode 'from=2025-11-01T00:00:00Z' \
  --data-urlencode 'to=2025-11-01T23:59:59Z' \
  --output idle_report.xlsx
```

**Example 2: Python Script**
```python
import requests
from datetime import datetime, timedelta

# Configuration
BASE_URL = 'http://localhost:8082/api'
TOKEN = 'YOUR_TOKEN'
DEVICE_ID = 1

# Date range (last 7 days)
end_date = datetime.now()
start_date = end_date - timedelta(days=7)

# Make request
response = requests.get(
    f'{BASE_URL}/reports/idle/xlsx',
    headers={'Authorization': f'Bearer {TOKEN}'},
    params={
        'deviceId': DEVICE_ID,
        'from': start_date.isoformat() + 'Z',
        'to': end_date.isoformat() + 'Z'
    }
)

# Save file
if response.status_code == 200:
    with open('idle_report.xlsx', 'wb') as f:
        f.write(response.content)
    print('Report downloaded successfully')
else:
    print(f'Error: {response.status_code}')
```

**Example 3: JavaScript (Node.js)**
```javascript
const axios = require('axios');
const fs = require('fs');

const downloadIdleReport = async () => {
    const response = await axios.get('http://localhost:8082/api/reports/idle/xlsx', {
        headers: {
            'Authorization': 'Bearer YOUR_TOKEN'
        },
        params: {
            deviceId: 1,
            from: '2025-11-01T00:00:00Z',
            to: '2025-11-01T23:59:59Z'
        },
        responseType: 'arraybuffer'
    });

    fs.writeFileSync('idle_report.xlsx', response.data);
    console.log('Report downloaded');
};

downloadIdleReport();
```

---

### 3. GET /reports/idle/mail

Email idle report to configured email address.

#### Request

**Method**: `GET`

**URL**: `/api/reports/idle/mail`

**Query Parameters**: Same as JSON endpoint

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `deviceId` | Long[] | Yes* | Device ID(s) |
| `groupId` | Long[] | Yes* | Group ID(s) |
| `from` | DateTime | Yes | Start time |
| `to` | DateTime | Yes | End time |

#### Response

**Status Code**: `204 No Content`

**Response Body**: Empty (report sent via email)

#### Prerequisites

1. **Configure SMTP** in `traccar.xml`:
```xml
<entry key='mail.smtp.host'>smtp.gmail.com</entry>
<entry key='mail.smtp.port'>587</entry>
<entry key='mail.smtp.starttls.enable'>true</entry>
<entry key='mail.smtp.from'>traccar@example.com</entry>
<entry key='mail.smtp.auth'>true</entry>
<entry key='mail.smtp.username'>your-email@gmail.com</entry>
<entry key='mail.smtp.password'>your-password</entry>
```

2. **Set User Email** in device/user settings

#### Examples

**Example 1: Send Email Report**
```bash
curl -X GET 'http://localhost:8082/api/reports/idle/mail' \
  -H 'Authorization: Bearer YOUR_TOKEN' \
  -G \
  --data-urlencode 'deviceId=1' \
  --data-urlencode 'from=2025-11-01T00:00:00Z' \
  --data-urlencode 'to=2025-11-01T23:59:59Z'
```

**Example 2: Automated Daily Report (Cron)**
```bash
#!/bin/bash
# daily_idle_report.sh

TODAY=$(date +%Y-%m-%d)
YESTERDAY=$(date -d "yesterday" +%Y-%m-%d)

curl -X GET 'http://localhost:8082/api/reports/idle/mail' \
  -H 'Authorization: Bearer YOUR_TOKEN' \
  -G \
  --data-urlencode 'deviceId=1' \
  --data-urlencode "from=${YESTERDAY}T00:00:00Z" \
  --data-urlencode "to=${TODAY}T00:00:00Z"
```

**Cron Entry**:
```cron
# Send daily idle report at 8 AM
0 8 * * * /path/to/daily_idle_report.sh
```

---

## Rate Limiting

- **Default Limit**: 100 requests per minute per user
- **Response Header**: `X-RateLimit-Remaining`
- **Exceeded Response**: `429 Too Many Requests`

## Pagination

Idle reports do not support pagination. To handle large datasets:
- Query shorter time periods
- Use device groups to batch requests
- Filter with appropriate thresholds

## Filtering

Idle periods are automatically filtered based on configuration:

### Server-Side Filters

These are configured in `traccar.xml` or device attributes:

| Filter | Config Key | Default |
|--------|------------|---------|
| Minimal Duration | `report.idle.minimalDuration` | 300s |
| Minimal Engine Hours | `report.idle.minimalEngineHours` | 0s (disabled) |
| Minimal Fuel | `report.idle.minimalFuel` | 0.0L (disabled) |

### Client-Side Filtering

You can further filter results in your application:

```javascript
// Filter idles longer than 10 minutes
const longIdles = idles.filter(idle => idle.duration > 600000);

// Filter by location (within bounding box)
const katmanduIdles = idles.filter(idle =>
    idle.latitude >= 27.6 && idle.latitude <= 27.8 &&
    idle.longitude >= 85.2 && idle.longitude <= 85.4
);

// Filter by fuel consumption
const wastefulIdles = idles.filter(idle => idle.spentFuel > 0.5);
```

---

## Integration Examples

### 1. Daily Email Report Automation

**Python Script** (`daily_idle_report.py`):
```python
#!/usr/bin/env python3
import requests
from datetime import datetime, timedelta
import logging

# Configuration
TRACCAR_URL = 'http://localhost:8082/api'
API_TOKEN = 'YOUR_TOKEN_HERE'
DEVICE_IDS = [1, 2, 3]  # Your fleet

# Setup logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

def send_daily_idle_report():
    """Send yesterday's idle report via email"""

    # Calculate date range (yesterday)
    today = datetime.now().replace(hour=0, minute=0, second=0, microsecond=0)
    yesterday = today - timedelta(days=1)

    # Format dates as ISO 8601
    from_date = yesterday.isoformat() + 'Z'
    to_date = today.isoformat() + 'Z'

    # Build request
    url = f'{TRACCAR_URL}/reports/idle/mail'
    headers = {'Authorization': f'Bearer {API_TOKEN}'}

    for device_id in DEVICE_IDS:
        params = {
            'deviceId': device_id,
            'from': from_date,
            'to': to_date
        }

        try:
            response = requests.get(url, headers=headers, params=params)
            response.raise_for_status()
            logger.info(f'Sent idle report for device {device_id}')
        except requests.exceptions.RequestException as e:
            logger.error(f'Failed to send report for device {device_id}: {e}')

if __name__ == '__main__':
    send_daily_idle_report()
```

**Crontab**:
```cron
# Send daily report at 7 AM
0 7 * * * /usr/bin/python3 /path/to/daily_idle_report.py
```

### 2. Fleet Dashboard Integration

**React Component**:
```jsx
import React, { useState, useEffect } from 'react';
import axios from 'axios';

const IdleDashboard = ({ deviceIds }) => {
    const [idles, setIdles] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchIdleData = async () => {
            const today = new Date();
            const weekAgo = new Date(today - 7 * 24 * 60 * 60 * 1000);

            try {
                const response = await axios.get('/api/reports/idle', {
                    headers: {
                        'Authorization': `Bearer ${localStorage.getItem('token')}`
                    },
                    params: {
                        deviceId: deviceIds,
                        from: weekAgo.toISOString(),
                        to: today.toISOString()
                    }
                });

                setIdles(response.data);
            } catch (error) {
                console.error('Failed to fetch idle data:', error);
            } finally {
                setLoading(false);
            }
        };

        fetchIdleData();
    }, [deviceIds]);

    const totalIdleTime = idles.reduce((sum, idle) => sum + idle.duration, 0);
    const totalFuelWaste = idles.reduce((sum, idle) => sum + idle.spentFuel, 0);

    return (
        <div className="idle-dashboard">
            <h2>Idle Statistics (Last 7 Days)</h2>
            <div className="stats">
                <div className="stat-box">
                    <h3>Total Idle Events</h3>
                    <p>{idles.length}</p>
                </div>
                <div className="stat-box">
                    <h3>Total Idle Time</h3>
                    <p>{(totalIdleTime / 1000 / 60 / 60).toFixed(2)} hours</p>
                </div>
                <div className="stat-box">
                    <h3>Fuel Wasted</h3>
                    <p>{totalFuelWaste.toFixed(2)} liters</p>
                </div>
            </div>

            <table className="idle-table">
                <thead>
                    <tr>
                        <th>Device</th>
                        <th>Location</th>
                        <th>Start Time</th>
                        <th>Duration</th>
                        <th>Fuel</th>
                    </tr>
                </thead>
                <tbody>
                    {idles.map(idle => (
                        <tr key={idle.positionId}>
                            <td>{idle.deviceName}</td>
                            <td>{idle.address}</td>
                            <td>{new Date(idle.startTime).toLocaleString()}</td>
                            <td>{(idle.duration / 1000 / 60).toFixed(0)} min</td>
                            <td>{idle.spentFuel.toFixed(2)} L</td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
};

export default IdleDashboard;
```

### 3. Real-Time Idle Alerts

**WebSocket Listener** (for real-time events):
```javascript
const WebSocket = require('ws');

const ws = new WebSocket('ws://localhost:8082/api/socket');

ws.on('open', () => {
    console.log('Connected to Traccar WebSocket');
});

ws.on('message', (data) => {
    const message = JSON.parse(data);

    if (message.events) {
        message.events.forEach(event => {
            if (event.type === 'deviceIdleStart') {
                handleIdleStart(event);
            } else if (event.type === 'deviceIdleEnd') {
                handleIdleEnd(event);
            }
        });
    }
});

function handleIdleStart(event) {
    console.log(`ALERT: Device ${event.deviceId} started idling`);
    // Send push notification, SMS, etc.
}

function handleIdleEnd(event) {
    const duration = event.attributes.duration / 1000 / 60; // minutes
    console.log(`Device ${event.deviceId} idle ended. Duration: ${duration} min`);
}
```

---

## Performance Optimization

### 1. Efficient Date Range Queries

**Good** (Fast):
```bash
# Query 1 day
from=2025-11-01T00:00:00Z&to=2025-11-01T23:59:59Z
```

**Acceptable** (Moderate):
```bash
# Query 7 days
from=2025-10-25T00:00:00Z&to=2025-11-01T23:59:59Z
```

**Slow** (Not Recommended):
```bash
# Query 30+ days - use with caution
from=2025-10-01T00:00:00Z&to=2025-11-01T23:59:59Z
```

### 2. Batch Processing

For large fleets, process devices in batches:

```python
def get_fleet_idle_report(device_ids, from_date, to_date, batch_size=10):
    """Get idle report for large fleet in batches"""
    all_idles = []

    for i in range(0, len(device_ids), batch_size):
        batch = device_ids[i:i+batch_size]

        response = requests.get(
            f'{TRACCAR_URL}/reports/idle',
            headers={'Authorization': f'Bearer {TOKEN}'},
            params={
                'deviceId': batch,
                'from': from_date,
                'to': to_date
            }
        )

        all_idles.extend(response.json())
        time.sleep(0.5)  # Rate limiting

    return all_idles
```

---

## Troubleshooting API Issues

### Empty Response

**Problem**: API returns `[]`

**Solutions**:
1. Check date range is correct
2. Verify device was active during period
3. Check threshold configuration (may be filtering all results)
4. Confirm device sends motion and ignition data

### Timeout Errors

**Problem**: Request times out

**Solutions**:
1. Reduce date range
2. Query fewer devices
3. Increase server timeout settings
4. Use device groups instead of individual devices

### Authentication Errors

**Problem**: 401 Unauthorized

**Solutions**:
1. Verify token is valid: `GET /api/session`
2. Check token hasn't expired
3. Ensure Bearer prefix: `Bearer YOUR_TOKEN`

---

## Support & Resources

- **API Base Documentation**: http://your-server:8082/api-docs
- **Traccar Forum**: https://www.traccar.org/forums/
- **GitHub Issues**: https://github.com/traccar/traccar/issues

---

## Changelog

### Version 1.0.0 (2025-11-01)
- Initial release
- JSON, Excel, and Email endpoints
- Advanced filtering thresholds
- Real-time event support
