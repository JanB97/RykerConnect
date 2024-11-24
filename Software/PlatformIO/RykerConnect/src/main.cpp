#include "main.h"
#include "screens.h"
#include <Preferences.h>
#include <SPI.h>
#include <WiFi.h>
#include <WiFiClient.h>
#include <WebServer.h>
#include <Update.h>
#include <ESPmDNS.h>
#include <Wire.h>
#include "mcp9808.h"


WebServer server(80);

const char* serverIndex = PROGMEM{R"rawliteral(<head><title>RykerConnect OTA-Update</title><style>table{border:1px;border-radius:24px;padding:16px;box-shadow:3px 2px 2px 0 rgba(50,50,50,.25)}td,th{padding:6px}input{padding:6px;border-radius:8px;border-color:#c9c9c9}</style></head><body style="font-family:Courier New,Courier,Arial"><form method="POST" action="#" enctype="multipart/form-data" id="upload_form"><table bgcolor="D9D9D9" align="center"><tr><td><center><font size="5"><b>RykerConnect OTA-Update</b></font></center><br></td><br><br></tr><tr><td style="padding-top:6px"><font size="5"><input type="file" name="update"></font></td></tr><br><br><tr><td style="padding-top:12px;padding-bottom:2px"><progress style="width:100%;height:24px" id="prg" max="100" value="0"></progress></td></tr><tr><td><font size="5"><input style="width:100%" type="submit" value="Update"></font></td></tr><tr><td><font size="3">Version: %PLACEHOLDER_VERSION%</font></td><br><br></tr></table></form><script src="https://ajax.googleapis.com/ajax/libs/jquery/3.2.1/jquery.min.js"></script><script>$("form").submit(function(e){e.preventDefault();var t=$("#upload_form")[0],n=new FormData(t);$.ajax({url:"/update",type:"POST",data:n,contentType:!1,processData:!1,xhr:function(){var e=new window.XMLHttpRequest;return e.upload.addEventListener("progress",function(e){if(e.lengthComputable){var t=e.loaded/e.total;document.getElementById("prg").value=Math.round(100*t)}},!1),e},success:function(e,t){console.log("success!")},error:function(e,t,n){}})})</script></body>)rawliteral"};
bool is_server_first_loop = true;
void setupWebServer();


void setup() {
  // put your setup code here, to run once:
  D_begin(115200);
  //u8g2_0.begin();
  //u8g2_1.begin(); 
;
  u8g2_0.setBusClock(60000000);
  u8g2_1.setBusClock(60000000);
  u8g2_0.begin();
  u8g2_1.begin();
  u8g2_0.begin();
  u8g2_1.begin();
  u8g2_0.setContrast(128);
  u8g2_1.setContrast(128);
  u8g2_0.enableUTF8Print();
  u8g2_1.enableUTF8Print();


  #ifdef SPLASHSCREEN
  setLeftSide();
  u8g2_current->clearBuffer();					// clear the internal memory
  drawXBMP(OLED_WIDTH/2-canam_width/2, OLED_HEIGHT/2-canam_height/2,canam_width,canam_height, canam_bits);
  u8g2_current->sendBuffer();					// transfer internal memory to the display
  
  setRightSide();
  drawXBMP(OLED_WIDTH/2-canam_width/2, OLED_HEIGHT/2-canam_height/2,canam_width,canam_height, canam_bits);
  u8g2_current->sendBuffer();					// transfer internal memory to the display
  #endif

  D_delay(1000);

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
    global_temp = ts.getTemperature();
  }else{
    global_temp = RTC.getTemperature();
  }


  D_println(""); D_println("");

  D_println("############## BOOTUP ##################");
  D_println("############## START SETUP #############");
  D_print("Start Setup Core: ");
  D_println(xPortGetCoreID());

  setupBLEServer();

  setupWebServer();

  srand(millis());
  resetPin = 1000 + (rand() % 9999);

  //D_delay(1000);
  
  #ifdef DEBUG
  long delta = end-start;
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