@file:Suppress("unused")

package de.chaostheorybot.rykerconnect.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class RykerConnectStore(private val context: Context) {
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("RykerConnectStore")
        private val FIRST_LAUNCH_TOKEN = booleanPreferencesKey("first_launch")
        private val SEL_MAC_TOKEN = stringPreferencesKey("sel_mac")
        private val INTERCOM_CONNECTED_TOKEN = booleanPreferencesKey("intercom_connected")
        private val MEDIA_TITLE_TOKEN = stringPreferencesKey("media_title")
        private val MEDIA_ARTIST_TOKEN = stringPreferencesKey("media_artist")
        private val MEDIA_PLAYSTATE_TOKEN = booleanPreferencesKey("media_playstate")
        private val MEDIA_TRACK_LENGTH_TOKEN = intPreferencesKey("media_track_length")
        private val MEDIA_PLAYBACK_POSITION_TOKEN = intPreferencesKey("media_playback_position")
        private val NOTIFICATION_TITLE_TOKEN = stringPreferencesKey("notification_title")
        private val NOTIFICATION_TEXT_TOKEN = stringPreferencesKey("notification_text")
        private val NOTIFICATION_CATEGORY_TOKEN = stringPreferencesKey("notification_category")
        private val NOTIFICATION_APP_TOKEN = stringPreferencesKey("notification_app")
        private val NOTIFICATION_APP_NAME_TOKEN = stringPreferencesKey("notification_app_name")
        private val BLE_MAC_TOKEN = stringPreferencesKey("ble_mac")
        private val BLE_APPEAR_TOKEN = booleanPreferencesKey("ble_appear")
        private val MUSIC_PLAYER_TOKEN = intPreferencesKey("music_player")
        
        private val INTERCOM_BATTERY_TOKEN = intPreferencesKey("intercom_battery")

        // Firmware Update Settings
        private val FW_AUTO_DOWNLOAD = booleanPreferencesKey("fw_auto_download")
        private val FW_VERSION = stringPreferencesKey("fw_version")
        private val FW_USE_HOTSPOT = booleanPreferencesKey("fw_use_hotspot")
        private val FW_WLAN_SSID = stringPreferencesKey("fw_wlan_ssid")
        private val FW_WLAN_PWD = stringPreferencesKey("fw_wlan_pwd")
        private val FW_HOTSPOT_SSID = stringPreferencesKey("fw_hotspot_ssid")
        private val FW_HOTSPOT_PWD = stringPreferencesKey("fw_hotspot_pwd")

        // Service Toggles
        private val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        private val MUSIC_ENABLED = booleanPreferencesKey("music_enabled")
        private val VOLUME_ENABLED = booleanPreferencesKey("volume_enabled")
        private val INTERCOM_BATTERY_ENABLED = booleanPreferencesKey("intercom_battery_enabled")
    }

    // Getters
    val getFirstLaunchToken: Flow<Boolean> = context.dataStore.data.map { it[FIRST_LAUNCH_TOKEN] ?: true }
    val getSelectedMacToken: Flow<String> = context.dataStore.data.map { it[SEL_MAC_TOKEN] ?: "" }
    val getInterComConnectedToken: Flow<Boolean> = context.dataStore.data.map { it[INTERCOM_CONNECTED_TOKEN] ?: false }
    val getMediaTitleToken: Flow<String> = context.dataStore.data.map { it[MEDIA_TITLE_TOKEN] ?: "" }
    val getMediaArtistToken: Flow<String> = context.dataStore.data.map { it[MEDIA_ARTIST_TOKEN] ?: "" }
    val getMediaPlayStateToken: Flow<Boolean> = context.dataStore.data.map { it[MEDIA_PLAYSTATE_TOKEN] ?: false }
    val getMediaTrackLengthToken: Flow<Int> = context.dataStore.data.map { it[MEDIA_TRACK_LENGTH_TOKEN] ?: 0 }
    val getMediaPlayBackPositionToken: Flow<Int> = context.dataStore.data.map { it[MEDIA_PLAYBACK_POSITION_TOKEN] ?: 0 }
    val getNotificationTitleToken: Flow<String> = context.dataStore.data.map { it[NOTIFICATION_TITLE_TOKEN] ?: "" }
    val getNotificationTextToken: Flow<String> = context.dataStore.data.map { it[NOTIFICATION_TEXT_TOKEN] ?: "" }
    val getNotificationCategoryToken: Flow<String> = context.dataStore.data.map { it[NOTIFICATION_CATEGORY_TOKEN] ?: "" }
    val getNotificationAppToken: Flow<String> = context.dataStore.data.map { it[NOTIFICATION_APP_TOKEN] ?: "" }
    val getNotificationAppNameToken: Flow<String> = context.dataStore.data.map { it[NOTIFICATION_APP_NAME_TOKEN] ?: "" }
    val getBLEMACToken: Flow<String> = context.dataStore.data.map { it[BLE_MAC_TOKEN] ?: "" }
    val getBLEAppearToken: Flow<Boolean> = context.dataStore.data.map { it[BLE_APPEAR_TOKEN] ?: false }
    val getIntercomBatteryToken: Flow<Int> = context.dataStore.data.map { it[INTERCOM_BATTERY_TOKEN] ?: -1 }
    val getMusicPlayerToken: Flow<MusicService> = context.dataStore.data.map { pref ->
        MusicService.fromId(pref[MUSIC_PLAYER_TOKEN] ?: 0) ?: MusicService.SPOTIFY 
    }

    // Firmware Getters
    val getFwAutoDownload: Flow<Boolean> = context.dataStore.data.map { it[FW_AUTO_DOWNLOAD] ?: true }
    val getFwVersion: Flow<String> = context.dataStore.data.map { it[FW_VERSION] ?: "V0001" }
    val getFwUseHotspot: Flow<Boolean> = context.dataStore.data.map { it[FW_USE_HOTSPOT] ?: false }
    val getFwWlanSsid: Flow<String> = context.dataStore.data.map { it[FW_WLAN_SSID] ?: "" }
    val getFwWlanPwd: Flow<String> = context.dataStore.data.map { it[FW_WLAN_PWD] ?: "" }
    val getFwHotspotSsid: Flow<String> = context.dataStore.data.map { it[FW_HOTSPOT_SSID] ?: "" }
    val getFwHotspotPwd: Flow<String> = context.dataStore.data.map { it[FW_HOTSPOT_PWD] ?: "" }

    // Service Toggle Getters
    val getNotificationsEnabled: Flow<Boolean> = context.dataStore.data.map { it[NOTIFICATIONS_ENABLED] ?: true }
    val getMusicEnabled: Flow<Boolean> = context.dataStore.data.map { it[MUSIC_ENABLED] ?: true }
    val getVolumeEnabled: Flow<Boolean> = context.dataStore.data.map { it[VOLUME_ENABLED] ?: true }
    val getIntercomBatteryEnabled: Flow<Boolean> = context.dataStore.data.map { it[INTERCOM_BATTERY_ENABLED] ?: true }

    // Save Methods
    suspend fun saveFistLaunch(token: Boolean) { context.dataStore.edit { it[FIRST_LAUNCH_TOKEN] = token } }
    suspend fun saveSelectedMac(token: String) { context.dataStore.edit { it[SEL_MAC_TOKEN] = token } }
    suspend fun saveInterComConnected(token: Boolean) { context.dataStore.edit { it[INTERCOM_CONNECTED_TOKEN] = token } }
    suspend fun saveMediaTitle(token: String) { context.dataStore.edit { it[MEDIA_TITLE_TOKEN] = token } }
    suspend fun saveMediaArtist(token: String) { context.dataStore.edit { it[MEDIA_ARTIST_TOKEN] = token } }
    suspend fun saveMediaPlayState(token: Boolean) { context.dataStore.edit { it[MEDIA_PLAYSTATE_TOKEN] = token } }
    suspend fun saveMediaTrackLength(token: Int) { context.dataStore.edit { it[MEDIA_TRACK_LENGTH_TOKEN] = token } }
    suspend fun saveMediaPlayBackPosition(token: Int) { context.dataStore.edit { it[MEDIA_PLAYBACK_POSITION_TOKEN] = token } }
    suspend fun saveMediaPlay(token: Int) { context.dataStore.edit { it[MUSIC_PLAYER_TOKEN] = token } }
    
    suspend fun saveFwAutoDownload(value: Boolean) { context.dataStore.edit { it[FW_AUTO_DOWNLOAD] = value } }
    suspend fun saveFwVersion(value: String) { context.dataStore.edit { it[FW_VERSION] = value } }
    suspend fun saveFwUseHotspot(value: Boolean) { context.dataStore.edit { it[FW_USE_HOTSPOT] = value } }
    suspend fun saveFwWlanSsid(value: String) { context.dataStore.edit { it[FW_WLAN_SSID] = value } }
    suspend fun saveFwWlanPwd(value: String) { context.dataStore.edit { it[FW_WLAN_PWD] = value } }
    suspend fun saveFwHotspotSsid(value: String) { context.dataStore.edit { it[FW_HOTSPOT_SSID] = value } }
    suspend fun saveFwHotspotPwd(value: String) { context.dataStore.edit { it[FW_HOTSPOT_PWD] = value } }

    // Service Toggle Savers
    suspend fun saveNotificationsEnabled(value: Boolean) { context.dataStore.edit { it[NOTIFICATIONS_ENABLED] = value } }
    suspend fun saveMusicEnabled(value: Boolean) { context.dataStore.edit { it[MUSIC_ENABLED] = value } }
    suspend fun saveVolumeEnabled(value: Boolean) { context.dataStore.edit { it[VOLUME_ENABLED] = value } }
    suspend fun saveIntercomBatteryEnabled(value: Boolean) { context.dataStore.edit { it[INTERCOM_BATTERY_ENABLED] = value } }

    // Service Toggle one-shot getters
    suspend fun isNotificationsEnabled(): Boolean = context.dataStore.data.first()[NOTIFICATIONS_ENABLED] ?: true
    suspend fun isMusicEnabled(): Boolean = context.dataStore.data.first()[MUSIC_ENABLED] ?: true
    suspend fun isVolumeEnabled(): Boolean = context.dataStore.data.first()[VOLUME_ENABLED] ?: true
    suspend fun isIntercomBatteryEnabled(): Boolean = context.dataStore.data.first()[INTERCOM_BATTERY_ENABLED] ?: true

    suspend fun clearMediaSaves() {
        context.dataStore.edit { preferences ->
            preferences[MEDIA_PLAYSTATE_TOKEN] = false
            preferences[MEDIA_ARTIST_TOKEN] = ""
            preferences[MEDIA_TITLE_TOKEN] = ""
            preferences[MEDIA_TRACK_LENGTH_TOKEN] = 0
            preferences[MEDIA_PLAYBACK_POSITION_TOKEN] = 0
        }
    }

    suspend fun saveNotification(category: String, title: String, text: String, app: String?, appname: String?) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATION_TITLE_TOKEN] = title
            preferences[NOTIFICATION_TEXT_TOKEN] = text
            preferences[NOTIFICATION_APP_TOKEN] = app.toString()
            preferences[NOTIFICATION_CATEGORY_TOKEN] = category
            preferences[NOTIFICATION_APP_NAME_TOKEN] = appname.toString()
        }
    }

    suspend fun saveBLEMAC(token: String) { context.dataStore.edit { it[BLE_MAC_TOKEN] = token } }
    suspend fun saveBLEAppear(token: Boolean) { context.dataStore.edit { it[BLE_APPEAR_TOKEN] = token } }
    suspend fun saveIntercomBattery(level: Int) { context.dataStore.edit { it[INTERCOM_BATTERY_TOKEN] = level } }

    suspend fun getBLEMAC(): String? = context.dataStore.data.first()[BLE_MAC_TOKEN]
    suspend fun getBLEAppear(): Boolean = context.dataStore.data.first()[BLE_APPEAR_TOKEN] ?: false
    suspend fun getInterComMAC(): String? = context.dataStore.data.first()[SEL_MAC_TOKEN]
    suspend fun getMusicPlayer(): MusicService? = MusicService.fromId(context.dataStore.data.first()[MUSIC_PLAYER_TOKEN] ?: 0)
    suspend fun saveMusicPlayer(service: MusicService) { context.dataStore.edit { it[MUSIC_PLAYER_TOKEN] = service.id } }
}

enum class MusicService(val id: Int, val displayName: String) {
    SPOTIFY(0, "Spotify"),
    YOUTUBE_MUSIC(1, "YouTube Music");
    companion object {
        fun fromId(id: Int): MusicService? = entries.find { it.id == id }
        @Suppress("unused")
        fun fromDisplayName(displayName: String): MusicService? = entries.find { it.displayName == displayName }
    }
}
