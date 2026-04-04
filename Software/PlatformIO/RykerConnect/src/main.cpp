#include "main.h"
#include "screens.h"
#include <Preferences.h>
#include <SPI.h>
#include <WiFi.h>
#include <WiFiClient.h>
#include <WebServer.h>
#include <Update.h>
#include <ESPmDNS.h>
#include <HTTPClient.h>
#include <WiFiClientSecure.h>
#include <Wire.h>
#include "mcp9808.h"


WebServer server(80);

const char* serverIndex = PROGMEM{R"rawliteral(<head><title>RykerConnect OTA-Update</title><style>table{border:1px;border-radius:24px;padding:16px;box-shadow:3px 2px 2px 0 rgba(50,50,50,.25)}td,th{padding:6px}input{padding:6px;border-radius:8px;border-color:#c9c9c9}</style></head><body style="font-family:Courier New,Courier,Arial"><form method="POST" action="#" enctype="multipart/form-data" id="upload_form"><table bgcolor="D9D9D9" align="center"><tr><td><center><font size="5"><b>RykerConnect OTA-Update</b></font></center><br></td><br><br></tr><tr><td style="padding-top:6px"><font size="5"><input type="file" name="update"></font></td></tr><br><br><tr><td style="padding-top:12px;padding-bottom:2px"><progress style="width:100%;height:24px" id="prg" max="100" value="0"></progress></td></tr><tr><td><font size="5"><input style="width:100%" type="submit" value="Update"></font></td></tr><tr><td><font size="3">Version: %PLACEHOLDER_VERSION%</font></td><br><br></tr></table></form><script src="https://ajax.googleapis.com/ajax/libs/jquery/3.2.1/jquery.min.js"></script><script>$("form").submit(function(e){e.preventDefault();var t=$("#upload_form")[0],n=new FormData(t);$.ajax({url:"/update",type:"POST",data:n,contentType:!1,processData:!1,xhr:function(){var e=new window.XMLHttpRequest;return e.upload.addEventListener("progress",function(e){if(e.lengthComputable){var t=e.loaded/e.total;document.getElementById("prg").value=Math.round(100*t)}},!1),e},success:function(e,t){console.log("success!")},error:function(e,t,n){}})})</script></body>)rawliteral"};
bool is_server_first_loop = true;
void setupWebServer();

bool downloadAndFlashFirmware(const String& url) {
    D_printf("[OTA] Starting download from: %s", url.c_str());
    
    WiFiClientSecure secureClient;
    secureClient.setInsecure();
    D_println("[OTA] WiFiClientSecure created, setInsecure() done");
    
    HTTPClient http;
    http.begin(secureClient, url);
    http.setTimeout(30000);
    http.setFollowRedirects(HTTPC_STRICT_FOLLOW_REDIRECTS);
    D_println("[OTA] HTTPClient configured, sending GET...");
    
    int httpCode = http.GET();
    D_printf("[OTA] HTTP response code: %d", httpCode);
    
    if (httpCode != HTTP_CODE_OK) {
        D_printf("[OTA] HTTP GET failed, code: %d", httpCode);
        http.end();
        return false;
    }
    int contentLength = http.getSize();
    D_printf("[OTA] Content-Length: %d", contentLength);
    
    if (contentLength <= 0) {
        D_println("[OTA] Invalid content length, trying chunked download");
        contentLength = UPDATE_SIZE_UNKNOWN;
    }
    
    D_printf("[OTA] Starting Update.begin(%d)", contentLength);
    if (!Update.begin(contentLength)) {
        D_println("[OTA] Not enough space for OTA");
        Update.printError(Serial);
        http.end();
        return false;
    }
    D_println("[OTA] Update.begin() OK, starting download...");
    otaDownloadPercent = 0;
    
    WiFiClient* stream = http.getStreamPtr();
    uint8_t buf[1024];
    int written = 0;
    unsigned long lastProgressLog = 0;
    unsigned long lastDisplayUpdate = 0;
    while (http.connected() && (contentLength == UPDATE_SIZE_UNKNOWN || written < contentLength)) {
        size_t available = stream->available();
        if (available) {
            int readBytes = stream->readBytes(buf, min(available, sizeof(buf)));
            if (Update.write(buf, readBytes) != (size_t)readBytes) {
                D_println("[OTA] Write failed!");
                Update.printError(Serial);
                otaDownloadPercent = -1;
                http.end();
                return false;
            }
            written += readBytes;
            if (contentLength > 0) {
                otaDownloadPercent = (int8_t)((written * 100L) / contentLength);
            }
            if (millis() - lastProgressLog > 1000) {
                D_printf("[OTA] Progress: %d bytes written", written);
                lastProgressLog = millis();
            }
            // Refresh display every 500ms
            if (millis() - lastDisplayUpdate > 500) {
                setLeftSide();
                u8g2_current->clearBuffer();
                drawFullClock();
                drawOTAPopup();
                u8g2_current->sendBuffer();
                setRightSide();
                u8g2_current->clearBuffer();
                drawFullTemperature();
                drawOTAPopup();
                u8g2_current->sendBuffer();
                lastDisplayUpdate = millis();
            }
        }
        delay(1);
    }
    D_printf("[OTA] Download complete, %d bytes written", written);
    http.end();
    if (Update.end(true)) {
        otaDownloadPercent = 100;
        D_printf("[OTA] Success! %d bytes. Rebooting...", written);
        delay(500);
        ESP.restart();
        return true;
    } else {
        D_println("[OTA] Finalize failed!");
        Update.printError(Serial);
        otaDownloadPercent = -1;
        return false;
    }
}


void setup() {
  // put your setup code here, to run once:
  D_begin(115200);

  // Ensure both CS pins start HIGH (deasserted) before SPI init.
  // After a soft-reset (ESP.restart) GPIO state is preserved, which can
  // leave one CS line stuck LOW and prevent that display from initialising.
  pinMode(14, OUTPUT); digitalWrite(14, HIGH);
  pinMode(10, OUTPUT); digitalWrite(10, HIGH);
  delay(50);

  u8g2_0.setBusClock(60000000);
  u8g2_1.setBusClock(60000000);
  u8g2_0.begin();
  u8g2_1.begin();
  delay(50);
  u8g2_0.begin();
  u8g2_1.begin();
  u8g2_0.setContrast(128);
  u8g2_1.setContrast(128);
  u8g2_0.enableUTF8Print();
  u8g2_1.enableUTF8Print();


  #ifdef SPLASHSCREEN
  setLeftSide();
  u8g2_current->clearBuffer();
  drawXBMP(OLED_WIDTH/2-canam_width/2, OLED_HEIGHT/2-canam_height/2,canam_width,canam_height, canam_bits);
  u8g2_current->setFont(u8g2_font_profont12_tf);
  char versionStr[16];
  snprintf(versionStr, sizeof(versionStr), "v%d.%d", (VERSION >> 8) & 0xFF, VERSION & 0xFF);
  drawStr(2, OLED_HEIGHT - 4, versionStr);
  u8g2_current->sendBuffer();
  
  setRightSide();
  u8g2_current->clearBuffer();
  drawXBMP(OLED_WIDTH/2-canam_width/2, OLED_HEIGHT/2-canam_height/2,canam_width,canam_height, canam_bits);
  u8g2_current->sendBuffer();
  #endif

  D_delay(3000); // Display is already running, wait for USB CDC to enumerate

  Wire.begin(8,18);
  D_println();


  prefs.begin("settings");
  prefs.getBytes("settings", &sEEPROM, sizeof(EEPROM_Struct));
  if(!valiade_eeprom()){
    reset_settings();
  }
  u8g2_0.setContrast(sEEPROM.display_brightness);
  u8g2_1.setContrast(sEEPROM.display_brightness);
  batteryIconSelection = sEEPROM.battery_icon_first;
  
  bool h12Flag;
  bool pmFlag;
  global_time = timeString(RTC.getHour(h12Flag, pmFlag)) + ":" + timeString(RTC.getMinute());

  if(ts.isConnected()){
    ts.setResolution(1);
    global_temp = ts.getTemperature() - sEEPROM.temp_calibration;
  }else{
    global_temp = RTC.getTemperature() - sEEPROM.temp_calibration;
  }


  D_println(""); D_println("");

  D_println("############## BOOTUP ##################");
  D_println("############## START SETUP #############");
  D_print("Start Setup Core: ");
  D_println(xPortGetCoreID());

  dataMutex = xSemaphoreCreateMutex();

  setupBLEServer();

  setupWebServer();

  srand(millis());
  resetPin = 1000 + (rand() % 9999);

  //D_delay(1000);
  
  #ifdef DEBUG
  long delta = dbg_end-dbg_start;
  #endif

  D_println("");
  D_printf("Delta: %i Micros", delta);
  D_printf("Delta: %i Millis", delta/1000);
  D_printf("Delta: %.2f Seconds", (float)delta/1000000);
  D_println("");
  D_printf("End Setup Core: %i", xPortGetCoreID());
  D_println("############## End   SETUP #############");
  D_println("");

}



void loop() {

  runTimers();

  switch(screenToDisplay){
    case 1:
        mediaScreen();
        break;
    case 2:
        splitScreen();
        break;
    default:
        defaultScreen();
  }

  //int lightSensorReading = 0;
  //lightSensorReading = analogRead(lightSensorPin);
  //Serial.println(lightSensorReading);


  if(firmwareUpdateEnabled){
    if(WiFi.status() == WL_CONNECTED){
        if(is_server_first_loop){
          D_println(" Wifi Connected...");
          // Try URL download first if URL was provided
          if(firmwareDownloadUrl.length() > 0 && !firmwareDownloadAttempted){
            firmwareDownloadAttempted = true;
            D_println("[OTA] WiFi connected, attempting firmware download from URL...");
            if(!downloadAndFlashFirmware(firmwareDownloadUrl)){
              D_println("[OTA] Download failed, falling back to browser OTA");
            }
            // If download succeeded, ESP restarts and we never reach here
          }
          if (!MDNS.begin(WIFI_HOSTNAME)) { //http://RykerConnect.local
              D_println("Error setting up MDNS responder!");
          }
          D_printf(" Hostname: RykerConnect.local\n Wifi Address: %s",WiFi.localIP().toString());
          server.begin();
          is_server_first_loop = false;
        }
        server.handleClient();
    }
  }

}


void setupWebServer(){
  server.on("/", HTTP_GET, []() {
    String serverT = serverIndex;
    serverT.replace("%PLACEHOLDER_VERSION%", String(VERSION));
    server.sendHeader("Connection", "close");
    server.send(200, "text/html", serverT);
  });
  /*handling uploading firmware file */
  server.on("/update", HTTP_POST, []() {
    server.sendHeader("Connection", "close");
    server.send(200, "text/plain", (Update.hasError()) ? "FAIL" : "OK");
    ESP.restart();
  }, []() {
    HTTPUpload& upload = server.upload();
    if (upload.status == UPLOAD_FILE_START) {
      D_printf("Update: %s\n", upload.filename.c_str());
      if (!Update.begin(UPDATE_SIZE_UNKNOWN)) { //start with max available size
        Update.printError(Serial);
      }
    } else if (upload.status == UPLOAD_FILE_WRITE) {
      /* flashing firmware to ESP*/
      if (Update.write(upload.buf, upload.currentSize) != upload.currentSize) {
        Update.printError(Serial);
      }
    } else if (upload.status == UPLOAD_FILE_END) {
      if (Update.end(true)) { //true to set the size to the current progress
        D_printf("Update Success: %u\nRebooting...\n", upload.totalSize);
      } else {
        Update.printError(Serial);
      }
    }
  });
}