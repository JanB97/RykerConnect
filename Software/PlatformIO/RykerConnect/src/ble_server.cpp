#include "ble_server.h"
#include "ble_functions.h"
#include <Arduino.h>

class ServerCallbacks : public NimBLEServerCallbacks
{
    void onConnect(NimBLEServer *pServer, ble_gap_conn_desc *desc)
    {

        D_println("");
        D_println(" *********** onConnect      ************");
        D_println("");
        D_printf(" Client connected:: %s\n", NimBLEAddress(desc->peer_ota_addr).toString().c_str());
        D_printf(" onConnect Core: %i", xPortGetCoreID());
        D_println("");
        D_println(" ***************************************");
        D_println("");

        blConnected = true;
        screenToDisplay = sEEPROM.screen;
        //NimBLEDevice::startAdvertising(); //Multiconnect
    };

    void onDisconnect(NimBLEServer *pServer, ble_gap_conn_desc *desc)
    {

        D_println("");
        D_println(" *********** onDisconnect   ************");
        D_println("");
        D_printf(" Client disconnected:: %s\n", NimBLEAddress(desc->peer_ota_addr).toString().c_str());
        D_printf(" onDisconnect Core: %i", xPortGetCoreID());
        D_println("");
        D_println(" ***************************************");
        D_println("");

        blConnected = false;
        NimBLEDevice::startAdvertising();
    };
};

class BLECharCallbacks : public NimBLECharacteristicCallbacks
{
    void onWrite(NimBLECharacteristic *pCharacteristic)
    {

        String char_uuid = pCharacteristic->getUUID().toString().c_str();

        D_println("");
        D_println(" *********** Characteristic ************");
        D_println("");
        D_printf(" Char Callback Core: %i", xPortGetCoreID());
        D_println("");
        D_print(" ");
        D_printlnV(char_uuid);

        if (char_uuid == TIME_UUID)
        { // ZEIT
            timeCallback(pCharacteristic->getValue().data(), pCharacteristic->getDataLength());
        }
        else if (char_uuid == NETWORK_UUID)
        { // NETZWERK
            networkCallback(pCharacteristic->getValue<uint16_t>());
        }
        else if (char_uuid == PHONE_BATTERY_UUID)
        {
            batteryCallback(pCharacteristic->getValue<int16_t>(), 0);
        }
        else if (char_uuid == INTERCOM_BATTERY_UUID)
        {
            batteryCallback(pCharacteristic->getValue<int8_t>(), 1);
        }
        else if (char_uuid == MEDIA_DATA_UUID)
        {
            mediaDataCallback(pCharacteristic->getValue().data(), pCharacteristic->getDataLength());
        }
        else if (char_uuid == NOTIFICATION_UUID)
        {
            notificationCallback(pCharacteristic->getValue());
        }
        else if (char_uuid == DISPLAY_BRIGHTNESS_UUID)
        {
            setDisplayBrightnessCallback(pCharacteristic->getValue<uint8_t>());
        }
        else if (char_uuid == SCREEN_UUID)
        {
            sEEPROM.screen = pCharacteristic->getValue<uint8_t>();
            screenToDisplay = sEEPROM.screen;
        }
        else if (char_uuid == SETTINGS_UUID){
            D_println(" Settings UUID");
            handleSettingsCallback(pCharacteristic->getValue().data(), pCharacteristic->getDataLength());            
        }
        else if (char_uuid == FIRMWARE_UPDATE_UUID){
            
            handleFirmwareUpdateCallback(pCharacteristic->getValue());
        }
        else if (char_uuid == FIRMWARE_RESET_UUID){
            handleFirmwareResetCallback(pCharacteristic->getValue<uint16_t>());
        }
        else
        {
            D_printf("UUID %s not matching or not implemented!", char_uuid);
            String value = pCharacteristic->getValue();
            if (value.length() > 0)
            {
                D_print(" New value: ");
                for (int i = 0; i < value.length(); i++)
                    D_print(value[i]);
            }
        }
        D_println("");
        D_println(" *************************************");
        D_println("");
    }
    void onRead(NimBLECharacteristic *pCharacteristic)
    {
        String char_uuid = pCharacteristic->getUUID().toString().c_str();
        if (char_uuid == TIME_UUID)
        {
            D_printf(" Read TIME: %s", pCharacteristic->getValue().c_str());
           // String clock = (String)hour + ":" + (String)minute;
            pCharacteristic->setValue("Set Time Char");
            //D_printf(" Clock: %s", clock.c_str());
            D_printf(" Read TIME: %s \n", pCharacteristic->getValue().c_str());
        }else if(char_uuid == SETTINGS_UUID){
            D_println(" Read Settings");
            pCharacteristic->setValue(sEEPROM);
        }
    }
};

