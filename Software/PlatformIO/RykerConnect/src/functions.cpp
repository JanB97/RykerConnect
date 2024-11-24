#include "functions.h"
#include <CRC32.h>
#include <Preferences.h>
#include <mcp9808.h>

bool h12Flag;
bool pmFlag;
void MCP_shutdown_wake(boolean sw);

void runTimers(){
  unsigned long currentMillis = millis();
  if(currentMillis - previousBatteryIconMillis >= sEEPROM.battery_icon_interval){
      previousBatteryIconMillis = currentMillis;
      int check_next = batteryIconSelection+1;
      if (check_next > sizeof(sEEPROM.battery_icon_selection)){
        check_next = 0;
      }
      for(int i = check_next; i<sizeof(sEEPROM.battery_icon_selection); i++){
          if(sEEPROM.battery_icon_selection[i]){
            batteryIconSelection = i;
            goto after_battery_selection;
          }
      }
      if(check_next != 0){
          for(int i = 0; i<check_next; i++){
            if(sEEPROM.battery_icon_selection[i]){
              batteryIconSelection = i;
              goto after_battery_selection;
            }
          }
      }
      batteryIconSelection = -1;

  }
  after_battery_selection:
  if(playstate){
    if (currentMillis - previousMusicMillis >= musicInterval) { //Music Zeit / Bar
      previousMusicMillis = currentMillis;
      music_timer++;
    }}else{
      previousMusicMillis = currentMillis;}
  
  if (currentMillis - previousScrollTitleMillis >= scrollTitleInterval) { // Title Scrolling
    previousScrollTitleMillis = currentMillis;
    if(scroll_title_en){
        scroll_title++;
    }
  }
  if (currentMillis - previousScrollArtistMillis >= scrollArtistInterval) { // Artist Scrolling
    previousScrollArtistMillis = currentMillis;
    if(scroll_artist_en){
        scroll_artist++;
    }
  }
  if(previousNotificationMillis != 0){
    if(currentMillis - previousNotificationMillis >= sEEPROM.notification_interval){
      notificationDisplayed = false;
    }
  }
  if(previousResetMillis != 0){
    if(currentMillis - previousResetMillis >= RESET_INTERVAL){
        previousResetMillis = 0;
    }
  }

  // Time and Temp
  if(currentMillis - previousTimeMillis >= TIME_INTERVAL){
      previousTimeMillis = currentMillis;
      global_time = timeString(RTC.getHour(h12Flag, pmFlag)) + ":" + timeString(RTC.getMinute());
  }
  if(currentMillis - previousTempMillis >= TEMP_INTERVAL){
      previousTempMillis = currentMillis;
      if(ts.isConnected()){
        //MCP_shutdown_wake(false);
        //delay(65);
        global_temp = ts.getTemperature();
        //MCP_shutdown_wake(true);
      }else{
        global_temp = RTC.getTemperature();
      }     
  }
}


void MCP_shutdown_wake(boolean sw) {
  uint16_t conf_shutdown;
  uint16_t conf_register = ts.getConfigRegister();
  if (sw == true) {
    conf_shutdown = conf_register | MCP9808_SHUTDOWN;
  }
  else {
    conf_shutdown = conf_register & ~MCP9808_SHUTDOWN;
  }
  ts.setConfigRegister(conf_shutdown);
}


String timeString(uint8_t value){
  if(value<10){
    return "0" + String(value);
  }
  return String(value);
}

void reset_settings(){
    sEEPROM.adaptive_brightness = ADAPTIVE_BRIGHTNESS;
    sEEPROM.display_brightness = DISPLAY_BRIGHTNESS
    sEEPROM.screen = SCREEN_SELECTION;
    sEEPROM.sub_screen = SUB_SCREEN_SELECTION;
    sEEPROM.battery_icon_selection[0] = BATTERY_ICON_SELECTION1;
    sEEPROM.battery_icon_selection[1] = BATTERY_ICON_SELECTION2;
    sEEPROM.battery_icon_selection[2] = BATTERY_ICON_SELECTION3;
    sEEPROM.battery_icon_selection[3] = BATTERY_ICON_SELECTION4;
    sEEPROM.battery_icon_first = BATTERY_ICON_FIRST;
    sEEPROM.battery_icon_interval = BATTERY_ICON_INTERVAL;
    sEEPROM.notification_interval = NOTIFICATION_INTERVAL;
    sEEPROM.crc = calc_crc();

    D_printf("Settings reset... \n New CRC: %i, Saving Settings to EEPROM", sEEPROM.crc);
    //EEPROM.put(0, sEEPROM);
    //EEPROM.commit();
    prefs.putBytes("settings", &sEEPROM, sizeof(EEPROM_Struct));
}

bool valiade_eeprom(){
  uint32_t crc = calc_crc();
  if(crc == sEEPROM.crc){
    D_println("CRC MATCH!");
    return true;
  }
  D_println("CRC DOES NOT MATCH!");
  return false;
}

uint32_t calc_crc(){
  //uint8_t numBuff = sizeof(EEPROM_Struct)-sizeof(sEEPROM.crc)-3;
  uint8_t numBuff = sizeof(EEPROM_Struct)-sizeof(sEEPROM.crc);
  uint8_t crcBuffer[numBuff];
  memcpy(crcBuffer, &sEEPROM,numBuff);
  uint32_t crc = CRC32::calculate(crcBuffer, sizeof(crcBuffer));
  D_println();
  D_printf("Size Of CRC: %i", sizeof(sEEPROM.crc));
  D_printf("Size of Struct: %i", sizeof(EEPROM_Struct));
  #ifdef DEBUG
  D_print("| ");
  for (int i = 0; i<numBuff;i++){
    D_print(crcBuffer[i], BIN);
    D_print(" | ");
  }
  #endif
  D_println("");
  D_print("CRC alt: ");
  D_println(sEEPROM.crc, HEX);   
  D_print("CRC neu: ");
  D_println(crc, HEX);
  
  return crc;
}