#pragma once
#include "main.h"
#include "images.h"

void drawUI_right(void);
void drawUI_left(void);
void drawBattery(int pos_x, int pos_y, int8_t type = 0);
void drawUiClock();
void drawFullClock();
void drawTemperature(int t);
void drawFullTemperature();
void drawBLConnect(int pos_x, int pos_y, bool c = false);
void drawNetworkIcon(int x, int y, uint8_t value);
void drawNetworkStatus(int x, int y, uint8_t signal, String type);
void drawOTAPopup();
void drawResetPopup();
void drawNotificationPopup(String type,String title, String text);
void drawPlay(int pos_x, int pos_y);
void drawPause(int pos_x, int pos_y);
void drawSeekbar(int pos_x, int pos_y, int w, int time, int song_length);
String strTime(int seconds);
String getScrollString(uint8_t pos, String value, bool *en, uint8_t *i, unsigned short *interval);

void drawMusicUI(String title, String artist, int time, int song_length, bool playstate, uint8_t screen = 0);
void drawSpeedSign(int16_t speed);
void drawWeatherSymbol(u8g2_uint_t x, u8g2_uint_t y, uint8_t symbol);
void drawWeather(uint8_t symbol, int degree);