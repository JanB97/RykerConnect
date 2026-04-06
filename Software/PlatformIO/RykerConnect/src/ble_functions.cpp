#include "ble_functions.h"
#include "global_vars.h"
#include "functions.h"
#include "func_u8g2.h"
#include <WiFi.h>



void timeCallback(const uint8_t* data, uint8_t size)
{
    D_println(" Time Characteristic!: ");
    if(size == 3){
        D_printf(" Zeit: %i:%i", (uint8_t)data[0], (uint8_t)data[1]);
        RTC.setSecond((uint8_t)data[2]);
        RTC.setHour((uint8_t)data[0]);
        RTC.setMinute((uint8_t)data[1]);
    }
}

void networkCallback(uint16_t bytes)
{     
    D_println(" Network Characteristic!");
    String localType = convertNetworkType(bytes >> 8);
    D_printf(" Signal: %i | Type %i (%s)", (uint8_t)bytes, bytes >> 8, localType.c_str());
    network_signal = (uint8_t)bytes;
    if(xSemaphoreTake(dataMutex, pdMS_TO_TICKS(100)) == pdTRUE){
        network_type = localType;
        xSemaphoreGive(dataMutex);
    }
}
String convertNetworkType(byte type)
{
  switch(type){
      case 0: return "!";
      case 1: return "G";
      case 2: return "E";
      case 3: return "3G";
      case 4: return "H";
      case 5: return "H+";
      case 6: return "4G";
      case 7: return "4G+";
      case 8: return "5G";
      case 9: return "5G+";
      default: return "";
  }
}

void batteryCallback(int16_t value, uint8_t type)
{
    D_println(" Phone Battery Characteristic!");  
    if(type == 0){
        phone_battery_level = (int8_t)value;
        phone_battery_status = value >> 8;
        D_printf(" Phone Battery: %i; Status: %i", phone_battery_level, value>>8);
    }
    else if(type == 1){
        intercom_battery_level = (uint8_t)value;
        intercom_battery_status = value >> 8;
        D_printf(" Intercom Battery: %i", intercom_battery_level);
    }

    // Low battery warning check
    bool phoneLow = (phone_battery_level > 0 && phone_battery_level <= sEEPROM.low_battery_threshold_phone);
    bool intercomLow = (intercom_battery_level > 0 && intercom_battery_level <= sEEPROM.low_battery_threshold_intercom);
    if(phoneLow || intercomLow){
        String text = "";
        if(phoneLow) text += "Phone: " + String(phone_battery_level) + "%";
        if(phoneLow && intercomLow) text += "  ";
        if(intercomLow) text += "Intercom: " + String(intercom_battery_level) + "%";
        if(xSemaphoreTake(dataMutex, pdMS_TO_TICKS(50)) == pdTRUE){
            lowBatteryText = text;
            xSemaphoreGive(dataMutex);
        }
        lowBatteryDisplayed = true;
        previousLowBatteryMillis = millis();
    }
}

void volumeCallback(uint8_t value)
{
    D_printf(" Volume Characteristic: %i%%", value);
    volumeLevel = value;
    volumeDisplayed = true;
    previousVolumeMillis = millis();
}


void mediaDataCallback(const uint8_t* data, uint8_t size)
{
    D_println(" Media Data Characteristic!");  

    if(size < 5) return;

    bool localPlaystate = playstate;
    if((byte)data[0] == 0){
      localPlaystate = false;
    }else if((byte)data[0] == 1){
      localPlaystate = true;
    }

    uint16_t localTimer = (data[2]) | (data[1] << 8);
    uint16_t localLength = (data[4]) | (data[3] << 8);

    String localTitle = "";
    String localArtist = "";

    if(size > 5){
        uint8_t strLen = size - 5;
        String strValue((const char*)(data + 5), strLen);
        D_println(strValue.c_str());
        int delimiter_title = strValue.indexOf(0x03);
        localTitle = strValue.substring(0, delimiter_title);
        localTitle.trim();
        localArtist = strValue.substring(delimiter_title + 1);
        localArtist.trim();
    }

    D_printf(" Media Playstate: %i", localPlaystate);
    D_printf(" Position: %i", localTimer);
    D_printf(" Title: %s", localTitle.c_str());
    D_printf(" Artist: %s", localArtist.c_str());

    if(xSemaphoreTake(dataMutex, pdMS_TO_TICKS(100)) == pdTRUE){
        playstate = localPlaystate;
        music_timer = localTimer;
        music_length = localLength;
        music_title = localTitle;
        music_artist = localArtist;
        xSemaphoreGive(dataMutex);
    }
}

void notificationCallback(String value)
{
    D_println(" Notification Characteristic!");

    int delimiter_type = value.indexOf(0x03);
    int delimiter_title = value.indexOf(0x03,delimiter_type+1);

    String localType = value.substring(0,delimiter_type);
    String localTitle = value.substring(delimiter_type+1, delimiter_title);
    localTitle.trim();
    String localText = value.substring(delimiter_title+1, value.length());
    localText.trim();

    D_printf(" Type: %s", localType.c_str());
    D_printf(" Title: %s", localTitle.c_str());
    D_printf(" Text: %s", localText.c_str());
    
    if(xSemaphoreTake(dataMutex, pdMS_TO_TICKS(100)) == pdTRUE){
        notificationType = localType;
        notificationTitle = localTitle;
        notificationText = localText;
        previousNotificationMillis = millis();
        notificationDisplayed = true;
        xSemaphoreGive(dataMutex);
    }
}