void setupBLEServer()
{
    D_printf("Start Task Core: %i", xPortGetCoreID());
    D_printf("Start Task: %i Micros", micros());
#ifdef DEBUG
    start = micros();
#endif

    NimBLEDevice::init("RykerConnect-MainUnit");

    NimBLEDevice::setSecurityAuth(true, true, true);
    NimBLEDevice::setSecurityPasskey(123456);
    NimBLEDevice::setSecurityIOCap(BLE_HS_IO_DISPLAY_YESNO);
    NimBLEServer *pServer = NimBLEDevice::createServer(); 
    pServer = NimBLEDevice::createServer();
    pServer->setCallbacks(new ServerCallbacks());

    /** Optional: set the transmit power, default is 3db */
    // Other Options ESP_PWR_LVL_N9 (+9), ESP_PWR_LVL_P6 (+6), ESP_PWR_LVL_P3 (+3), ESP_PWR_LVL_N0, ESP_PWR_LVL_N3 (-3), ESP_PWR_LVL_N6 (-6), ESP_PWR_LVL_N9 (-9), ESP_PWR_LVL_N12 (-12)
    #ifdef ESP_PLATFORM
        NimBLEDevice::setPower(ESP_PWR_LVL_P9); /** -12db */
    #else
            NimBLEDevice::setPower(-12); /** -12db */
    #endif

    // region HID Setup
    /* HID */
    NimBLEHIDDevice *hid = new NimBLEHIDDevice(pServer);
    // input = hid->inputReport(1); // <-- input REPORTID from report map
    // output = hid->outputReport(1); // <-- output REPORTID from report map

    // output->setCallbacks(new MyCharCallbacks());

    hid->manufacturer()->setValue((std::string) "Jan Boerschlein");

    //hid->pnp(0x02, 0xe502, 0xa111, 0x0210);
    hid->pnp(0x00, 0x00, 0x0100, (VERSION << 8) | ((VERSION >> 8) & 0xFF));
    hid->hidInfo(0x00, 0x02);

    const uint8_t report[] = {
        USAGE_PAGE(1), 0x01, // Generic Desktop Ctrls
        USAGE(1), 0x02,      // Keyboard
        COLLECTION(1), 0x01, // Application
        REPORT_ID(1), 0x01,  //   Report ID (1)
        USAGE_PAGE(1), 0x09, //   Kbrd/Keypad
        USAGE_MINIMUM(1), 0xE0,
        USAGE_MAXIMUM(1), 0xE7,
        LOGICAL_MINIMUM(1), 0x00,
        LOGICAL_MAXIMUM(1), 0x01,
        REPORT_SIZE(1), 0x01, //   1 byte (Modifier)
        REPORT_COUNT(1), 0x08,
        HIDINPUT(1), 0x02,     //   Data,Var,Abs,No Wrap,Linear,Preferred State,No Null Position
        REPORT_COUNT(1), 0x01, //   1 byte (Reserved)
        REPORT_SIZE(1), 0x08,
        HIDINPUT(1), 0x01,     //   Const,Array,Abs,No Wrap,Linear,Preferred State,No Null Position
        REPORT_COUNT(1), 0x06, //   6 bytes (Keys)
        REPORT_SIZE(1), 0x08,
        LOGICAL_MINIMUM(1), 0x00,
        LOGICAL_MAXIMUM(1), 0x65, //   101 keys
        USAGE_MINIMUM(1), 0x00,
        USAGE_MAXIMUM(1), 0x65,
        HIDINPUT(1), 0x00,     //   Data,Array,Abs,No Wrap,Linear,Preferred State,No Null Position
        REPORT_COUNT(1), 0x05, //   5 bits (Num lock, Caps lock, Scroll lock, Compose, Kana)
        REPORT_SIZE(1), 0x01,
        USAGE_PAGE(1), 0x08,    //   LEDs
        USAGE_MINIMUM(1), 0x01, //   Num Lock
        USAGE_MAXIMUM(1), 0x05, //   Kana
        HIDOUTPUT(1), 0x02,     //   Data,Var,Abs,No Wrap,Linear,Preferred State,No Null Position,Non-volatile
        REPORT_COUNT(1), 0x01,  //   3 bits (Padding)
        REPORT_SIZE(1), 0x03,
        HIDOUTPUT(1), 0x01, //   Const,Array,Abs,No Wrap,Linear,Preferred State,No Null Position,Non-volatile
        END_COLLECTION(0)};
    hid->reportMap((uint8_t *)report, sizeof(report));

    /**/
    // endregion

    NimBLEService *pService = pServer->createService(SERVICE_UUID);

    NimBLECharacteristic *timeCharacteristic = pService->createCharacteristic(TIME_UUID, NIMBLE_PROPERTY::READ | NIMBLE_PROPERTY::WRITE);
    NimBLECharacteristic *networkCharacteristic = pService->createCharacteristic(NETWORK_UUID, NIMBLE_PROPERTY::WRITE);
    NimBLECharacteristic *phoneBatteryCharacteristic = pService->createCharacteristic(PHONE_BATTERY_UUID, NIMBLE_PROPERTY::WRITE);
    NimBLECharacteristic *intercomBatteryCharacteristic = pService->createCharacteristic(INTERCOM_BATTERY_UUID, NIMBLE_PROPERTY::WRITE);
    NimBLECharacteristic *mediaDataCharacteristic = pService->createCharacteristic(MEDIA_DATA_UUID, NIMBLE_PROPERTY::WRITE);
    NimBLECharacteristic *notificationCharacteristic = pService->createCharacteristic(NOTIFICATION_UUID, NIMBLE_PROPERTY::WRITE | NIMBLE_PROPERTY::WRITE_ENC | NIMBLE_PROPERTY::WRITE_AUTHEN);

    NimBLECharacteristic *brightnessCharacteristic = pService->createCharacteristic(DISPLAY_BRIGHTNESS_UUID, NIMBLE_PROPERTY::READ | NIMBLE_PROPERTY::WRITE);
    NimBLECharacteristic *screenCharacteristic = pService->createCharacteristic(SCREEN_UUID, NIMBLE_PROPERTY::READ | NIMBLE_PROPERTY::WRITE);
    NimBLECharacteristic *settingsCharacteristic = pService->createCharacteristic(SETTINGS_UUID, NIMBLE_PROPERTY::READ | NIMBLE_PROPERTY::READ_ENC | NIMBLE_PROPERTY::READ_AUTHEN | NIMBLE_PROPERTY::WRITE | NIMBLE_PROPERTY::WRITE_ENC | NIMBLE_PROPERTY::WRITE_AUTHEN);
    NimBLECharacteristic *firmwareUpdateCharacteristic = pService->createCharacteristic(FIRMWARE_UPDATE_UUID, NIMBLE_PROPERTY::WRITE | NIMBLE_PROPERTY::WRITE_ENC | NIMBLE_PROPERTY::WRITE_AUTHEN);
    NimBLECharacteristic *firmwareResetCharacteristic = pService->createCharacteristic(FIRMWARE_RESET_UUID, NIMBLE_PROPERTY::WRITE | NIMBLE_PROPERTY::WRITE_AUTHEN);

    timeCharacteristic->setCallbacks(new BLECharCallbacks());
    networkCharacteristic->setCallbacks(new BLECharCallbacks());
    phoneBatteryCharacteristic->setCallbacks(new BLECharCallbacks());
    intercomBatteryCharacteristic->setCallbacks(new BLECharCallbacks());
    mediaDataCharacteristic->setCallbacks(new BLECharCallbacks());
    notificationCharacteristic->setCallbacks(new BLECharCallbacks());

    brightnessCharacteristic->setCallbacks(new BLECharCallbacks());
    screenCharacteristic->setCallbacks(new BLECharCallbacks());
    settingsCharacteristic->setCallbacks(new BLECharCallbacks());
    firmwareUpdateCharacteristic->setCallbacks(new BLECharCallbacks());
    firmwareResetCharacteristic->setCallbacks(new BLECharCallbacks());

    brightnessCharacteristic->setValue(sEEPROM.display_brightness);
    timeCharacteristic->setValue("");

    pService->start();
    hid->startServices();

    NimBLEAdvertising *pAdvertising = NimBLEDevice::getAdvertising();
    pAdvertising->setAppearance(0x0180);
    // pAdvertising->addServiceUUID((uint16_t)0x1849);
    pAdvertising->addServiceUUID(SERVICE_UUID);
    pAdvertising->setScanResponse(true);
    pAdvertising->setMinPreferred(0x06); // functions that help with iPhone connections issue
    pAdvertising->setMaxPreferred(0x12);
    // NimBLEUUID u = hid->hidService()->getUUID();
    pAdvertising->start();
    // hid->setBatteryLevel(100);

#ifdef DEBUG
    end = micros();
#endif
    D_printf("End Task: %i Micros", end);
    D_printf("End Task Core: %i", xPortGetCoreID());
}
