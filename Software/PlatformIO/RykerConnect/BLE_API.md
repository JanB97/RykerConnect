# RykerConnect BLE API Documentation

## Overview

The RykerConnect MainUnit uses Bluetooth Low Energy (BLE) with NimBLE stack on an ESP32. The device advertises as **"RykerConnect-MainUnit"** and uses a **6-digit numeric pairing PIN** (display-only IO capability).

- **Bonding:** Enabled (bond + MITM protection)
- **TX Power:** +9 dBm
- **HID:** Registered as HID Keyboard device (Appearance: 0x0180)

## Connection Parameters

After connection, the ESP requests fast connection parameters:
- Min Interval: 7.5ms (6 × 1.25ms)
- Max Interval: 15ms (12 × 1.25ms)
- Latency: 0
- Timeout: 1000ms (100 × 10ms)

---

## Service

| Name | UUID |
|------|------|
| RykerConnect Service | `db7ba582-229a-4b96-9000-cf0f69f86f73` |

---

## Characteristics

### 1. Time

| Property | Value |
|----------|-------|
| UUID | `a41dcc81-d45e-4445-99bb-38c37c1ef1c8` |
| Permissions | READ, WRITE |

**Write** – Set the RTC clock time.

| Byte | Type | Description |
|------|------|-------------|
| 0 | uint8_t | Hour (0–23) |
| 1 | uint8_t | Minute (0–59) |
| 2 | uint8_t | Second (0–59) |

Total: **3 bytes**

**Read** – Returns a placeholder string (not the current time).

---

### 2. Network Status

| Property | Value |
|----------|-------|
| UUID | `49c7fba8-9ba7-474b-b8a5-a5431e057e23` |
| Permissions | WRITE |

**Write** – Send mobile network signal and type.

| Byte | Type | Description |
|------|------|-------------|
| 0 | uint8_t | Signal strength (raw value) |
| 1 | uint8_t | Network type code |

Total: **2 bytes** (uint16_t, little-endian)

Network type codes:
| Code | Display |
|------|---------|
| 0 | `!` (no signal) |
| 1 | `G` (GPRS) |
| 2 | `E` (EDGE) |
| 3 | `3G` |
| 4 | `H` (HSPA) |
| 5 | `H+` (HSPA+) |
| 6 | `4G` (LTE) |
| 7 | `4G+` (LTE-A) |
| 8 | `5G` |
| 9 | `5G+` |

---

### 3. Phone Battery

| Property | Value |
|----------|-------|
| UUID | `1f74ccf5-376a-40b6-ab60-7b1c5efbf652` |
| Permissions | WRITE |

**Write** – Send phone battery level and charging status.

| Byte | Type | Description |
|------|------|-------------|
| 0 | int8_t | Battery level (0–100, or negative) |
| 1 | uint8_t | Charging status (0 = not charging, 1 = charging) |

Total: **2 bytes** (int16_t, little-endian)

---

### 4. Intercom Battery

| Property | Value |
|----------|-------|
| UUID | `85546838-6ae5-45cb-aa2f-4c8af50d17d4` |
| Permissions | WRITE |

**Write** – Send intercom battery level.

| Byte | Type | Description |
|------|------|-------------|
| 0 | uint8_t | Battery level (0–100) |

Total: **1 byte** (int8_t)

---

### 5. Media Data

| Property | Value |
|----------|-------|
| UUID | `dcadc0d8-24ed-40ed-952b-5d1c872a69aa` |
| Permissions | WRITE, WRITE_NR (write without response) |

**Write** – Send current media playback state.

| Byte | Type | Description |
|------|------|-------------|
| 0 | uint8_t | Playstate: 0 = paused, 1 = playing |
| 1–2 | uint16_t (BE) | Current position in seconds (big-endian) |
| 3–4 | uint16_t (BE) | Total track length in seconds (big-endian) |
| 5+ | String | Title + `0x03` (ETX separator) + Artist |

Total: **5+ bytes** (minimum 5 without title/artist)

**Important:** The position and length fields are **big-endian** (high byte first), unlike most BLE conventions.

String format for bytes 5+:
```
<Title>\x03<Artist>
```

---

### 6. Notification