void setDisplayBrightnessCallback(uint8_t brightness)
{
    D_println(" Brightness Characteristic!");
    D_printf(" Screen Brightness: %i", brightness);
    u8g2_0.setContrast(brightness);
    u8g2_1.setContrast(brightness);
}

void reinitDisplayCallback()
{
    D_println(" Display Reinit Characteristic!");
    initDisplays(sEEPROM.display_brightness);
    D_println(" Display reinitialized");
}

void handleSettingsCallback(const uint8_t* data, uint8_t size){
    D_println(" Settings Characteristic!");
    if(size == sizeof(EEPROM_Struct)-sizeof(uint32_t)){
            #pragma region DEBUGGINg
            #ifdef DEBUG
                D_print("| ");
                for(int i = 0; i<size; i++){
                    Serial.printf("Byte[%i]: %i | ", i, data[i]);
                }
                D_flush();
                D_println();
            #endif
            D_printf(" Bool Adaptive Brightness: %i", data[0]);
            D_printf(" UINT8 Display Brightness: %i", data[1]);
            D_printf(" UINT16 ADC Low: %i", (uint16_t)(data[2]) | ((uint16_t)(data[3]) << 8));
            D_printf(" UINT16 ADC High: %i", (uint16_t)(data[4]) | ((uint16_t)(data[5]) << 8));
            D_printf(" UINT8 Screen: %i", data[6]);
            D_printf(" UINT8 Sub Screen: %i", data[7]);
            D_printf(" BOOL ICON1: %i", data[8]);
            D_printf(" BOOL ICON2: %i", data[9]);
            D_printf(" BOOL ICON3: %i", data[10]);
            D_printf(" BOOL ICON4: %i", data[11]);
            D_flush();
            D_printf(" INT8 First Battery Icon: %i", data[12]);
            D_printf(" UINT32 Battery INTERVAL: %i", (data[16] << 24) | (data[15] << 16) | (data[14] << 8) | data[13]);
            D_printf(" UINT8 Low Battery Threshold Phone: %i", data[17]);
            D_printf(" UINT8 Low Battery Threshold Intercom: %i", data[18]);
            D_printf(" UINT32 Notification INTERVAL: %i", (data[22] << 24) | (data[21] << 16) | (data[20] << 8) | data[19]);
            D_flush();
            #pragma endregion

            sEEPROM.adaptive_brightness = (bool)data[0];
            sEEPROM.display_brightness = (uint8_t)data[1];
            uint16_t new_adc_low  = (uint16_t)(data[2]) | ((uint16_t)(data[3]) << 8);
            uint16_t new_adc_high = (uint16_t)(data[4]) | ((uint16_t)(data[5]) << 8);
            if(new_adc_high > 0 && new_adc_high > new_adc_low){
                sEEPROM.auto_brightness_adc_low  = new_adc_low;
                sEEPROM.auto_brightness_adc_high = new_adc_high;
            }
            sEEPROM.screen = (uint8_t)data[6];
            sEEPROM.sub_screen = (uint8_t)data[7];
            sEEPROM.battery_icon_selection[0] = (bool)data[8];
            sEEPROM.battery_icon_selection[1] = (bool)data[9];
            sEEPROM.battery_icon_selection[2] = (bool)data[10];
            sEEPROM.battery_icon_selection[3] = (bool)data[11];
            sEEPROM.battery_icon_first = (int8_t)data[12];
            sEEPROM.battery_icon_interval = (data[16] << 24) | (data[15] << 16) | (data[14] << 8) | data[13];
            sEEPROM.low_battery_threshold_phone = data[17];
            sEEPROM.low_battery_threshold_intercom = data[18];
            sEEPROM.notification_interval = (data[22] << 24) | (data[21] << 16) | (data[20] << 8) | data[19];
            // temp_calibration at bytes 23-26: not overwritten from BLE
            // reserved bytes 27-34: not parsed
            sEEPROM.crc = calc_crc();
            //EEPROM.put(0,sEEPROM);
            //EEPROM.commit();
            prefs.putBytes("settings", &sEEPROM, sizeof(EEPROM_Struct));

    }else{
        D_printf(" Incomplete Data! Data Size %i != EEPROM Size %i", size, sizeof(EEPROM_Struct)-sizeof(uint32_t));
    }
}

void handleFirmwareUpdateCallback(String value){
    D_println(" FIRMWARE UUID");
    int delimiter = value.indexOf(0x03);
    String ssid = value.substring(0, delimiter);
    String rest = value.substring(delimiter + 1);
    int delimiter2 = rest.indexOf(0x03);
    String password;
    if(delimiter2 >= 0){
        password = rest.substring(0, delimiter2);
        firmwareDownloadUrl = rest.substring(delimiter2 + 1);
        D_printf(" Begin WiFi | SSID: %s - PW: %s - URL: %s\n", ssid.c_str(), password.c_str(), firmwareDownloadUrl.c_str());
    } else {
        password = rest;
        firmwareDownloadUrl = "";
        D_printf(" Begin WiFi | SSID: %s - PW: %s (no URL, browser fallback)\n", ssid.c_str(), password.c_str());
    }
    firmwareUpdateEnabled = true;
    WiFi.begin(ssid.c_str(), password.c_str());
}

void handleFirmwareResetCallback(uint16_t value){
     D_printf(" RESET UUID - Pin get: %i | Pin needed: %i", value, resetPin);
    if(value == 0){
        D_println(" Reset cancelled");
        previousResetMillis = 0;
    }else if(value == resetPin){
         D_println(" Pin correct, reset");
        reset_settings();
        ESP.restart();
    }else{
        srand(millis());
        resetPin = 1000 + (rand() % 9999);
        D_println(" Wrong, show Ui");
        previousResetMillis = millis();
    }
}