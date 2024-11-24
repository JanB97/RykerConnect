
#include "func_gui.h"
#include <WiFi.h>

#define ABSTAND_MITTE 73




#pragma region STATUSBAR

void drawUI_right(void){
  u8g2_1.drawHLine(OLED_WIDTH/2-50, 20, 50); //Rechts langer Strich
  u8g2_1.drawHLine(OLED_WIDTH/2-49, 18, 5); //Rechts kurzer Strich
  u8g2_1.drawLine(OLED_WIDTH/2-49, 18, OLED_WIDTH/2-63, 4); // Rechts diagonaler Strich innen
  u8g2_1.drawLine(OLED_WIDTH/2-50, 20, OLED_WIDTH/2-62, 8); // Rechts diagonaler Strich außen

  u8g2_1.drawHLine(ABSTAND_MITTE-10, 0, 10);  //Rechts kurzer Strich mitte oben
  u8g2_1.drawLine(ABSTAND_MITTE-10, 0, ABSTAND_MITTE-10-14, 14); //Rechts diagonaler Strich miite
  u8g2_1.drawHLine(0, 14, ABSTAND_MITTE-10-14); //Langer Strich mitte
  u8g2_1.drawHLine(0, 16, ABSTAND_MITTE-10-14+2); //Schatten
  u8g2_1.drawLine(ABSTAND_MITTE-10-14+1, 16, ABSTAND_MITTE-10-14+1+14, 2); //Langer Strich mitte
  /*
  u8g2_1.drawPixel(0,0);
  u8g2_1.drawPixel(0,1);
  u8g2_1.drawPixel(0,2);
  u8g2_1.drawPixel(1,0);
  u8g2_1.drawPixel(1,1);
  u8g2_1.drawPixel(2,0);
  */
}
void drawUI_left(void){
  u8g2_0.drawHLine(0, 20, 50); //Links langer Strich
  u8g2_0.drawHLine(44, 18, 5); //Links kurzer Strich
  u8g2_0.drawLine(49, 18, 63, 4); // Links diagonaler Strich innen
  u8g2_0.drawLine(50, 20, 62, 8); // Links diagonaler Strich außen

  u8g2_0.drawHLine(OLED_WIDTH/2-ABSTAND_MITTE, 0, 10);  //kurzer Strich mitte oben
  u8g2_0.drawLine(OLED_WIDTH/2-ABSTAND_MITTE+10, 0, OLED_WIDTH/2-ABSTAND_MITTE+10+14, 14); //diagonaler Strich miite
  u8g2_0.drawHLine(OLED_WIDTH/2-ABSTAND_MITTE+10+14, 14, OLED_WIDTH/2 - (OLED_WIDTH/2-ABSTAND_MITTE+10+14)); //langer Strich Mitte

  u8g2_0.drawHLine(OLED_WIDTH/2-ABSTAND_MITTE+10+14+8,16, OLED_WIDTH/2- (OLED_WIDTH/2-ABSTAND_MITTE+10+14+8,14)); //Schatten

}
void drawBattery(int pos_x, int pos_y, int8_t type){
  int8_t level = 0;
  bool status = false;
  if(type == 0){
    level = phone_battery_level;
    status = phone_battery_status;
    u8g2_0.drawXBMP(pos_x,pos_y,phone_16_width,phone_16_height,phone_16_bits);
  }else if(type == 1){
    level = intercom_battery_level;
    status = intercom_battery_status;
    u8g2_0.drawXBMP(pos_x,pos_y,headset_16_width,headset_16_height,headset_16_bits);
  }else if(type == -1){
    return;
  }else{
    level = -1;
    u8g2_0.drawXBMP(pos_x,pos_y,unkown_device_16_width,unkown_device_16_height,unkown_device_16_bits);
  }
  u8g2_0.drawBox(13+pos_x+2,pos_y,3,2);
  u8g2_0.drawFrame(13+pos_x,pos_y+2,7,14);
  if(level != -1){
      u8g2_0.drawBox(13+pos_x+1,pos_y+3+(float)12/(float)100*(100-level),5,(float)12/(float)100*level+1);
      if(status){
        u8g2_0.setBitmapMode(1);
        u8g2_0.setDrawColor(2);
        u8g2_0.drawXBM(13+pos_x+1,pos_y+4,charging1_width, charging1_height, charging1_bits);
        u8g2_0.setDrawColor(1);
        u8g2_0.setBitmapMode(0);
      }
      String l = String(level) + "%";
      u8g2_0.setFont(u8g2_font_helvR08_tr);
      u8g2_0.drawStr(13+pos_x+9,pos_y+13, l.c_str());
  }else{
    u8g2_0.drawLine(13+pos_x+3, pos_y+6, 13+pos_x+3, pos_y+9);
    u8g2_0.drawPixel(13+pos_x+3, pos_y+11);
  } 
}

