#include "screens.h"
#include "func_u8g2.h"


void drawPopups();

static bool isPopupActive() {
  return notificationDisplayed || volumeDisplayed || lowBatteryDisplayed || firmwareUpdateEnabled || previousResetMillis > 0 || (pairingActive && (millis() - pairingConnectTime >= 1000));
}

// Set dim contrast on the CURRENT display only, right before sendBuffer
static void setDimContrast() {
  uint8_t c = currentDisplayContrast;
  uint8_t d = (uint8_t)(((uint16_t)c * popupDimPercent) / 100);
  u8g2_current->setContrast(d < 25 ? 25 : d);
}

// Set bright contrast on the CURRENT display only, right before sendBuffer
static void setBrightContrast() {
  u8g2_current->setContrast(currentDisplayContrast);
}


void defaultScreen(){
  // defaultScreen has no UI lines → dual-pass only needed when popup is active
  bool popup = isPopupActive();
  bool doDim = dualPassEnabled && popup && dualPassFrame == 0;
  bool doBright = dualPassEnabled && popup && dualPassFrame == 1;

  for(int side = 0; side < 2; side++){
    if(side == 0) setLeftSide(); else setRightSide();
    u8g2_current->clearBuffer();

    if(!dualPassEnabled || !popup) {
      // No dual-pass needed: draw everything at current brightness
      if(side == 0) drawFullClock(); else drawFullTemperature();
      drawPopups();
      setBrightContrast();
    } else if(doDim) {
      // Dim pass: all content + popups (popup bg masks content behind it)
      if(side == 0) drawFullClock(); else drawFullTemperature();
      drawPopups();
      setDimContrast();
    } else {
      // Bright pass: only popups
      drawPopups();
      setBrightContrast();
    }

    u8g2_current->sendBuffer();
  }
}

void splitScreen() {
  String localMusicTitle, localMusicArtist, localNetworkType;
  if(xSemaphoreTake(dataMutex, pdMS_TO_TICKS(50)) == pdTRUE){
    localMusicTitle = music_title;
    localMusicArtist = music_artist;
    localNetworkType = network_type;
    xSemaphoreGive(dataMutex);
  }

  bool popup = isPopupActive();

  for(int side = 0; side < 2; side++){
    if(side == 0) setLeftSide(); else setRightSide();
    u8g2_current->clearBuffer();

    if(!dualPassEnabled) {
      // Single pass: everything
      if(side == 0) {
        drawUiClock();
        drawUI_left();
        drawBattery(3,2,batteryIconSelection);
        drawMusicUI(localMusicTitle, localMusicArtist, music_timer, music_length, playstate, sEEPROM.screen);
      } else {
        drawTemperature(global_temp);
        drawBLConnect(OLED_WIDTH/2-2, 2, blConnected);
        drawNetworkStatus(OLED_WIDTH/2-4-32, 2, network_signal, localNetworkType);
        drawUI_right();
        drawMusicUI(localMusicTitle, localMusicArtist, music_timer, music_length, playstate, sEEPROM.screen);
      }
      drawPopups();
      u8g2_current->sendBuffer();
      continue;
    }

    if(popup) {
      // Popup active: dim pass = everything + popups, bright pass = popups only
      if(dualPassFrame == 0) {
        if(side == 0) {
          drawUiClock();
          drawUI_left();
          drawBattery(3,2,batteryIconSelection);
          drawMusicUI(localMusicTitle, localMusicArtist, music_timer, music_length, playstate, sEEPROM.screen);
        } else {
          drawTemperature(global_temp);
          drawBLConnect(OLED_WIDTH/2-2, 2, blConnected);
          drawNetworkStatus(OLED_WIDTH/2-4-32, 2, network_signal, localNetworkType);
          drawUI_right();
          drawMusicUI(localMusicTitle, localMusicArtist, music_timer, music_length, playstate, sEEPROM.screen);
        }
        drawPopups();
        setDimContrast();
      } else {
        drawPopups();
        setBrightContrast();
      }
    } else {
      // No popup: dim pass = everything, bright pass = text/icons only (no UI lines)
      if(dualPassFrame == 0) {
        if(side == 0) {
          drawUiClock();
          drawUI_left();
          drawBattery(3,2,batteryIconSelection);
          drawMusicUI(localMusicTitle, localMusicArtist, music_timer, music_length, playstate, sEEPROM.screen);
        } else {
          drawTemperature(global_temp);
          drawBLConnect(OLED_WIDTH/2-2, 2, blConnected);
          drawNetworkStatus(OLED_WIDTH/2-4-32, 2, network_signal, localNetworkType);
          drawUI_right();
          drawMusicUI(localMusicTitle, localMusicArtist, music_timer, music_length, playstate, sEEPROM.screen);
        }
        drawPopups();
        setDimContrast();
      } else {
        if(side == 0) {
          drawUiClock();
          drawBattery(3,2,batteryIconSelection);
          drawMusicUI(localMusicTitle, localMusicArtist, music_timer, music_length, playstate, sEEPROM.screen);
        } else {
          drawTemperature(global_temp);
          drawBLConnect(OLED_WIDTH/2-2, 2, blConnected);
          drawNetworkStatus(OLED_WIDTH/2-4-32, 2, network_signal, localNetworkType);
          drawMusicUI(localMusicTitle, localMusicArtist, music_timer, music_length, playstate, sEEPROM.screen);
        }
        drawPopups();
        setBrightContrast();
      }
    }

    u8g2_current->sendBuffer();
  }
}

