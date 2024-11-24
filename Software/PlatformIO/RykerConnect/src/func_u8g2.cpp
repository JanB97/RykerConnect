#include "func_u8g2.h"

void setLeftSide() {
  u8g2_current = &u8g2_0;
  offset = 0;
}
void setRightSide() {
  u8g2_current = &u8g2_1;
  offset = 160;
}

void drawStr(u8g2_uint_t x, u8g2_uint_t y, const char *s) {
  u8g2_current->drawStr(x-offset,y,s);
}

void drawUTF8(u8g2_uint_t x, u8g2_uint_t y, const char *s){
  u8g2_current->drawUTF8(x-offset,y,s);
}

void drawXBMP(u8g2_uint_t x, u8g2_uint_t y, u8g2_uint_t w, u8g2_uint_t h, const uint8_t *bitmap){
  u8g2_current->drawXBMP(x-offset,y,w,h,bitmap);
}

void drawCircle(u8g2_uint_t x0, u8g2_uint_t y0, u8g2_uint_t rad){
  u8g2_current->drawCircle(x0-offset, y0, rad);
}

void drawDisc(u8g2_uint_t x, u8g2_uint_t y, u8g2_uint_t r){
  u8g2_current->drawDisc(x-offset,y,r);
}

void drawTriangle(int16_t x0, int16_t y0, int16_t x1, int16_t y1, int16_t x2, int16_t y2){
  u8g2_current->drawTriangle(x0-offset, y0, x1-offset, y1,x2-offset,y2);
}

void drawBox(u8g2_uint_t x, u8g2_uint_t y, u8g2_uint_t w, u8g2_uint_t h){
  u8g2_current->drawBox(x-offset, y, w, h);
}

void drawRBox(u8g2_uint_t x, u8g2_uint_t y, u8g2_uint_t w, u8g2_uint_t h, u8g2_uint_t r){
  u8g2_current->drawRBox(x-offset, y, w, h, r);
}

void drawRFrame(u8g2_uint_t x, u8g2_uint_t y, u8g2_uint_t w, u8g2_uint_t h, u8g2_uint_t r){
  u8g2_current->drawRFrame(x-offset, y, w, h, r);
}