void drawUiClock(){
  u8g2_0.setFont(u8g2_font_crox2c_mf  );
  u8g2_0.drawStr(OLED_WIDTH/2-ABSTAND_MITTE+23, 12, global_time.c_str());
  
}
void drawFullClock(){
  String time_t1 = global_time;
  time_t1.replace('0', 'O');
  u8g2_current->setFont(u8g2_font_spleen16x32_mf);
  u8g2_current->drawUTF8(u8g2_current->getWidth()/2 -u8g2_current->getStrWidth(time_t1.c_str())/2 +5 ,u8g2_current->getHeight()/2+11, time_t1.c_str());
}

void drawFullTemperature(){
  String temp_txt = String(global_temp) + "°C";
  temp_txt.replace('0', 'O');
  u8g2_current->setFont(u8g2_font_spleen16x32_mf);
  u8g2_current->drawUTF8(u8g2_current->getWidth()/2 -u8g2_current->getStrWidth(temp_txt.c_str())/2 +5 ,u8g2_current->getHeight()/2+11, temp_txt.c_str());
}

void drawTemperature(int t){
  u8g2_0.setFont(u8g2_font_profont17_tf);
  u8g2_1.setFont(u8g2_font_crox2c_mf  );
  String temp_txt = String(t) + "°C";
  int temp_width = u8g2_0.getUTF8Width(temp_txt.c_str());
  if(t < 0 ){
    temp_width = temp_width -1;
  }
  u8g2_1.drawUTF8(ABSTAND_MITTE-temp_width-23, 12, temp_txt.c_str());
}
void drawBLConnect(int pos_x, int pos_y, bool c){
  if(c){
    u8g2_1.drawXBMP(pos_x-bluetooth_16_verbunden_width,pos_y,bluetooth_16_verbunden_width,bluetooth_16_verbunden_height,bluetooth_16_verbunden_bits);
  }else{
    u8g2_1.drawXBMP(pos_x-bluetooth_16_width,pos_y,bluetooth_16_width,bluetooth_16_height,bluetooth_16_bits);
  }
}

void drawNetworkIcon(int x, int y, uint8_t value){
  switch (value){
    case 0:
      u8g2_1.drawXBMP(x,y,network_width,network_height, network0_bits);
      break;
    case 1:
      u8g2_1.drawXBMP(x,y,network_width,network_height, network1_bits);
      break;
    case 2:
      u8g2_1.drawXBMP(x,y,network_width,network_height, network2_bits);
      break;
    case 3:
      u8g2_1.drawXBMP(x,y,network_width,network_height, network3_bits);
      break;
    case 4:
      u8g2_1.drawXBMP(x,y,network_width,network_height, network4_bits);
      break;
    case 5:
      u8g2_1.drawXBMP(x,y,network_width,network_height, network5_bits);
      break;
    default:
      u8g2_1.drawXBMP(x,y,network_width,network_height, network0_bits);
  }
}
void drawNetworkStatus(int x, int y, uint8_t signal, String type){
  u8g2_1.setFont(u8g2_font_courR08_tr);
  drawNetworkIcon(x,y,signal);
  u8g2_1.drawStr(x-u8g2_1.getStrWidth(type.c_str())+2,y+9,type.c_str());
}
#pragma endregion


