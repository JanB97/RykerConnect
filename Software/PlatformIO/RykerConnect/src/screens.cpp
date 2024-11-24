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

  setLeftSide();
  u8g2_current->clearBuffer();					// clear the internal memory
  drawUiClock();
  drawUI_left();    
  drawBattery(3,2,batteryIconSelection);
  drawMusicUI(music_title, music_artist, music_timer,music_length,playstate,sEEPROM.screen);
  //u8g2_current->setFont(u8g2_font_profont17_tf); //fix temp location, keine ahnung wieso...

  drawPopups();
  u8g2_current->sendBuffer();					
  
  setRightSide();
  u8g2_current->clearBuffer();					// clear the internal memory
  drawTemperature(global_temp);
  drawBLConnect(OLED_WIDTH/2-2,2 ,blConnected);
  drawNetworkStatus(OLED_WIDTH/2-4-32, 2, network_signal,network_type);
  drawUI_right();
  drawMusicUI(music_title, music_artist, music_timer,music_length,playstate,sEEPROM.screen);

  drawPopups();
  u8g2_current->sendBuffer();


}

void mediaScreen()
{

  setLeftSide();
  u8g2_current->clearBuffer();					// clear the internal memory
  drawUiClock();
  drawUI_left();    
  drawBattery(3,2,batteryIconSelection);
  drawMusicUI(music_title, music_artist, music_timer,music_length,playstate,sEEPROM.screen);

  drawPopups();
  u8g2_current->sendBuffer();					// transfer internal memory to the display
  
  setRightSide();
  u8g2_current->clearBuffer();					// clear the internal memory
  drawTemperature(global_temp);
  drawBLConnect(OLED_WIDTH/2-2,2 ,blConnected);
  drawNetworkStatus(OLED_WIDTH/2-4-32, 2, network_signal,network_type);
  drawUI_right();
  drawMusicUI(music_title, music_artist, music_timer,music_length,playstate,sEEPROM.screen);

  drawPopups();
  u8g2_current->sendBuffer();

}

void drawPopups(){
  drawNotificationPopup(notificationType,notificationTitle,notificationText);
  drawOTAPopup();
  drawResetPopup();
}