void mediaScreen() {
  String localMusicTitle, localMusicArtist, localNetworkType;
  if(xSemaphoreTake(dataMutex, pdMS_TO_TICKS(50)) == pdTRUE){
    localMusicTitle = music_title;
    localMusicArtist = music_artist;
    localNetworkType = network_type;
    xSemaphoreGive(dataMutex);
  }

  bool popup = isPopupActive();

  for(int side = 0; side < 2; side++){
    if(side == 0) setLeftSide(); else setRightSide();
    u8g2_current->clearBuffer();

    if(!dualPassEnabled) {
      if(side == 0) {
        drawUiClock();
        drawUI_left();
        drawBattery(3,2,batteryIconSelection);
        drawMusicUI(localMusicTitle, localMusicArtist, music_timer, music_length, playstate, sEEPROM.screen);
      } else {
        drawTemperature(global_temp);
        drawUI_right();
        drawBLConnect(OLED_WIDTH/2-2, 2, blConnected);
        drawNetworkStatus(OLED_WIDTH/2-4-32, 2, network_signal, localNetworkType);
        drawMusicUI(localMusicTitle, localMusicArtist, music_timer, music_length, playstate, sEEPROM.screen);
      }
      drawPopups();
      u8g2_current->sendBuffer();
      continue;
    }

    if(popup) {
      if(dualPassFrame == 0) {
        if(side == 0) {
          drawUiClock();
          drawUI_left();
          drawBattery(3,2,batteryIconSelection);
          drawMusicUI(localMusicTitle, localMusicArtist, music_timer, music_length, playstate, sEEPROM.screen);
        } else {
          drawTemperature(global_temp);
          drawUI_right();
          drawBLConnect(OLED_WIDTH/2-2, 2, blConnected);
          drawNetworkStatus(OLED_WIDTH/2-4-32, 2, network_signal, localNetworkType);
          drawMusicUI(localMusicTitle, localMusicArtist, music_timer, music_length, playstate, sEEPROM.screen);
        }
        drawPopups();
        setDimContrast();
      } else {
        drawPopups();
        setBrightContrast();
      }
    } else {
      if(dualPassFrame == 0) {
        if(side == 0) {
          drawUiClock();
          drawUI_left();
          drawBattery(3,2,batteryIconSelection);
          drawMusicUI(localMusicTitle, localMusicArtist, music_timer, music_length, playstate, sEEPROM.screen);
        } else {
          drawTemperature(global_temp);
          drawUI_right();
          drawBLConnect(OLED_WIDTH/2-2, 2, blConnected);
          drawNetworkStatus(OLED_WIDTH/2-4-32, 2, network_signal, localNetworkType);
          drawMusicUI(localMusicTitle, localMusicArtist, music_timer, music_length, playstate, sEEPROM.screen);
        }
        drawPopups();
        setDimContrast();
      } else {
        if(side == 0) {
          drawUiClock();
          drawBattery(3,2,batteryIconSelection);
          drawMusicUI(localMusicTitle, localMusicArtist, music_timer, music_length, playstate, sEEPROM.screen);
        } else {
          drawTemperature(global_temp);
          drawBLConnect(OLED_WIDTH/2-2, 2, blConnected);
          drawNetworkStatus(OLED_WIDTH/2-4-32, 2, network_signal, localNetworkType);
          drawMusicUI(localMusicTitle, localMusicArtist, music_timer, music_length, playstate, sEEPROM.screen);
        }
        drawPopups();
        setBrightContrast();
      }
    }

    u8g2_current->sendBuffer();
  }
}

void drawPopups(){
  #ifdef DEBUG
  {
    char fpsBuf[8];
    snprintf(fpsBuf, sizeof(fpsBuf), "%uFPS", fps_value);
    u8g2_current->setFont(u8g2_font_profont10_tf);
    u8g2_current->drawStr(OLED_WIDTH/2 - u8g2_current->getStrWidth(fpsBuf) - 1, 30, fpsBuf);
  }
  #endif
  if(notificationDisplayed){
    String localType, localTitle, localText;
    if(xSemaphoreTake(dataMutex, pdMS_TO_TICKS(50)) == pdTRUE){
      localType = notificationType;
      localTitle = notificationTitle;
      localText = notificationText;
      xSemaphoreGive(dataMutex);
    }
    drawNotificationPopup(localType, localTitle, localText);
  }
  drawVolumePopup();
  drawLowBatteryPopup();
  drawOTAPopup();
  drawResetPopup();
  drawPairingPopup();
}