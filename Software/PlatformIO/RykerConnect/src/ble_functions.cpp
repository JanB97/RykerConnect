#include "ble_functions.h"
#include "global_vars.h"
#include "functions.h"
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
    D_printf(" Signal: %i | Type %i (%s)", (uint8_t)bytes, bytes >> 8,  convertNetworkType(bytes >> 8));
    network_signal = (uint8_t)bytes;
    network_type = convertNetworkType(bytes >> 8);
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
}


void mediaDataCallback(const uint8_t* data, uint8_t size)
{
    D_println(" Media Data Characteristic!");  

    if((byte)data[0] == 0){
      playstate = false;
    }else if((byte)data[0] == 1){
      playstate = true;
    }
    //music_timer = value.substring(1, value.length()).toInt();
    //music_timer = (uint32_t)

    music_timer = (data[2]) | (data[1] << 8);
    music_length = (data[4]) | (data[3] << 8);

    if(size >5){
        char str[size-4];
        memcpy(str, data+5, size);
        D_println(str);
        int delimiter_title = String(str).indexOf(0x03);
        music_title = String(str).substring(0, delimiter_title);
        music_title.trim();
        music_artist = String(str).substring(delimiter_title + 1, size-4);
        music_artist.trim();
    }

    //D_print("| ");
    //for(int i = 0; i<size; i++){
    //    Serial.printf("Byte[%i]: %i | ", i, data[i]);
    //}
    //D_println();

    D_printf(" Media Playstate: %i", playstate);
    D_printf(" Position: %i", music_timer);
    D_printf(" Title: %s", music_title);
    D_printf(" Artist: %s", music_artist);
}

void notificationCallback(String value)
{
    D_println(" Notification Characteristic!");

    int delimiter_type = value.indexOf(0x03);
    int delimiter_title = value.indexOf(0x03,delimiter_type+1);

    notificationType = value.substring(0,delimiter_type);
    notificationTitle = value.substring(delimiter_type+1, delimiter_title);
    notificationTitle.trim();
    notificationText = value.substring(delimiter_title+1, value.length());
    notificationText.trim();

    D_printf(" Type: %s", notificationType);
    D_printf(" Title: %s", notificationTitle);
    D_printf(" Text: %s", notificationText.c_str());
    
    previousNotificationMillis = millis();
    notificationDisplayed = true;
}

void setDisplayBrightnessCallback(uint8_t brightness)
{
    D_println(" Brightness Characteristic!");
    D_printf(" Screen Brightness: %i", brightness);
    u8g2_0.setContrast(brightness);
    u8g2_1.setContrast(brightness);
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
                D_println();
            #endif
            D_printf(" Bool Adaptive Brightness: %i", data[0]);
            D_printf(" UINT8 Display Brightness: %i", data[1]);
            D_printf(" UINT8 Screen: %i", data[2]);
            D_printf(" UINT8 Sub Screen: %i", data[3]);
            D_printf(" BOOL ICON1: %i", data[4]);
            D_printf(" BOOL ICON2: %i", data[5]);
            D_printf(" BOOL ICON3: %i", data[6]);
            D_printf(" BOOL ICON4: %i", data[7]);
            D_printf(" UINT8_t First Battery Icon: %i", data[8]);
            D_printf(" UINT32 Battery INTERVAL: %i", (data[12] << 24) | (data[11] << 16) | (data[10] << 8) | data[9]);
            D_printf(" UINT32 Notification INTERVAL: %i", (data[16] << 24) | (data[15] << 16) | (data[14] << 8) | data[13]);
            #pragma endregion

            sEEPROM.adaptive_brightness = (bool)data[0];
            sEEPROM.display_brightness = (uint8_t)data[1];
            sEEPROM.screen = (uint8_t)data[2];
            sEEPROM.sub_screen = (uint8_t)data[3];
            sEEPROM.battery_icon_selection[0] = (bool)data[4];
            sEEPROM.battery_icon_selection[1] = (bool)data[5];
            sEEPROM.battery_icon_selection[2] = (bool)data[6];
            sEEPROM.battery_icon_selection[3] = (bool)data[7];
            sEEPROM.battery_icon_first = (int8_t)data[8];
            sEEPROM.battery_icon_interval = (data[12] << 24) | (data[11] << 16) | (data[10] << 8) | data[9];
            sEEPROM.notification_interval = (data[16] << 24) | (data[15] << 16) | (data[14] << 8) | data[13];
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
    firmwareUpdateEnabled = true;
    D_printf(" Begin WiFi | SSID: %s - PW: %s", value.substring(0,delimiter).c_str(), value.substring(delimiter+1, value.length()).c_str());
    WiFi.begin(value.substring(0,delimiter).c_str(), value.substring(delimiter+1, value.length()).c_str());    
}

void handleFirmwareResetCallback(uint16_t value){
     D_printf(" RESET UUID - Pin get: %i | Pin needed: %i", value, resetPin);
    if(value == resetPin){
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