void drawNotificationPopup(String type, String title, String text){

  if(notificationDisplayed){

      unsigned char icon[60];
      String type_low = type;
      type_low.toLowerCase();
      if(type_low.startsWith("whatsapp")){
          memcpy(icon, whatsapp_20_bits, sizeof icon);
      }
      else if(type_low.startsWith("snapchat")){
          memcpy(icon, snapchat_20_bits, sizeof icon);
      }
      else if(type_low.startsWith("instagram")){
          memcpy(icon, instagram_20_bits, sizeof icon);
      }
      else if(type_low.startsWith("discord")){
          memcpy(icon, discord_20_bits, sizeof icon);
      }
      else if(type_low.startsWith("gmail")){
          memcpy(icon, mail_20_bits, sizeof icon);
      }
      else if(type_low.startsWith("facebook")){
          memcpy(icon, facebook_20_bits, sizeof icon);
      }
      else if(type_low.startsWith("messenger")){
          memcpy(icon, messenger_20_bits, sizeof icon);
      }
      else if(type_low.startsWith("messages")){
          memcpy(icon, chat_20_bits, sizeof icon);
      }
      else{
          memcpy(icon, notification_20_bits, sizeof icon);
      }


      u8g2_current->setDrawColor(0);
      drawRBox(14,33,OLED_WIDTH-14*2,OLED_HEIGHT-33-11,8);
      u8g2_current->setDrawColor(1);
      drawXBMP(21, 38, notification_20_width, notification_20_height, icon);
      drawRFrame(14,33,OLED_WIDTH-14*2,OLED_HEIGHT-33-11,8);
      drawRFrame(15,34,OLED_WIDTH-15*2,OLED_HEIGHT-34-12,7);
      u8g2_current->setFont(u8g2_font_profont17_tf);   
      if(u8g2_current->getUTF8Width(type.c_str()) > 250){
          int type_length = type.length()-1;
          while(u8g2_current->getUTF8Width(type.substring(0,type_length).c_str()) > 250){
              type_length--;
          }
          type = type.substring(0, type_length-1) + "\x85";
      }
      drawUTF8(23+notification_20_width+4,37+16,type.c_str());
      u8g2_current->setFont(u8g2_font_profont22_tf);
      if(title.length() > 23){
        int i = 22;
        while(u8g2_current->getUTF8Width(title.substring(0,i).c_str()) < 262){
            i++;
        }
        title = title.substring(0,i);
        title += "\x85";
      }
      drawUTF8(22,79, title.c_str());

      u8g2_current->setFont(u8g2_font_profont15_tf);
      text.replace('\n', ' ');
      if(u8g2_current->getUTF8Width(text.c_str()) > 278){
          String text2 = "";
          uint8_t text1_size = 40;
          while(u8g2_current->getUTF8Width(text.substring(0,text1_size).c_str()) <279){
            text1_size++;
          }
          int delimiter1 = text.lastIndexOf(0x20, text1_size);
          if(u8g2_current->getUTF8Width(text.substring(delimiter1+1, text.length()).c_str()) > 278){
              uint8_t text2_size = 40;
              while(u8g2_current->getUTF8Width(text.substring(delimiter1+1,text2_size).c_str()) <262){
                  text2_size++;
              }
              text2 = text.substring(delimiter1+1, text2_size) + "\x85";
          }else{
              text2 = text.substring(delimiter1+1, text.length());
          }
          drawUTF8(22, 97, text.substring(0, delimiter1).c_str());
          drawUTF8(22, 113, text2.c_str());
      }else{
          drawUTF8(22, 97, text.c_str());
      }
  }
}


void drawOTAPopup(){
    if(firmwareUpdateEnabled){
        if(WiFi.status() != WL_CONNECTED){
            u8g2_current->setDrawColor(0);
            drawRBox(OLED_WIDTH/2-150/2,OLED_HEIGHT-45-4,153,48,24);
            u8g2_current->setDrawColor(1);
            drawRFrame(OLED_WIDTH/2-150/2,OLED_HEIGHT-44-4,150,44,22);
            drawXBMP(OLED_WIDTH/2-150/2+19,OLED_HEIGHT-44-4+5, ota_size, ota_size, ota_16_bits);
            u8g2_current->setFont(u8g2_font_profont17_tf);
            drawStr(OLED_WIDTH/2-(u8g2_current->getStrWidth("OTA Update"))/2+9, OLED_HEIGHT-44-4+18, "OTA Update");
            u8g2_current->setFont(u8g2_font_profont15_tf);
            drawStr(OLED_WIDTH/2-(u8g2_current->getStrWidth("Connecting WiFi\x85"))/2, OLED_HEIGHT-12, "Connecting WiFi\x85");
        }else{
            u8g2_current->setDrawColor(0);
            drawRBox(OLED_WIDTH/2-224/2,OLED_HEIGHT-60-4,227,64,32);
            u8g2_current->setDrawColor(1);
            drawRFrame(OLED_WIDTH/2-224/2,OLED_HEIGHT-60-4,224,60,30);
            drawXBMP(OLED_WIDTH/2-160/2+22,OLED_HEIGHT-60-4+4, ota_size, ota_size, ota_16_bits);
            u8g2_current->setFont(u8g2_font_profont17_tf);
            drawStr(OLED_WIDTH/2-(u8g2_current->getStrWidth("OTA Update"))/2+11, OLED_HEIGHT-60-4+17, "OTA Update");
            u8g2_current->setFont(u8g2_font_profont15_tf);
            String host = String("Hostname: ") + WIFI_HOSTNAME +".local";
            drawStr(OLED_WIDTH/2-(u8g2_current->getStrWidth(host.c_str()))/2, OLED_HEIGHT-60+17+14, host.c_str());
            String ip_address = "IP-Address: " + WiFi.localIP().toString();
            drawStr(OLED_WIDTH/2-(u8g2_current->getStrWidth(ip_address.c_str()))/2, OLED_HEIGHT-11, ip_address.c_str());
        }
    }
}

