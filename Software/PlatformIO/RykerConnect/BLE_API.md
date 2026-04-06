# RykerConnect BLE API Documentation

## Overview

The RykerConnect MainUnit uses Bluetooth Low Energy (BLE) with NimBLE stack on an ESP32. The device advertises as **"RykerConnect-MainUnit"** and uses a **6-digit numeric pairing PIN** (display-only IO capability).

- **Bonding:** Enabled (bond + MITM protection)
- **TX Power:** +9 dBm
- **HID:** Registered as Gamepad (Appearance: `0x0180`, 1 button)

## Connection Parameters

After connection, the ESP requests fast connection parameters:
- Min Interval: 7.5ms (6 × 1.25ms)
- Max Interval: 15ms (12 × 1.25ms)
- Latency: 0
- Timeout: 1000ms (100 × 10ms)

---

## BLE Watchdog

If BLE is connected but **no characteristic is written for 10 minutes** (600,000 ms), the ESP force-disconnects the client. Any write resets the watchdog timer.

---

## Quick Reference — All Characteristics

| # | Name | UUID | Permissions |
|---|------|------|-------------|
| 1 | Time | `a41dcc81-d45e-4445-99bb-38c37c1ef1c8` | READ, WRITE |
| 2 | Network Status | `49c7fba8-9ba7-474b-b8a5-a5431e057e23` | WRITE |
| 3 | Phone Battery | `1f74ccf5-376a-40b6-ab60-7b1c5efbf652` | WRITE |
| 4 | Intercom Battery | `85546838-6ae5-45cb-aa2f-4c8af50d17d4` | WRITE |
| 5 | Media Data | `dcadc0d8-24ed-40ed-952b-5d1c872a69aa` | WRITE, WRITE\_NR |
| 6 | Notification | `755cf5b1-ded3-4c7b-a6fc-8c5ce2f99fdb` | WRITE + ENC + AUTHEN |
| 7 | Display Brightness | `7bc28f30-10bc-46e2-b84b-96e0545c2f5c` | READ, WRITE |
| 8 | Screen Selection | `62dbb02d-4a3a-452e-b753-02bcb2272b9d` | READ, WRITE |
| 9 | Settings | `05f7c3e4-daac-4953-8c71-20eacdf0c7a1` | READ + ENC + AUTHEN, WRITE + ENC + AUTHEN |
| 10 | Firmware Update (OTA) | `1d1306c5-98d9-4998-8dfd-35136295575f` | WRITE + ENC + AUTHEN |
| 11 | Firmware Reset | `18cb54fe-45e8-4819-a262-24b731c8b236` | WRITE + AUTHEN |
| 12 | Display Reinit | `3a6e4b2c-8f71-4d09-b5a3-c7e2f1d08a94` | WRITE |
| 13 | Firmware Version | `fb2385da-5290-4513-bb0c-6d0b21de619a` | READ |
| 14 | Hardware Version | `3ae9aece-1b67-4281-a53b-748adf23f484` | READ |
| 15 | Volume | `c4e83b7d-5a12-4f8e-b9d6-3e7f1c2a4b8d` | WRITE, WRITE\_NR |

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

**Low battery warning:** If level ≤ `low_battery_threshold_phone`, a popup is shown. The warning triggers **only once per threshold crossing** — the flag resets only when the battery rises above the threshold again. Popup duration = `warning_popup_duration` seconds.

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

**Low battery warning:** Same one-time-per-crossing logic as for phone battery. No charging status for intercom.

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
| 1–2 | uint16_t (LE) | Current position in seconds |
| 3–4 | uint16_t (LE) | Total track length in seconds |
| 5+ | String | Title + `0x03` (ETX separator) + Artist |

Total: **5+ bytes** (minimum 5 without title/artist)

All fields are **little-endian**, consistent with all other characteristics.

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
| 17 | 1 | uint8_t | low_battery_threshold_phone | Phone low-battery warning threshold % (default: 20) |
| 18 | 1 | uint8_t | low_battery_threshold_intercom | Intercom low-battery warning threshold % (default: 20) |
| 19 | 1 | uint8_t | warning_popup_duration | Warning popup display duration in seconds (default: 5) |
| **Other** |
| 20–23 | 4 | uint32_t (LE) | notification_interval | Notification display duration (ms) |
| 24–27 | 4 | float (LE) | temp_calibration | Temperature offset in °C (subtracted from sensor) |
| **Reserved** |
| 28–37 | 10 | uint8_t[10] | reserved | Reserved for future use |
| **CRC** |
| 38–41 | 4 | uint32_t (LE) | crc | CRC32 checksum |