| Property | Value |
|----------|-------|
| UUID | `755cf5b1-ded3-4c7b-a6fc-8c5ce2f99fdb` |
| Permissions | WRITE (encrypted + authenticated) |

**Write** – Display a notification popup on the device.

String format:
```
<Type>\x03<Title>\x03<Text>
```

Fields are separated by `0x03` (ETX byte). All fields are UTF-8 strings. The notification is displayed for a configurable duration (`notification_interval` in settings).

---

### 7. Display Brightness

| Property | Value |
|----------|-------|
| UUID | `7bc28f30-10bc-46e2-b84b-96e0545c2f5c` |
| Permissions | READ, WRITE |

**Write** – Set display brightness (contrast) for both OLEDs.

| Byte | Type | Description |
|------|------|-------------|
| 0 | uint8_t | Brightness (0–255) |

**Read** – Returns the current brightness value (uint8_t).

---

### 8. Screen Selection

| Property | Value |
|----------|-------|
| UUID | `62dbb02d-4a3a-452e-b753-02bcb2272b9d` |
| Permissions | READ, WRITE |

**Write** – Switch the active screen layout.

| Byte | Type | Description |
|------|------|-------------|
| 0 | uint8_t | Screen index |

Screen indices:
| Value | Screen |
|-------|--------|
| 0 | Default (fullscreen clock left, temperature right) |
| 1 | Media screen |
| 2 | Split screen |

**Read** – Returns the current screen index (uint8_t).

---

### 9. Settings

| Property | Value |
|----------|-------|
| UUID | `05f7c3e4-daac-4953-8c71-20eacdf0c7a1` |
| Permissions | READ (encrypted + authenticated), WRITE (encrypted + authenticated) |

**Read** – Returns the full EEPROM_Struct (all settings as raw bytes). The struct is packed, so it can be directly parsed byte by byte.

**EEPROM_Struct layout (read response):**

| Offset | Size | Type | Field | Description |
|--------|------|------|-------|-------------|
| **Brightness** |
| 0 | 1 | bool | adaptive_brightness | Adaptive brightness enabled |
| 1 | 1 | uint8_t | display_brightness | Display brightness (0–255) |
| 2–3 | 2 | uint16_t (LE) | auto_brightness_adc_low | ADC value (0–4095) below which min brightness is applied (default: 200) |
| 4–5 | 2 | uint16_t (LE) | auto_brightness_adc_high | ADC value (0–4095) above which max brightness is applied (default: 3500) |
| **Screen** |
| 6 | 1 | uint8_t | screen | Active screen index (0=Default, 1=Media, 2=Split) |
| 7 | 1 | uint8_t | sub_screen | Sub-screen index |
| **Battery Icon** |
| 8 | 1 | bool | battery_icon_selection[0] | Show phone battery icon |
| 9 | 1 | bool | battery_icon_selection[1] | Show intercom battery icon |
| 10 | 1 | bool | battery_icon_selection[2] | (reserved) |
| 11 | 1 | bool | battery_icon_selection[3] | (reserved) |
| 12 | 1 | int8_t | battery_icon_first | First battery icon to show |
| 13–16 | 4 | uint32_t (LE) | battery_icon_interval | Battery icon rotation interval (ms) |
| **Low Battery Warning** |
| 17 | 1 | uint8_t | low_battery_threshold_phone | Phone low-battery warning threshold % (default: 15) |
| 18 | 1 | uint8_t | low_battery_threshold_intercom | Intercom low-battery warning threshold % (default: 15) |
| **Other** |
| 19–22 | 4 | uint32_t (LE) | notification_interval | Notification display duration (ms) |
| 23–26 | 4 | float (LE) | temp_calibration | Temperature offset in °C (subtracted from sensor) |
| **Reserved** |
| 27–34 | 8 | uint8_t[8] | reserved | Reserved for future use |
| **CRC** |
| 35–38 | 4 | uint32_t (LE) | crc | CRC32 checksum |

Total: **39 bytes**

**Write** – Update all settings. Send **35 bytes** (struct without CRC). The CRC is recalculated automatically. Settings are persisted to flash (NVS/Preferences).

Write payload (35 bytes = struct size minus CRC):