void drawResetPopup(){
    if(previousResetMillis > 0 ){
        u8g2_current->setDrawColor(0);
        drawRBox(OLED_WIDTH/2-150/2,OLED_HEIGHT-45-4,153,48,24);
        u8g2_current->setDrawColor(1);
        drawRFrame(OLED_WIDTH/2-150/2,OLED_HEIGHT-44-4,150,44,22);
        drawXBMP(OLED_WIDTH/2-150/2+11,OLED_HEIGHT-44-4+4, reset_size, reset_size, reset_16_bits);
        u8g2_current->setFont(u8g2_font_profont17_tf);
        drawStr(OLED_WIDTH/2-(u8g2_current->getStrWidth("Reset Device"))/2+10, OLED_HEIGHT-44-4+18, "Reset Device");
        u8g2_current->setFont(u8g2_font_profont15_tf);
        drawStr(OLED_WIDTH/2-(u8g2_current->getStrWidth(("Pin: " + String(resetPin)).c_str()))/2, OLED_HEIGHT-12, ("Pin: " + String(resetPin)).c_str());
    }    
}


#pragma region MUSICUI
#define btnSize 14
void drawPlay(int pos_x, int pos_y){
  u8g2_0.drawTriangle(pos_x, pos_y, pos_x, pos_y-btnSize, pos_x+btnSize/2, pos_y-btnSize/2);}
void drawPause(int pos_x, int pos_y){
  drawBox(pos_x, pos_y-btnSize, btnSize/3 ,btnSize);
  drawBox(pos_x+(btnSize/3)*2, pos_y-btnSize, btnSize/3 ,btnSize);}
void drawSeekbar(int pos_x, int pos_y, int w, int time, int song_length){
  if(time > song_length){
    time = song_length;
  }
  int x = pos_x+3+(((float)w-7)/(float)song_length*(float)time);
  drawBox(pos_x, pos_y-btnSize+3, w, 2);
  drawCircle(x, pos_y-btnSize+3, 3);
  u8g2_current->setDrawColor(0);
  drawDisc(x, pos_y-btnSize+3, 2);
  u8g2_current->setDrawColor(1);
  drawCircle(x, pos_y-btnSize+3, 1);
  u8g2_current->setFont(u8g2_font_profont10_mn);
  drawStr(pos_x,pos_y,strTime(time).c_str());
  String tmp_time = strTime(song_length);
  drawStr(pos_x+w-u8g2_current->getStrWidth(tmp_time.c_str()),pos_y,tmp_time.c_str());
}
String strTime(int seconds){
  int runHours= seconds/3600;
  int secsRemaining=seconds%3600;
  int runMinutes=secsRemaining/60;
  int runSeconds=secsRemaining%60;
  if(runHours != 0){
    return String(runHours) + ":" + timeString(runMinutes) + ":" + timeString(runSeconds);
  }
  else if(runMinutes != 0){
    return String(runMinutes) + ":" + timeString(runSeconds);
  }
  return "0:" + timeString(seconds);
}
String getScrollString(uint8_t pos, String value, bool *en, uint8_t *i, unsigned short *interval){
  int width_avail = OLED_WIDTH-pos;
  if(u8g2_current->getUTF8Width(value.substring(*i , value.length()).c_str())>width_avail){
    *en = true;
      if(*i == 0){
        *interval = FIRSTSCROLL_INTERVAL;
      }else{
        *interval = SCROLL_INTERVAL;
        //D_println(((String)value[*i]+(String)value[*i+1]));
        if(value.charAt(*i) == 0xC3 && value.charAt(*i+1) == 0xBC) {   // ü == 0xC3BC
          *i = *i +2; // skip over the second byte which is already used
        }
        value = value.substring(*i , value.length());
      }
  }else if(u8g2_current->getUTF8Width(value.substring(*i-1 , value.length()).c_str())>width_avail && *en){
        *interval = FIRSTSCROLL_INTERVAL;
        //D_println(((String)value[*i]+(String)value[*i+1]));
        if(value.charAt(*i) == 0xC3 && value.charAt(*i+1) == 0xBC) {   // ü == 0xC3BC
          *i = *i +2; // skip over the second byte which is already used
        }
        value = value.substring(*i , value.length());
  }else{
    *i = 0;
    *en = false;
  }
  int l = value.length();
  value.trim();
  if(l != value.length()){
    *i = *i +1;
  }
  return value;
}

