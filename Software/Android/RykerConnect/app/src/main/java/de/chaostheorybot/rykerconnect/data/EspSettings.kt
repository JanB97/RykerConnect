package de.chaostheorybot.rykerconnect.data

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Mirrors the ESP's EEPROM_Struct (42 bytes read, 38 bytes written without CRC).
 *
 * Struct layout (all multi-byte LE):
 *  0      bool     adaptive_brightness
 *  1      uint8    display_brightness
 *  2-3    uint16   auto_brightness_adc_low
 *  4-5    uint16   auto_brightness_adc_high
 *  6      uint8    screen
 *  7      uint8    sub_screen
 *  8-11   bool[4]  battery_icon_selection
 *  12     int8     battery_icon_first
 *  13-16  uint32   battery_icon_interval  (ms)
 *  17     uint8    low_battery_threshold_phone  (%)
 *  18     uint8    low_battery_threshold_intercom (%)
 *  19     uint8    warning_popup_duration  (seconds)
 *  20-23  uint32   notification_interval   (ms)
 *  24-27  float    temp_calibration
 *  28-37  reserved (10 bytes)
 *  38-41  uint32   crc  (only in read response)
 */
data class EspSettings(
    val adaptiveBrightness: Boolean = false,
    val displayBrightness: Int = 128,
    val autoBrightnessAdcLow: Int = 200,
    val autoBrightnessAdcHigh: Int = 3500,
    val screen: Int = 0,
    val subScreen: Int = 0,
    val batteryIconSelection: BooleanArray = booleanArrayOf(true, true, false, false),
    val batteryIconFirst: Int = 0,
    val batteryIconInterval: Long = 30_000,
    val lowBatteryThresholdPhone: Int = 20,
    val lowBatteryThresholdIntercom: Int = 20,
    val warningPopupDuration: Int = 5,
    val notificationInterval: Long = 10_000,
    val tempCalibration: Float = 0f
) {
    companion object {
        /** Parse the full 42-byte read response from the ESP Settings characteristic. */
        fun fromBytes(data: ByteArray): EspSettings? {
            if (data.size < 42) return null
            val buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
            return EspSettings(
                adaptiveBrightness       = buf.get() != 0.toByte(),              // 0
                displayBrightness        = buf.get().toInt() and 0xFF,            // 1
                autoBrightnessAdcLow     = buf.getShort().toInt() and 0xFFFF,     // 2-3
                autoBrightnessAdcHigh    = buf.getShort().toInt() and 0xFFFF,     // 4-5
                screen                   = buf.get().toInt() and 0xFF,            // 6
                subScreen                = buf.get().toInt() and 0xFF,            // 7
                batteryIconSelection     = booleanArrayOf(                        // 8-11
                    buf.get() != 0.toByte(),
                    buf.get() != 0.toByte(),
                    buf.get() != 0.toByte(),
                    buf.get() != 0.toByte()
                ),
                batteryIconFirst         = buf.get().toInt(),                     // 12
                batteryIconInterval      = buf.getInt().toLong() and 0xFFFFFFFFL, // 13-16
                lowBatteryThresholdPhone = buf.get().toInt() and 0xFF,            // 17
                lowBatteryThresholdIntercom = buf.get().toInt() and 0xFF,         // 18
                warningPopupDuration     = buf.get().toInt() and 0xFF,            // 19
                notificationInterval     = buf.getInt().toLong() and 0xFFFFFFFFL, // 20-23
                tempCalibration          = buf.getFloat()                         // 24-27
                // 28-37 reserved, 38-41 CRC – ignored
            )
        }
    }

    /**
     * Serialize to the 38-byte write payload (struct without CRC).
     * The ESP recalculates the CRC itself.
     */
    fun toWriteBytes(): ByteArray {
        val buf = ByteBuffer.allocate(38).order(ByteOrder.LITTLE_ENDIAN)
        buf.put(if (adaptiveBrightness) 1.toByte() else 0.toByte())   // 0
        buf.put(displayBrightness.toByte())                            // 1
        buf.putShort(autoBrightnessAdcLow.toShort())                   // 2-3
        buf.putShort(autoBrightnessAdcHigh.toShort())                  // 4-5
        buf.put(screen.toByte())                                       // 6
        buf.put(subScreen.toByte())                                    // 7
        for (i in 0 until 4) {                                         // 8-11
            buf.put(if (batteryIconSelection.getOrElse(i) { false }) 1.toByte() else 0.toByte())
        }
        buf.put(batteryIconFirst.toByte())                             // 12
        buf.putInt(batteryIconInterval.toInt())                        // 13-16
        buf.put(lowBatteryThresholdPhone.toByte())                     // 17
        buf.put(lowBatteryThresholdIntercom.toByte())                  // 18
        buf.put(warningPopupDuration.toByte())                         // 19
        buf.putInt(notificationInterval.toInt())                       // 20-23
        buf.putFloat(tempCalibration)                                  // 24-27
        repeat(10) { buf.put(0.toByte()) }                             // 28-37 reserved
        return buf.array()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EspSettings) return false
        return adaptiveBrightness == other.adaptiveBrightness &&
                displayBrightness == other.displayBrightness &&
                autoBrightnessAdcLow == other.autoBrightnessAdcLow &&
                autoBrightnessAdcHigh == other.autoBrightnessAdcHigh &&
                screen == other.screen &&
                subScreen == other.subScreen &&
                batteryIconSelection.contentEquals(other.batteryIconSelection) &&
                batteryIconFirst == other.batteryIconFirst &&
                batteryIconInterval == other.batteryIconInterval &&
                lowBatteryThresholdPhone == other.lowBatteryThresholdPhone &&
                lowBatteryThresholdIntercom == other.lowBatteryThresholdIntercom &&
                warningPopupDuration == other.warningPopupDuration &&
                notificationInterval == other.notificationInterval &&
                tempCalibration == other.tempCalibration
    }

    override fun hashCode(): Int {
        var result = adaptiveBrightness.hashCode()
        result = 31 * result + displayBrightness
        result = 31 * result + autoBrightnessAdcLow
        result = 31 * result + autoBrightnessAdcHigh
        result = 31 * result + screen
        result = 31 * result + subScreen
        result = 31 * result + batteryIconSelection.contentHashCode()
        result = 31 * result + batteryIconFirst
        result = 31 * result + batteryIconInterval.hashCode()
        result = 31 * result + lowBatteryThresholdPhone
        result = 31 * result + lowBatteryThresholdIntercom
        result = 31 * result + warningPopupDuration
        result = 31 * result + notificationInterval.hashCode()
        result = 31 * result + tempCalibration.hashCode()
        return result
    }
}
