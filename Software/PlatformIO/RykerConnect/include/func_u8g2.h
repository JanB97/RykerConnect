#pragma once
#include "main.h"

void setLeftSide();
void setRightSide();
void drawStr(u8g2_uint_t x, u8g2_uint_t y, const char *s);
void drawUTF8(u8g2_uint_t x, u8g2_uint_t y, const char *s);
void drawXBMP(u8g2_uint_t x, u8g2_uint_t y, u8g2_uint_t w, u8g2_uint_t h, const uint8_t *bitmap);
void drawCircle(u8g2_uint_t x0, u8g2_uint_t y0, u8g2_uint_t rad);
void drawDisc(u8g2_uint_t x, u8g2_uint_t y, u8g2_uint_t r);
void drawTriangle(int16_t x0, int16_t y0, int16_t x1, int16_t y1, int16_t x2, int16_t y2);
void drawBox(u8g2_uint_t x, u8g2_uint_t y, u8g2_uint_t w, u8g2_uint_t h);
void drawRBox(u8g2_uint_t x, u8g2_uint_t y, u8g2_uint_t w, u8g2_uint_t h, u8g2_uint_t r);
void drawRFrame(u8g2_uint_t x, u8g2_uint_t y, u8g2_uint_t w, u8g2_uint_t h, u8g2_uint_t r);