| Offset | Size | Type | Field | Notes |
|--------|------|------|-------|-------|
| 0 | 1 | bool | adaptive_brightness | |
| 1 | 1 | uint8_t | display_brightness | |
| 2–3 | 2 | uint16_t (LE) | auto_brightness_adc_low | Send `0` to keep current value |
| 4–5 | 2 | uint16_t (LE) | auto_brightness_adc_high | Send `0` to keep current value |
| 6 | 1 | uint8_t | screen | |
| 7 | 1 | uint8_t | sub_screen | |
| 8–11 | 4 | bool[4] | battery_icon_selection | |
| 12 | 1 | int8_t | battery_icon_first | |
| 13–16 | 4 | uint32_t (LE) | battery_icon_interval | |
| 17 | 1 | uint8_t | low_battery_threshold_phone | |
| 18 | 1 | uint8_t | low_battery_threshold_intercom | |
| 19–22 | 4 | uint32_t (LE) | notification_interval | |
| 23–26 | 4 | float (LE) | temp_calibration | Sent but **not overwritten** by ESP |
| 27–34 | 8 | uint8_t[8] | reserved | Send `0` |

**Note:** `crc` is NOT included in the write payload. The ESP expects exactly `sizeof(EEPROM_Struct) - sizeof(uint32_t)` = **35 bytes**. ADC thresholds are only updated if `adc_high > adc_low` and both are non-zero.

---

### 10. Firmware Update (OTA)

| Property | Value |
|----------|-------|
| UUID | `1d1306c5-98d9-4998-8dfd-35136295575f` |
| Permissions | WRITE (encrypted + authenticated) |

**Write** – Start WiFi and OTA update process.

String format (two variants):

**Browser-based OTA (no download URL):**
```
<SSID>\x03<Password>
```
The ESP connects to WiFi and starts a web server at `http://RykerConnect.local` for manual firmware upload via browser.

**Direct download OTA:**
```
<SSID>\x03<Password>\x03<Firmware_URL>
```
The ESP connects to WiFi and automatically downloads & flashes the firmware from the given HTTPS URL. Falls back to browser OTA on failure.

Fields are separated by `0x03` (ETX byte).

---

### 11. Firmware Reset

| Property | Value |
|----------|-------|
| UUID | `18cb54fe-45e8-4819-a262-24b731c8b236` |
| Permissions | WRITE (authenticated) |

**Write** – Factory reset the device (if correct PIN is provided), or cancel a pending reset.

| Byte | Type | Description |
|------|------|-------------|
| 0–1 | uint16_t (LE) | Reset PIN (or `0` to cancel) |

- **Value `0`:** Cancels a pending reset and hides the reset popup on the display.
- **Correct PIN:** Settings are reset and ESP restarts.
- **Wrong PIN:** A new PIN is generated and displayed.

A random 4-digit PIN (1000–9999) is generated at boot and shown on the display when a wrong PIN is sent.

---

### 12. Display Reinit

| Property | Value |
|----------|-------|
| UUID | `3a6e4b2c-8f71-4d09-b5a3-c7e2f1d08a94` |
| Permissions | WRITE |

**Write** – Reinitialize both OLED displays. Useful when a display fails to initialize at boot.

Send any value (content is ignored). The ESP will:
1. Reset CS pins
2. Initialize displays with slow SPI clock (8 MHz)
3. Re-initialize with fast SPI clock (60 MHz)
4. Restore brightness from saved settings

---

### 13. Firmware Version

| Property | Value |
|----------|-------|
| UUID | `fb2385da-5290-4513-bb0c-6d0b21de619a` |
| Permissions | READ |

**Read** – Returns the firmware version as a raw 16-bit integer.

| Byte | Type | Description |
|------|------|-------------|
| 0–1 | uint16_t (LE) | Firmware version (e.g. `0x0004` = V00.04) |

The version value corresponds to the `VERSION` compile-time constant. To display it in `Vxx.yy` format:
- High byte = major (`(version >> 8) & 0xFF`)
- Low byte = minor (`version & 0xFF`)

---

### 14. Hardware Version

| Property | Value |
|----------|-------|
| UUID | `3ae9aece-1b67-4281-a53b-748adf23f484` |
| Permissions | READ |