Total: **42 bytes**

**Write** – Update all settings. Send **38 bytes** (struct without CRC). The CRC is recalculated automatically. Settings are persisted to flash (NVS/Preferences).

Write payload (38 bytes = struct size minus CRC):

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
| 19 | 1 | uint8_t | warning_popup_duration | Seconds (default: 5) |
| 20–23 | 4 | uint32_t (LE) | notification_interval | |
| 24–27 | 4 | float (LE) | temp_calibration | Temperature offset in °C (subtracted from sensor) |
| 28–37 | 10 | uint8_t[10] | reserved | Send `0` |

**Note:** `crc` is NOT included in the write payload. The ESP expects exactly `sizeof(EEPROM_Struct) - sizeof(uint32_t)` = **38 bytes**. ADC thresholds are only updated if `adc_high > adc_low` and both are non-zero. `temp_calibration` is applied directly and subtracted from every sensor reading.

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
The ESP connects to WiFi and automatically downloads & flashes the firmware from the given URL. Both `http://` and `https://` are supported (GitHub Releases URLs work). Falls back to browser OTA on failure.

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

**Read** – Returns the hardware version as a UTF-8 string (e.g. `"ESP32S3-REV01"`).

---

### 15. Volume

| Property | Value |
|----------|-------|
| UUID | `c4e83b7d-5a12-4f8e-b9d6-3e7f1c2a4b8d` |
| Permissions | WRITE |

**Write** – Send the current volume level. Displays a popup with percentage and progress bar (duration configurable via `warning_popup_duration` setting).

| Byte | Type | Description |
|------|------|-------------|
| 0 | uint8_t | Volume level (0 = Mute, 1–100 = percentage) |

Total: **1 byte**

---

## Data Encoding Notes

- **Byte order:** All multi-byte integers use **little-endian**.
- **String separator:** `0x03` (ETX) is used as field delimiter in all string-based characteristics.
- **String encoding:** UTF-8.

## Behavior Notes

**Low Battery Warning:** Triggers once when battery first falls below the threshold. The trigger flag resets only after the battery recovers above the threshold — preventing repeated popups on every BLE update.

**Volume Popup:** Shown on every Volume write. Duration = `warning_popup_duration` seconds.

**Auto-Brightness:** EMA-smoothed ADC reading (α=0.05, 100 ms interval) from light sensor on ADC pin 7. Mapped linearly from `[adc_low, adc_high]` → brightness `[25, 255]`. Minimum brightness is **25** (never goes below). Manual Brightness characteristic writes are ignored while `adaptive_brightness` is enabled.

---

## Android / Kotlin Implementation

### Constants

```kotlin
object RykerBLE {
    const val DEVICE_NAME            = "RykerConnect-MainUnit"
    const val SERVICE_UUID           = "db7ba582-229a-4b96-9000-cf0f69f86f73"
    const val TIME_UUID              = "a41dcc81-d45e-4445-99bb-38c37c1ef1c8"
    const val NETWORK_UUID           = "49c7fba8-9ba7-474b-b8a5-a5431e057e23"
    const val PHONE_BATTERY_UUID     = "1f74ccf5-376a-40b6-ab60-7b1c5efbf652"
    const val INTERCOM_BATTERY_UUID  = "85546838-6ae5-45cb-aa2f-4c8af50d17d4"
    const val MEDIA_DATA_UUID        = "dcadc0d8-24ed-40ed-952b-5d1c872a69aa"
    const val NOTIFICATION_UUID      = "755cf5b1-ded3-4c7b-a6fc-8c5ce2f99fdb"
    const val BRIGHTNESS_UUID        = "7bc28f30-10bc-46e2-b84b-96e0545c2f5c"
    const val SCREEN_UUID            = "62dbb02d-4a3a-452e-b753-02bcb2272b9d"
    const val SETTINGS_UUID          = "05f7c3e4-daac-4953-8c71-20eacdf0c7a1"
    const val FIRMWARE_UPDATE_UUID   = "1d1306c5-98d9-4998-8dfd-35136295575f"
    const val FIRMWARE_RESET_UUID    = "18cb54fe-45e8-4819-a262-24b731c8b236"
    const val DISPLAY_REINIT_UUID    = "3a6e4b2c-8f71-4d09-b5a3-c7e2f1d08a94"
    const val FIRMWARE_VERSION_UUID  = "fb2385da-5290-4513-bb0c-6d0b21de619a"
    const val HARDWARE_VERSION_UUID  = "3ae9aece-1b67-4281-a53b-748adf23f484"
    const val VOLUME_UUID            = "c4e83b7d-5a12-4f8e-b9d6-3e7f1c2a4b8d"
    const val SETTINGS_READ_SIZE     = 42  // full struct including CRC
    const val SETTINGS_WRITE_SIZE    = 38  // struct without CRC
}
```

