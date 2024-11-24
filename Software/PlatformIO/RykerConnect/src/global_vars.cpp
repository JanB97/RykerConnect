#include "global_vars.h"



//U8G2_SSD1322_NHD_256X64_F_4W_HW_SPI u8g2(U8G2_R0, /* cs=*/ 5, /* dc=*/ 4, /* reset=*/ -1);
U8G2_SSD1320_160X132_F_4W_HW_SPI u8g2_0(U8G2_R0, /* cs=*/ 14, /* dc=*/ 13, /* reset=*/ -1); 
U8G2_SSD1320_160X132_F_4W_HW_SPI u8g2_1(U8G2_R2, /* cs=*/ 10, /* dc=*/ 13, /* reset=*/ -1);
//U8G2_SSD1320_160X132_1_4W_SW_SPI u8g2_0(U8G2_R0, /* clock=*/ 12, /* data=*/ 11, /* cs=*/ 10, /* dc=*/ 13, /* reset=*/ -1); 
//U8G2_SSD1320_160X132_1_4W_SW_SPI u8g2_1(U8G2_R0, /* clock=*/ 12, /* data=*/ 11, /* cs=*/ 14, /* dc=*/ 13, /* reset=*/ -1); 

U8G2 *u8g2_current;
u8g2_uint_t offset;


DS3231 RTC;
MCP9808 ts(24);

const int lightSensorPin = 7;

String global_time;
float global_temp;
unsigned long previousMusicMillis = 0;  // will store last time LED was updated
unsigned long previousBatteryIconMillis;
unsigned long previousResetMillis = 0;
unsigned long previousTimeMillis = 0;
unsigned long previousTempMillis = 0;
int8_t batteryIconSelection = 0;

uint16_t resetPin;


int8_t phone_battery_level = 0;
int8_t intercom_battery_level = 0;
bool phone_battery_status = false;
bool intercom_battery_status = false;
const unsigned short musicInterval = 1000;
uint8_t network_signal = 0;
String network_type = "";
bool blConnected = false;
uint8_t screenToDisplay = 0;

#pragma region Firmware Update

bool firmwareUpdateEnabled = 0;
String wifiSSID = "";
String wifiPassword = "";
String wifiIPAddress = "";

#pragma endregion

#pragma region Notification Variables

unsigned long previousNotificationMillis = 0;
bool notificationDisplayed = false;

String notificationType = "";
String notificationTitle = "";
String notificationText = "";

#pragma endregion



#pragma region Music Variables
bool playstate = false;
uint16_t music_timer = 0;
uint16_t music_length = 0;
String music_title = "";
String music_artist = "";

uint8_t scroll_title = 0;
uint8_t scroll_artist = 0;
bool scroll_title_en = false;
bool scroll_artist_en = false;

unsigned long previousScrollTitleMillis = 0;
unsigned short scrollTitleInterval = FIRSTSCROLL_INTERVAL;
unsigned long previousScrollArtistMillis = 0;
unsigned short scrollArtistInterval = FIRSTSCROLL_INTERVAL;
#pragma endregion

#pragma region Settings
uint8_t display_brightness = 128;
uint8_t screen_selection = 0;
bool batteryIconsSelection[2] = {false,false};
bool showIntercomIcon = false;

Preferences prefs;
struct EEPROM_Struct sEEPROM;


#pragma endregion


#ifdef DEBUG
    unsigned long start = 0, end = 0;
#endif