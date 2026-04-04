#include "screens.h"
#include "func_u8g2.h"


void drawPopups();


void defaultScreen(){

/*
setLeftSide();
  u8g2_current->firstPage();
  do { 
      drawFullClock();
      drawPopups();
    } 
  while (u8g2_current->nextPage());
  setRightSide();
  u8g2_current->firstPage();
  do {
      drawFullTemperature();
      drawPopups();
    } 
  while ( u8g2_current->nextPage());
  */

  setLeftSide();
  u8g2_current->clearBuffer();					// clear the internal memory
  drawFullClock();
  drawPopups();
  u8g2_current->sendBuffer();					// transfer internal memory to the display
  
  setRightSide();
  u8g2_current->clearBuffer();					// clear the internal memory
  drawFullTemperature();
  drawPopups();
  u8g2_current->sendBuffer();					// transfer internal memory to the display

}

void splitScreen()
{
  String localMusicTitle, localMusicArtist, localNetworkType;
  if(xSemaphoreTake(dataMutex, pdMS_TO_TICKS(50)) == pdTRUE){
    localMusicTitle = music_title;
    localMusicArtist = music_artist;
    localNetworkType = network_type;
    xSemaphoreGive(dataMutex);
  }

  setLeftSide();
  u8g2_current->clearBuffer();					// clear the internal memory
  drawUiClock();
  drawUI_left();    
  drawBattery(3,2,batteryIconSelection);
  drawMusicUI(localMusicTitle, localMusicArtist, music_timer,music_length,playstate,sEEPROM.screen);
  //u8g2_current->setFont(u8g2_font_profont17_tf); //fix temp location, keine ahnung wieso...

  drawPopups();
  u8g2_current->sendBuffer();					
  
  setRightSide();
  u8g2_current->clearBuffer();					// clear the internal memory
  drawTemperature(global_temp);
  drawBLConnect(OLED_WIDTH/2-2,2 ,blConnected);
  drawNetworkStatus(OLED_WIDTH/2-4-32, 2, network_signal,localNetworkType);
  drawUI_right();
  drawMusicUI(localMusicTitle, localMusicArtist, music_timer,music_length,playstate,sEEPROM.screen);

  drawPopups();
  u8g2_current->sendBuffer();


}

void mediaScreen()
{
  String localMusicTitle, localMusicArtist, localNetworkType;
  if(xSemaphoreTake(dataMutex, pdMS_TO_TICKS(50)) == pdTRUE){
    localMusicTitle = music_title;
    localMusicArtist = music_artist;
    localNetworkType = network_type;
    xSemaphoreGive(dataMutex);
  }

  setLeftSide();
  u8g2_current->clearBuffer();					// clear the internal memory
  drawUiClock();
  drawUI_left();    
  drawBattery(3,2,batteryIconSelection);
  drawMusicUI(localMusicTitle, localMusicArtist, music_timer,music_length,playstate,sEEPROM.screen);

  drawPopups();
  u8g2_current->sendBuffer();					// transfer internal memory to the display
  
  setRightSide();
  u8g2_current->clearBuffer();					// clear the internal memory
  drawTemperature(global_temp);
  drawUI_right();
  drawBLConnect(OLED_WIDTH/2-2,2 ,blConnected);
  drawNetworkStatus(OLED_WIDTH/2-4-32, 2, network_signal,localNetworkType);
  drawMusicUI(localMusicTitle, localMusicArtist, music_timer,music_length,playstate,sEEPROM.screen);

  drawPopups();
  u8g2_current->sendBuffer();

}

void drawPopups(){
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
  drawOTAPopup();
  drawResetPopup();
  drawPairingPopup();
}