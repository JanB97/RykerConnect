#include "main.h"
#include "screens.h"
#include <Preferences.h>
#include <esp_task_wdt.h>
#include <SPI.h>
#include <WiFi.h>
#include <WiFiClientSecure.h>
#include <HTTPClient.h>
#include <Update.h>
#include <ESPmDNS.h>
#include <Wire.h>
#include "mcp9808.h"


WiFiServer server(80);

static const char otaPage[] PROGMEM =
  "<!DOCTYPE html><html><body>"
  "<h2>RykerConnect OTA</h2>"
  "<input type=file id=f><button onclick=u()>Update</button><br>"
  "<progress id=p max=100 value=0></progress> <span id=s></span>"
  "<script>function u(){var f=document.getElementById('f').files[0];"
  "if(!f)return;var x=new XMLHttpRequest();x.open('POST','/update');"
  "x.upload.onprogress=function(e){if(e.lengthComputable)"
  "document.getElementById('p').value=Math.round(100*e.loaded/e.total)};"
  "x.onload=function(){document.getElementById('s').textContent="
  "x.status==200?'OK, Rebooting...':'FAILED'};"
  "x.send(f)}</script></body></html>";
bool is_server_first_loop = true;
void handleOTAClient();

bool downloadAndFlashFirmware(const String& url) {
    D_printf("[OTA] Starting download from: %s", url.c_str());

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

    const bool useHttps = url.startsWith("https://");
    WiFiClientSecure secureClient;
    WiFiClient plainClient;
    HTTPClient http;

    bool beginOk = false;
    if(useHttps) {
        secureClient.setInsecure();
        beginOk = http.begin(secureClient, url);
    } else {
        beginOk = http.begin(plainClient, url);
    }

    if(!beginOk) {
        D_println("[OTA] HTTP begin failed");
        return false;
    }

    http.setTimeout(30000);
    http.setFollowRedirects(HTTPC_STRICT_FOLLOW_REDIRECTS);

    int httpCode = http.GET();
    D_printf("[OTA] HTTP response code: %d", httpCode);
    if(httpCode != HTTP_CODE_OK) {
        D_printf("[OTA] HTTP GET failed, code: %d", httpCode);
        http.end();
        return false;
    }

    int contentLength = http.getSize();
    if(contentLength <= 0) {
        D_println("[OTA] Unknown content length, using chunked/stream mode");
        contentLength = UPDATE_SIZE_UNKNOWN;
    }

    if(!Update.begin(contentLength)) {
        D_println("[OTA] Not enough space for OTA");
        Update.printError(Serial);
        http.end();
        return false;
    }

    otaDownloadPercent = 0;
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

    WiFiClient* stream = http.getStreamPtr();
    uint8_t buf[1024];
    int written = 0;
    unsigned long lastDisplayUpdate = millis();
    int8_t lastDisplayedPercent = 0;
    while(http.connected() && (contentLength == UPDATE_SIZE_UNKNOWN || written < contentLength)) {
        size_t available = stream->available();
        if(available) {
            int readBytes = stream->readBytes(buf, min(available, sizeof(buf)));
            if(Update.write(buf, readBytes) != (size_t)readBytes) {
                D_println("[OTA] Write failed!");
                Update.printError(Serial);
                otaDownloadPercent = -1;
                http.end();
                return false;
            }
            written += readBytes;
            if(contentLength > 0) {
                otaDownloadPercent = (int8_t)((written * 100L) / contentLength);
            }
            if(millis() - lastDisplayUpdate > 500 || otaDownloadPercent - lastDisplayedPercent >= 5) {
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
                lastDisplayedPercent = otaDownloadPercent;
            }
        }
        esp_task_wdt_reset();
        delay(1);
    }
    D_printf("[OTA] Download complete, %d bytes written", written);
    http.end();
    if(Update.end(true)) {
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
  D_begin(460800);

  initDisplays();


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
    currentDisplayContrast = sEEPROM.display_brightness;
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

  // Hardware Watchdog: restart if loop hangs for 30s
  esp_task_wdt_init(30, true);
  esp_task_wdt_add(NULL);

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

  esp_task_wdt_reset();
  runTimers();

  #ifdef DEBUG
  fps_counter++;
  if (millis() - fps_last_ms >= 1000) {
    fps_value = fps_counter;
    fps_counter = 0;
    fps_last_ms = millis();
  }
  #endif

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

  if(dualPassEnabled){
    dualPassFrame = 1 - dualPassFrame;
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
        handleOTAClient();
    }
  }

}


void handleOTAClient(){
    WiFiClient client = server.available();
    if(!client) return;

    unsigned long timeout = millis();
    while(!client.available() && millis() - timeout < 3000) delay(1);
    if(!client.available()) { client.stop(); return; }

    String requestLine = client.readStringUntil('\n');
    requestLine.trim();
    int contentLength = 0;
    while(client.available()) {
        String header = client.readStringUntil('\n');
        header.trim();
        if(header.length() == 0) break;
        if(header.startsWith("Content-Length:")) contentLength = header.substring(16).toInt();
    }

    if(requestLine.startsWith("GET / ")) {
        client.println("HTTP/1.1 200 OK");
        client.println("Content-Type: text/html");
        client.println("Connection: close");
        client.println();
        client.print(otaPage);
    } else if(requestLine.startsWith("POST /update")) {
        bool success = false;
        if(contentLength > 0 && Update.begin(contentLength)) {
            uint8_t buf[1024];
            int written = 0;
            while(written < contentLength) {
                if(client.available()) {
                    int toRead = min((int)sizeof(buf), contentLength - written);
                    int readBytes = client.readBytes(buf, toRead);
                    if(Update.write(buf, readBytes) != (size_t)readBytes) break;
                    written += readBytes;
                    esp_task_wdt_reset();
                } else if(!client.connected()) {
                    break;
                } else {
                    delay(1);
                }
            }
            success = Update.end(true);
        }
        client.println("HTTP/1.1 200 OK");
        client.println("Connection: close");
        client.println();
        client.println(success ? "OK" : "FAIL");
        client.stop();
        if(success) {
            D_println("[OTA] Browser upload success, rebooting...");
            delay(500);
            ESP.restart();
        }
    } else {
        client.println("HTTP/1.1 404 Not Found");
        client.println("Connection: close");
        client.println();
    }
    client.stop();
}