### Write Helper

```kotlin
/** Queue BLE writes — only one can be in-flight at a time. */
fun BluetoothGatt.writeChar(uuid: String, data: ByteArray) {
    val svc  = getService(UUID.fromString(RykerBLE.SERVICE_UUID)) ?: return
    val char = svc.getCharacteristic(UUID.fromString(uuid)) ?: return
    char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
    char.value = data
    writeCharacteristic(char)
}

/** Fire-and-forget write (no BLE ACK) — use for frequent updates like Volume. */
fun BluetoothGatt.writeCharNR(uuid: String, data: ByteArray) {
    val svc  = getService(UUID.fromString(RykerBLE.SERVICE_UUID)) ?: return
    val char = svc.getCharacteristic(UUID.fromString(uuid)) ?: return
    char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
    char.value = data
    writeCharacteristic(char)
}

// Android 13+ (API 33):
fun BluetoothGatt.writeChar33(uuid: String, data: ByteArray, noResponse: Boolean = false) {
    val svc  = getService(UUID.fromString(RykerBLE.SERVICE_UUID)) ?: return
    val char = svc.getCharacteristic(UUID.fromString(uuid)) ?: return
    val type = if (noResponse) BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
               else BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
    writeCharacteristic(char, data, type)
}
```

### Sending Time

```kotlin
fun sendTime(gatt: BluetoothGatt) {
    val now = Calendar.getInstance()
    gatt.writeChar(RykerBLE.TIME_UUID, byteArrayOf(
        now.get(Calendar.HOUR_OF_DAY).toByte(),
        now.get(Calendar.MINUTE).toByte(),
        now.get(Calendar.SECOND).toByte()
    ))
}
```

### Sending Network Status

```kotlin
fun sendNetwork(gatt: BluetoothGatt, signalBars: Int, typeCode: Int) {
    // signalBars: 0–5 (icon level), typeCode: 0–9 (see network type table)
    gatt.writeChar(RykerBLE.NETWORK_UUID, byteArrayOf(
        signalBars.toByte(),
        typeCode.toByte()
    ))
}
```

### Sending Phone Battery

```kotlin
fun sendPhoneBattery(gatt: BluetoothGatt, level: Int, isCharging: Boolean) {
    val data = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN)
        .put(level.toByte())
        .put(if (isCharging) 1.toByte() else 0.toByte())
        .array()
    gatt.writeChar(RykerBLE.PHONE_BATTERY_UUID, data)
}
```

### Sending Intercom Battery

```kotlin
fun sendIntercomBattery(gatt: BluetoothGatt, level: Int) {
    gatt.writeChar(RykerBLE.INTERCOM_BATTERY_UUID, byteArrayOf(level.toByte()))
}
```

### Sending Media Data

