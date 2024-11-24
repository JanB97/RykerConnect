#pragma once
#include <Arduino.h>

void timeCallback(const uint8_t* data, uint8_t size);

void networkCallback(uint16_t bytes);
String convertNetworkType(byte);

void batteryCallback(int16_t value, uint8_t type);
void mediaDataCallback(const uint8_t* data, uint8_t size);
void notificationCallback(String value);

void setDisplayBrightnessCallback(uint8_t brightness);
void handleSettingsCallback(const uint8_t* data, uint8_t size);
void handleFirmwareUpdateCallback(String value);
void handleFirmwareResetCallback(uint16_t value);