**Read** – Returns the hardware version as a string (e.g. `"REV01"`).

---

### 15. Volume

| Property | Value |
|----------|-------|
| UUID | `c4e83b7d-5a12-4f8e-b9d6-3e7f1c2a4b8d` |
| Permissions | WRITE |

**Write** – Send the current volume level. Displays a popup with percentage and progress bar for 2 seconds.

| Byte | Type | Description |
|------|------|-------------|
| 0 | uint8_t | Volume level (0 = Mute, 1–100 = percentage) |

Total: **1 byte**

---

## Data Encoding Notes

- **Byte order:** All multi-byte integers use **little-endian** unless noted otherwise.
- **Exception:** Media Data position and length (bytes 1–4) use **big-endian**.
- **String separator:** `0x03` (ETX) is used as field delimiter in all string-based characteristics.
- **String encoding:** UTF-8.

## Android BLE Implementation Notes

### Connecting
1. Scan for device name `"RykerConnect-MainUnit"` or filter by Service UUID.
2. Bond using the 6-digit PIN displayed on the device.
3. After bonding, subsequent connections auto-authenticate.

### Reading Settings
```kotlin
// After discovering services:
val settingsChar = service.getCharacteristic(UUID.fromString("05f7c3e4-daac-4953-8c71-20eacdf0c7a1"))
gatt.readCharacteristic(settingsChar)
// In onCharacteristicRead callback, parse the 39-byte response per struct layout above
```

### Writing Settings
```kotlin
val data = ByteArray(35) // sizeof(EEPROM_Struct) - sizeof(CRC)
val buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)

// Brightness (bytes 0-5)
buf.put(if (adaptiveBrightness) 1.toByte() else 0.toByte())  // [0]
buf.put(brightness.toByte())                                   // [1]
buf.putShort(adcBrightnessLow.toShort())                       // [2-3] 0 = keep current
buf.putShort(adcBrightnessHigh.toShort())                      // [4-5] 0 = keep current

// Screen (bytes 6-7)
buf.put(screen.toByte())                                       // [6]
buf.put(subScreen.toByte())                                    // [7]

// Battery icon (bytes 8-16)
buf.put(if (batteryIcon1) 1.toByte() else 0.toByte())          // [8]
buf.put(if (batteryIcon2) 1.toByte() else 0.toByte())          // [9]
buf.put(0)                                                     // [10] reserved
buf.put(0)                                                     // [11] reserved
buf.put(batteryIconFirst.toByte())                             // [12]
buf.putInt(batteryIconInterval)                                // [13-16]

// Low battery warning (bytes 17-18)
buf.put(lowBatteryThresholdPhone.toByte())                     // [17]
buf.put(lowBatteryThresholdIntercom.toByte())                  // [18]

// Other (bytes 19-26)
buf.putInt(notificationInterval)                               // [19-22]
buf.putFloat(0f)                                               // [23-26] temp_calibration: send 0, ESP ignores

// Reserved (bytes 27-34)
repeat(8) { buf.put(0) }

settingsChar.value = data
gatt.writeCharacteristic(settingsChar)
```

### Sending Media Data
```kotlin
val buffer = ByteBuffer.allocate(5 + titleBytes.size + 1 + artistBytes.size)
buffer.put(if (playing) 1.toByte() else 0.toByte())
buffer.putShort(positionSeconds.toShort())  // big-endian (ByteBuffer default)
buffer.putShort(lengthSeconds.toShort())    // big-endian
buffer.put(titleBytes)
buffer.put(0x03.toByte())  // separator
buffer.put(artistBytes)
mediaChar.value = buffer.array()
gatt.writeCharacteristic(mediaChar)
```

### Sending Notifications
```kotlin
val payload = "$type\u0003$title\u0003$text"
notificationChar.value = payload.toByteArray(Charsets.UTF_8)
gatt.writeCharacteristic(notificationChar)
```

### Sending Volume
```kotlin
val volumeChar = service.getCharacteristic(UUID.fromString("c4e83b7d-5a12-4f8e-b9d6-3e7f1c2a4b8d"))
volumeChar.value = byteArrayOf(volumePercent.toByte()) // 0 = Mute, 1-100 = %
gatt.writeCharacteristic(volumeChar)
```