```kotlin
fun sendMediaData(
    gatt: BluetoothGatt,
    playing: Boolean,
    positionSec: Int,
    lengthSec: Int,
    title: String,
    artist: String
) {
    val titleBytes  = title.toByteArray(Charsets.UTF_8)
    val artistBytes = artist.toByteArray(Charsets.UTF_8)
    val buf = ByteBuffer.allocate(5 + titleBytes.size + 1 + artistBytes.size)
        .order(ByteOrder.LITTLE_ENDIAN)
    buf.put(if (playing) 1.toByte() else 0.toByte())  // [0] playstate
    buf.putShort(positionSec.toShort())                 // [1-2] little-endian
    buf.putShort(lengthSec.toShort())                   // [3-4] little-endian
    buf.put(titleBytes)
    buf.put(0x03.toByte())                              // ETX separator
    buf.put(artistBytes)
    gatt.writeChar(RykerBLE.MEDIA_DATA_UUID, buf.array())
}
```

### Sending a Notification

```kotlin
fun sendNotification(gatt: BluetoothGatt, type: String, title: String, text: String) {
    // type examples: "WhatsApp", "Instagram", "Discord", "Gmail", "Messages"
    gatt.writeChar(
        RykerBLE.NOTIFICATION_UUID,
        "$type\u0003$title\u0003$text".toByteArray(Charsets.UTF_8)
    )
}
```

### Setting Brightness

```kotlin
fun setBrightness(gatt: BluetoothGatt, brightness: Int) {
    // 0–255. No effect while adaptive_brightness is enabled.
    gatt.writeChar(RykerBLE.BRIGHTNESS_UUID, byteArrayOf(brightness.coerceIn(0, 255).toByte()))
}
```

### Setting Screen

```kotlin
fun setScreen(gatt: BluetoothGatt, screenIndex: Int) {
    // 0 = Default (clock+temp), 1 = Media, 2 = Split
    gatt.writeChar(RykerBLE.SCREEN_UUID, byteArrayOf(screenIndex.toByte()))
}
```

### Sending Volume

```kotlin
fun sendVolume(gatt: BluetoothGatt, volumePercent: Int) {
    // Use WRITE_NO_RESPONSE for fast, frequent volume updates (no BLE round-trip ACK).
    // 0 = Mute, 1–100 = percentage. Shows popup for warning_popup_duration seconds.
    gatt.writeCharNR(RykerBLE.VOLUME_UUID, byteArrayOf(volumePercent.coerceIn(0, 100).toByte()))
}
```

### Reading Settings (42 bytes)

```kotlin
fun parseSettings(data: ByteArray): RykerSettings {
    require(data.size >= 42) { "Settings response must be 42 bytes" }
    val buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
    return RykerSettings(
        adaptiveBrightness    = buf.get().toInt() != 0,           // [0]
        displayBrightness     = buf.get().toInt() and 0xFF,        // [1]
        adcLow                = buf.short.toInt() and 0xFFFF,      // [2-3]
        adcHigh               = buf.short.toInt() and 0xFFFF,      // [4-5]
        screen                = buf.get().toInt() and 0xFF,         // [6]
        subScreen             = buf.get().toInt() and 0xFF,         // [7]
        batteryIcon0          = buf.get().toInt() != 0,            // [8]
        batteryIcon1          = buf.get().toInt() != 0,            // [9]
        // [10],[11] reserved bools — read and discard
        also { buf.get(); buf.get() },
        batteryIconFirst      = buf.get().toInt(),                  // [12] signed
        batteryIconInterval   = buf.int,                            // [13-16] ms
        lowBatteryPhone       = buf.get().toInt() and 0xFF,         // [17] %
        lowBatteryIntercom    = buf.get().toInt() and 0xFF,         // [18] %
        warningPopupDuration  = buf.get().toInt() and 0xFF,         // [19] seconds
        notificationInterval  = buf.int,                            // [20-23] ms
        tempCalibration       = buf.float,                          // [24-27]
        // [28-37] reserved — skip, [38-41] CRC — skip
    )
}
```

### Writing Settings (38 bytes)