void drawMusicUI(String title, String artist, int time, int song_length, bool playstate, uint8_t screen){

    uint8_t y = 0;
    uint8_t x = 0;
    switch(screen){
      case 2:
          y = 48;
          x = 90;
          drawXBMP(x, y-musik_20_height, musik_20_width, musik_20_height, musik_20_bits);
          u8g2_current->setFont(u8g2_font_profont12_tf );
          drawStr(x+musik_20_width+10, y-6, "Audio");
          break;
      default:
          y = 34;
          x = 8;
          break;
    }
  
    u8g2_current->setFont(u8g2_font_profont22_tf );
    drawUTF8(x, y+24, getScrollString(x, title, &scroll_title_en, &scroll_title, &scrollTitleInterval).c_str());
    //drawUTF8(x, y+24, "\x85");
    u8g2_current->setFont(u8g2_font_profont15_tf );
    drawUTF8(x, y+44, getScrollString(x, artist, &scroll_artist_en, &scroll_artist, &scrollArtistInterval).c_str());

    if(!(title == "" && artist == "")){
        if(!playstate){
            drawPlay(x,y+74);
        }else{
            drawPause(x, y+74);
        }
        if(song_length>15){
            drawSeekbar(x + btnSize + 4, y+74, OLED_WIDTH-(x + btnSize)-8, time, song_length);
        }else{
            u8g2_current->setFont(u8g2_font_profont12_mn);
            drawStr(x + btnSize + 4+2,y+72,strTime(time).c_str());
        }
    }
}

#pragma endregion



#pragma region ICON_FRONT
void drawSpeedSign(int16_t speed){
  u8g2_current->setFont(u8g2_font_profont17_tr);
  uint8_t x = 32;
  uint8_t y = 56;
  drawDisc(x, y, 20);
  u8g2_current->setDrawColor(0);
  drawDisc(x, y, 16);
  u8g2_current->setDrawColor(1);
  String s = String(speed);
  if(speed > 0){
    drawStr(x-u8g2_current->getStrWidth(String(speed).c_str())/2, y+u8g2_current->getMaxCharHeight()/3+1, String(speed).c_str());
  }else if(speed == 0){
    u8g2_0.drawLine(x-18/2-5, y+14/2, x+18/2+1, y-26/2);
    u8g2_0.drawLine(x-18/2-3, y+20/2, x+18/2+3, y-20/2);
    u8g2_0.drawLine(x-18/2-1, y+26/2, x+18/2+5, y-14/2);
  }else{
    drawStr(x-3, y+u8g2_current->getMaxCharHeight()/3+1, "?");
  }
}
#pragma endregion



void drawWeatherSymbol(u8g2_uint_t x, u8g2_uint_t y, uint8_t symbol)
{
  // fonts used:
  // u8g2_font_open_iconic_embedded_6x_t
  // u8g2_font_open_iconic_weather_6x_t
  // encoding values, see: https://github.com/olikraus/u8g2/wiki/fntgrpiconic
  
  switch(symbol)
  {
    case SUN:
      u8g2_0.setFont(u8g2_font_open_iconic_weather_1x_t);
      u8g2_0.drawGlyph(x, y, 69);	
      break;
    case SUN_CLOUD:
      u8g2_0.setFont(u8g2_font_open_iconic_weather_2x_t);
      u8g2_0.drawGlyph(x, y, 12);	
      break;
    case CLOUD:
      u8g2_0.setFont(u8g2_font_open_iconic_weather_6x_t);
      u8g2_0.drawGlyph(x, y, 12);	
      break;
    case RAIN:
      u8g2_0.setFont(u8g2_font_open_iconic_weather_6x_t);
      u8g2_0.drawGlyph(x, y, 12);	
      break;
    case THUNDER:
      u8g2_0.setFont(u8g2_font_open_iconic_embedded_6x_t);
      u8g2_0.drawGlyph(x, y, 12);
      break;      
  }
}
void drawWeather(uint8_t symbol, int degree)
{
  drawWeatherSymbol(0, 48, symbol);
  u8g2_0.setFont(u8g2_font_logisoso32_tf);
  u8g2_0.setCursor(48+3, 42);
  u8g2_0.print(degree);
  u8g2_0.print("°C");		// requires enableUTF8Print()
}