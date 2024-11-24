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
        //region UI STATE TOKEN
        private val FIRST_LAUNCH_TOKEN = booleanPreferencesKey("first_launch")
        //endregion
        //region INTERCOM TOKEN
        private val SEL_MAC_TOKEN = stringPreferencesKey("sel_mac")
        private val INTERCOM_CONNECTED_TOKEN = booleanPreferencesKey("intercom_connected")
        //endregion
        //region MEDIA TOKEN
        private val MEDIA_TITLE_TOKEN = stringPreferencesKey("media_title")
        private val MEDIA_ARTIST_TOKEN = stringPreferencesKey("media_artist")
        private val MEDIA_PLAYSTATE_TOKEN = booleanPreferencesKey("media_playstate")
        private val MEDIA_TRACK_LENGTH_TOKEN = intPreferencesKey("media_track_length")
        private val MEDIA_PLAYBACK_POSITION_TOKEN = intPreferencesKey("media_playback_position")
        //endregion
        //region NOTIFICATION TOKEN
        private val NOTIFICATION_TITLE_TOKEN = stringPreferencesKey("notification_title")
        private val NOTIFICATION_TEXT_TOKEN = stringPreferencesKey("notification_text")
        private val NOTIFICATION_CATEGORY_TOKEN = stringPreferencesKey("notification_category")
        private val NOTIFICATION_APP_TOKEN = stringPreferencesKey("notification_app")
        private val NOTIFICATION_APP_NAME_TOKEN = stringPreferencesKey("notification_app_name")
        //endregion
        //region BLE
        private val BLE_MAC_TOKEN = stringPreferencesKey("ble_mac")
        private val BLE_APPEAR_TOKEN = booleanPreferencesKey("ble_appear")
        //endregion
    }

    //---------------------------------
    //_________GETTER__________________

    //region UI STATE GET
    val getFirstLaunchToken: Flow<Boolean> = context.dataStore.data.map { it[FIRST_LAUNCH_TOKEN] ?: true }
    //endregion
    //region INTERCOM GET
    val getSelectedMacToken: Flow<String> = context.dataStore.data.map { it[SEL_MAC_TOKEN] ?: "" }
    val getInterComConnectedToken: Flow<Boolean> = context.dataStore.data.map { it[INTERCOM_CONNECTED_TOKEN] ?: false }
    //endregion
    //region MEDIA GET
    val getMediaTitleToken: Flow<String> = context.dataStore.data.map { it[MEDIA_TITLE_TOKEN] ?: "" }
    val getMediaArtistToken: Flow<String> = context.dataStore.data.map { it[MEDIA_ARTIST_TOKEN] ?: "" }
    val getMediaPlayStateToken: Flow<Boolean> = context.dataStore.data.map { it[MEDIA_PLAYSTATE_TOKEN] ?: false }
    val getMediaTrackLengthToken: Flow<Int> = context.dataStore.data.map { it[MEDIA_TRACK_LENGTH_TOKEN] ?: 0 }
    val getMediaPlayBackPositionToken: Flow<Int> = context.dataStore.data.map { it[MEDIA_PLAYBACK_POSITION_TOKEN] ?: 0 }
    //endregion
    //region NOTIFICATION GET
    val getNotificationTitleToken: Flow<String> = context.dataStore.data.map { it[NOTIFICATION_TITLE_TOKEN] ?: "" }
    val getNotificationTextToken: Flow<String> = context.dataStore.data.map { it[NOTIFICATION_TEXT_TOKEN] ?: "" }
    val getNotificationCategoryToken: Flow<String> = context.dataStore.data.map { it[NOTIFICATION_CATEGORY_TOKEN] ?: "" }
    val getNotificationAppToken: Flow<String> = context.dataStore.data.map { it[NOTIFICATION_APP_TOKEN] ?: "" }
    val getNotificationAppNameToken: Flow<String> = context.dataStore.data.map { it[NOTIFICATION_APP_NAME_TOKEN] ?: "" }
    //endregion
    //region BLE
    val getBLEMACToken: Flow<String> = context.dataStore.data.map { it[BLE_MAC_TOKEN] ?: "" }
    val getBLEAppearToken: Flow<Boolean> = context.dataStore.data.map { it[BLE_APPEAR_TOKEN] ?: false }
    //endregion


    //---------------------------------
    //___________SAVE__________________

    //region UI STATE SAVE
    suspend fun saveFistLaunch(token: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[FIRST_LAUNCH_TOKEN] = token
        }
    }
    //endregion
    //region INTERCOM SAVE
    suspend fun saveSelectedMac(token: String){
        context.dataStore.edit { preferences ->
            preferences[SEL_MAC_TOKEN] = token
        }
    }
    suspend fun saveInterComConnected(token: Boolean){
        context.dataStore.edit { preferences ->
            preferences[INTERCOM_CONNECTED_TOKEN] = token
        }
    }
    //endregion
    //region MEDIA SAVE
    suspend fun saveMediaTitle(token: String) {
        context.dataStore.edit { preferences ->
            preferences[MEDIA_TITLE_TOKEN] = token
        }
    }
    suspend fun saveMediaArtist(token: String) {
        context.dataStore.edit { preferences ->
            preferences[MEDIA_ARTIST_TOKEN] = token
        }
    }
    suspend fun saveMediaPlayState(token: Boolean){
        context.dataStore.edit { preferences ->
            preferences[MEDIA_PLAYSTATE_TOKEN] = token
        }
    }
    suspend fun saveMediaTrackLength(token: Int){
        context.dataStore.edit { preferences ->
            preferences[MEDIA_TRACK_LENGTH_TOKEN] = token.toInt()
        }
    }
    suspend fun saveMediaPlayBackPosition(token: Int){
        context.dataStore.edit { preferences ->
            preferences[MEDIA_PLAYBACK_POSITION_TOKEN] = token.toInt()
        }
    }
    suspend fun clearMediaSaves(){
        context.dataStore.edit { preferences ->
            preferences[MEDIA_PLAYSTATE_TOKEN] = false
            preferences[MEDIA_ARTIST_TOKEN] = ""
            preferences[MEDIA_TITLE_TOKEN] = ""
            preferences[MEDIA_TRACK_LENGTH_TOKEN] = 0
            preferences[MEDIA_PLAYBACK_POSITION_TOKEN] = 0
        }
    }

    //endregion
    //region NOTIFICATION SAVE
    suspend fun saveNotification(category: String, title: String, text: String, app: String?, appname: String?){
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATION_TITLE_TOKEN] = title
            preferences[NOTIFICATION_TEXT_TOKEN] = text
            preferences[NOTIFICATION_APP_TOKEN] = app.toString()
            preferences[NOTIFICATION_CATEGORY_TOKEN] = category
            preferences[NOTIFICATION_APP_NAME_TOKEN] = appname.toString()
        }
    }

    @Deprecated("Unsafe STATE", replaceWith = ReplaceWith("saveNotification()"))
    suspend fun saveNotificationTitle(token: String) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATION_TITLE_TOKEN] = token
        }
    }
    @Deprecated("Unsafe STATE", replaceWith = ReplaceWith("saveNotification()"))
    suspend fun saveNotificationText(token: String) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATION_TEXT_TOKEN] = token
        }
    }
    //endregion
    //region BLE
    suspend fun saveBLEMAC(token: String){
        context.dataStore.edit { preferences ->
            preferences[BLE_MAC_TOKEN] = token
        }
    }
    suspend fun saveBLEAppear(token: Boolean){
        context.dataStore.edit { preferences ->
            preferences[BLE_APPEAR_TOKEN] = token
        }
    }
    //endregion


    suspend fun getBLEMAC(): String? {
        return context.dataStore.data.first()[BLE_MAC_TOKEN]
    }
    suspend fun getBLEAppear(): Boolean? {
        return context.dataStore.data.first()[BLE_APPEAR_TOKEN]
    }

}