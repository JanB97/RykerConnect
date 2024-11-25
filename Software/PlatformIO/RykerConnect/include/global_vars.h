#pragma once

#include <Arduino.h>
#include <U8g2lib.h>
#include <SPI.h>
#include <DS3231.h>
#include <Preferences.h>
#include <mcp9808.h>

//#define DEBUG
#define SPLASHSCREEN

#define VERSION 0x0001
#define OLED_WIDTH 320
#define OLED_HEIGHT 132

#define FIRSTSCROLL_INTERVAL 5000
#define SCROLL_INTERVAL 800
#define RESET_INTERVAL 45000
#define TIME_INTERVAL 1000
#define TEMP_INTERVAL 30000


#define SUN	0
#define SUN_CLOUD  1
#define CLOUD 2
#define RAIN 3
#define THUNDER 4


#pragma region BLE UUIDS

#define SERVICE_UUID "db7ba582-229a-4b96-9000-cf0f69f86f73"

#define TIME_UUID "a41dcc81-d45e-4445-99bb-38c37c1ef1c8"
#define NETWORK_UUID "49c7fba8-9ba7-474b-b8a5-a5431e057e23"
#define PHONE_BATTERY_UUID "1f74ccf5-376a-40b6-ab60-7b1c5efbf652"
#define INTERCOM_BATTERY_UUID "85546838-6ae5-45cb-aa2f-4c8af50d17d4"
#define MEDIA_DATA_UUID "dcadc0d8-24ed-40ed-952b-5d1c872a69aa"
#define NOTIFICATION_UUID "755cf5b1-ded3-4c7b-a6fc-8c5ce2f99fdb"


#define SCREEN_UUID "62dbb02d-4a3a-452e-b753-02bcb2272b9d"
#define DISPLAY_BRIGHTNESS_UUID "7bc28f30-10bc-46e2-b84b-96e0545c2f5c"
#define SETTINGS_UUID "05f7c3e4-daac-4953-8c71-20eacdf0c7a1"
#define FIRMWARE_UPDATE_UUID "1d1306c5-98d9-4998-8dfd-35136295575f"
#define FIRMWARE_RESET_UUID "18cb54fe-45e8-4819-a262-24b731c8b236"

#pragma endregion  


//U8G2_SSD1322_NHD_256X64_F_4W_HW_SPI u8g2(U8G2_R0, /* cs=*/ 5, /* dc=*/ 4, /* reset=*/ -1);
extern U8G2_SSD1320_160X132_F_4W_HW_SPI u8g2_0; 
extern U8G2_SSD1320_160X132_F_4W_HW_SPI u8g2_1; 
extern U8G2 *u8g2_current;
extern u8g2_uint_t offset;


extern DS3231 RTC;
extern MCP9808 ts;
extern const int lightSensorPin;
extern String global_time;
extern float global_temp;
extern unsigned long previousMusicMillis;  // will store last time LED was updated
extern unsigned long previousBatteryIconMillis; //Change Battery Icon
extern unsigned long previousResetMillis; //Change Battery Icon
extern unsigned long previousTimeMillis;
extern unsigned long previousTempMillis;
extern int8_t batteryIconSelection;

extern uint16_t resetPin;


extern int8_t phone_battery_level;
extern int8_t intercom_battery_level;
extern bool phone_battery_status;
extern bool intercom_battery_status;
extern const unsigned short musicInterval;
extern uint8_t network_signal;
extern String network_type;
extern bool blConnected;
extern uint8_t screenToDisplay;


#pragma region Firmware Update

#define WIFI_HOSTNAME "RykerConnect"
extern bool firmwareUpdateEnabled;
extern String wifiIPAddress;

#pragma endregion

#pragma region Notification Variables

extern unsigned long previousNotificationMillis;
extern bool notificationDisplayed;

extern String notificationType;
extern String notificationTitle;
extern String notificationText;

#pragma endregion

#pragma region Music Variables
extern bool playstate;
extern uint16_t music_timer;
extern uint16_t music_length;
extern String music_title;
extern String music_artist;

extern uint8_t scroll_title;
extern uint8_t scroll_artist;
extern bool scroll_title_en;
extern bool scroll_artist_en;
extern unsigned long previousScrollTitleMillis;
extern unsigned short scrollTitleInterval;
extern unsigned long previousScrollArtistMillis;
extern unsigned short scrollArtistInterval;
#pragma endregion

#pragma region Settings

#define ADAPTIVE_BRIGHTNESS         0;
#define DISPLAY_BRIGHTNESS          148;
#define SCREEN_SELECTION            1;
#define SUB_SCREEN_SELECTION        0;
#define BATTERY_ICON_SELECTION1     1;
#define BATTERY_ICON_SELECTION2     1;
#define BATTERY_ICON_SELECTION3     0;
#define BATTERY_ICON_SELECTION4     0;
#define BATTERY_ICON_FIRST          0;
#define BATTERY_ICON_INTERVAL       30000; //Sekunden
#define NOTIFICATION_INTERVAL       20000; //Sekunden

extern Preferences prefs;
extern struct EEPROM_Struct{
    bool        adaptive_brightness;
    uint8_t     display_brightness;
    uint8_t     screen;
    uint8_t     sub_screen;
    bool        battery_icon_selection[4]; //2 bool reserved
    int8_t      battery_icon_first;
    uint32_t    battery_icon_interval;
    uint32_t    notification_interval;
    uint32_t    crc;
}__attribute__((packed)) sEEPROM;


#pragma endregion






#pragma region DEBUG CODE
    #ifdef DEBUG
    #define D_begin(...) Serial.begin(__VA_ARGS__);
    #define D_print(...)    Serial.print(__VA_ARGS__);
    #define D_printf(format, ...)   Serial.printf(format"\n", __VA_ARGS__);
    #define D_printV(x)  Serial.print(F(#x" = ")); Serial.print (x);Serial.print (F(" ")); 
    #define D_println(...)  Serial.println(__VA_ARGS__);
    #define D_printlnV(x)  D_printV(x); Serial.println ();
    #define D_delay(x) delay(x);
    extern unsigned long start, end;
    #else
    #define D_begin(...)
    #define D_print(...)
    #define D_printf(...)
    #define D_printV(x) 
    #define D_println(...)
    #define D_printlnV(x) 
    #define D_delay(x)
    #endif
#pragma endregion