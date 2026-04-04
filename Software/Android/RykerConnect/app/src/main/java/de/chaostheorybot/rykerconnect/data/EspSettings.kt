package de.chaostheorybot.rykerconnect.data

import java.nio.ByteBuffer
import java.nio.ByteOrder

data class EspSettings(
    val adaptiveBrightness: Boolean = false,
    val displayBrightness: Int = 128,
    val screen: Int = 0,
    val subScreen: Int = 0,
    val batteryIconSelection: BooleanArray = booleanArrayOf(true, true, false, false),
    val batteryIconFirst: Int = 0,
    val batteryIconInterval: Long = 30000,
    val notificationInterval: Long = 10000,
    val tempCalibration: Float = 0f
) {
    companion object {
        fun fromBytes(data: ByteArray): EspSettings? {
            if (data.size < 25) return null
            val buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
            return EspSettings(
                adaptiveBrightness = buf.get() != 0.toByte(),
                displayBrightness = buf.get().toInt() and 0xFF,
                screen = buf.get().toInt() and 0xFF,
                subScreen = buf.get().toInt() and 0xFF,
                batteryIconSelection = booleanArrayOf(
                    buf.get() != 0.toByte(),
                    buf.get() != 0.toByte(),
                    buf.get() != 0.toByte(),
                    buf.get() != 0.toByte()
                ),
                batteryIconFirst = buf.get().toInt(),
                batteryIconInterval = buf.getInt().toLong() and 0xFFFFFFFFL,
                notificationInterval = buf.getInt().toLong() and 0xFFFFFFFFL,
                tempCalibration = buf.getFloat()
                // CRC at bytes 21-24 is read but not stored
            )
        }
    }

    fun toWriteBytes(): ByteArray {
        val buf = ByteBuffer.allocate(21).order(ByteOrder.LITTLE_ENDIAN)
        buf.put(if (adaptiveBrightness) 1.toByte() else 0.toByte())
        buf.put(displayBrightness.toByte())
        buf.put(screen.toByte())
        buf.put(subScreen.toByte())
        for (i in 0 until 4) {
            buf.put(if (batteryIconSelection.getOrElse(i) { false }) 1.toByte() else 0.toByte())
        }
        buf.put(batteryIconFirst.toByte())
        buf.putInt(batteryIconInterval.toInt())
        buf.putInt(notificationInterval.toInt())
        buf.putFloat(tempCalibration)
        return buf.array()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EspSettings) return false
        return adaptiveBrightness == other.adaptiveBrightness &&
                displayBrightness == other.displayBrightness &&
                screen == other.screen &&
                subScreen == other.subScreen &&
                batteryIconSelection.contentEquals(other.batteryIconSelection) &&
                batteryIconFirst == other.batteryIconFirst &&
                batteryIconInterval == other.batteryIconInterval &&
                notificationInterval == other.notificationInterval &&
                tempCalibration == other.tempCalibration
    }

    override fun hashCode(): Int {
        var result = adaptiveBrightness.hashCode()
        result = 31 * result + displayBrightness
        result = 31 * result + screen
        result = 31 * result + subScreen
        result = 31 * result + batteryIconSelection.contentHashCode()
        result = 31 * result + batteryIconFirst
        result = 31 * result + batteryIconInterval.hashCode()
        result = 31 * result + notificationInterval.hashCode()
        result = 31 * result + tempCalibration.hashCode()
        return result
    }
}