```kotlin
fun buildSettingsPayload(s: RykerSettings): ByteArray {
    val data = ByteArray(RykerBLE.SETTINGS_WRITE_SIZE)  // 38 bytes, no CRC
    val buf  = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
    buf.put(if (s.adaptiveBrightness) 1 else 0)        // [0]
    buf.put(s.displayBrightness.toByte())               // [1]
    buf.putShort(s.adcLow.toShort())                    // [2-3]  0 = keep current
    buf.putShort(s.adcHigh.toShort())                   // [4-5]  0 = keep current
    buf.put(s.screen.toByte())                          // [6]
    buf.put(s.subScreen.toByte())                       // [7]
    buf.put(if (s.batteryIcon0) 1 else 0)              // [8]
    buf.put(if (s.batteryIcon1) 1 else 0)              // [9]
    buf.put(0)                                          // [10] reserved
    buf.put(0)                                          // [11] reserved
    buf.put(s.batteryIconFirst.toByte())                // [12]
    buf.putInt(s.batteryIconInterval)                   // [13-16] ms
    buf.put(s.lowBatteryPhone.toByte())                 // [17] %
    buf.put(s.lowBatteryIntercom.toByte())              // [18] %
    buf.put(s.warningPopupDuration.toByte())            // [19] seconds
    buf.putInt(s.notificationInterval)                  // [20-23] ms
    buf.putFloat(0f)                                    // [24-27] temp_calibration: ESP ignores writes
    repeat(10) { buf.put(0) }                           // [28-37] reserved
    return data
}

fun writeSettings(gatt: BluetoothGatt, s: RykerSettings) {
    gatt.writeChar(RykerBLE.SETTINGS_UUID, buildSettingsPayload(s))
}
```

### Reading Firmware / Hardware Version

```kotlin
// Trigger a read — handle result in onCharacteristicRead()
fun readVersions(gatt: BluetoothGatt) {
    val svc = gatt.getService(UUID.fromString(RykerBLE.SERVICE_UUID)) ?: return
    gatt.readCharacteristic(svc.getCharacteristic(UUID.fromString(RykerBLE.FIRMWARE_VERSION_UUID)))
    // Wait for onCharacteristicRead before issuing next read!
}

// In BluetoothGattCallback.onCharacteristicRead():
override fun onCharacteristicRead(gatt: BluetoothGatt, char: BluetoothGattCharacteristic, status: Int) {
    if (status != BluetoothGatt.GATT_SUCCESS) return
    val data = char.value
    when (char.uuid.toString().lowercase()) {
        RykerBLE.FIRMWARE_VERSION_UUID.lowercase() -> {
            val buf   = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
            val ver   = buf.short.toInt() and 0xFFFF  // e.g. 0x0006
            val major = (ver shr 8) and 0xFF           // 0
            val minor = ver and 0xFF                   // 6
            val text  = "v$major.$minor"               // "v0.6"
        }
        RykerBLE.HARDWARE_VERSION_UUID.lowercase() -> {
            val hw = String(data, Charsets.UTF_8)      // "ESP32S3-REV01"
        }
        RykerBLE.SETTINGS_UUID.lowercase() -> {
            val settings = parseSettings(data)         // 42 bytes
        }
    }
}
```

### OTA Update

```kotlin
// Variant A — Browser upload (no URL):
fun startOtaBrowser(gatt: BluetoothGatt, ssid: String, password: String) {
    gatt.writeChar(RykerBLE.FIRMWARE_UPDATE_UUID, "$ssid\u0003$password".toByteArray())
    // Device connects to WiFi and shows IP on screen.
    // Open http://<ip> (or http://RykerConnect.local) in browser, upload .bin file.
}

// Variant B — Direct download (HTTP and HTTPS supported, e.g. GitHub Releases):
fun startOtaDownload(gatt: BluetoothGatt, ssid: String, password: String, url: String) {
    gatt.writeChar(
        RykerBLE.FIRMWARE_UPDATE_UUID,
        "$ssid\u0003$password\u0003$url".toByteArray()
    )
}
```

### Factory Reset

```kotlin
fun sendResetPin(gatt: BluetoothGatt, pin: Int) {
    // pin = 0 → cancel and hide popup
    // pin = 4-digit value shown on display → reset all settings and reboot
    // any other value → new PIN generated and shown on display
    val data = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN)
        .putShort(pin.toShort()).array()
    gatt.writeChar(RykerBLE.FIRMWARE_RESET_UUID, data)
}
```

### Reinitialize Displays

```kotlin
fun reinitDisplays(gatt: BluetoothGatt) {
    gatt.writeChar(RykerBLE.DISPLAY_REINIT_UUID, byteArrayOf(0x01))
}
```
