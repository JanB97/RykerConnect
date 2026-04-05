#include "ble_server.h"
#include "ble_functions.h"
#include <Arduino.h>

class ServerCallbacks : public NimBLEServerCallbacks
{
    void onConnect(NimBLEServer *pServer, NimBLEConnInfo &connInfo) override
    {

        D_println("");
        D_println(" *********** onConnect      ************");
        D_println("");
        D_printf(" Client connected:: %s\n", connInfo.getAddress().toString().c_str());
        D_printf(" onConnect Core: %i", xPortGetCoreID());
        D_println("");
        D_println(" ***************************************");
        D_println("");

        blConnected = true;
        screenToDisplay = sEEPROM.screen;

        // Show pairing PIN if device is not bonded (1000ms delay in draw
        // prevents briefly flashing the PIN when a bonded device reconnects)
        if(!NimBLEDevice::isBonded(connInfo.getAddress())){
            pairingPin = NimBLEDevice::getSecurityPasskey();
            pairingActive = true;
            pairingConnectTime = millis();
            D_printf(" Not bonded - showing pairing PIN: %u", pairingPin);
        }

        // Request fast connection parameters for low latency (7.5ms - 15ms interval)
        pServer->updateConnParams(connInfo.getConnHandle(), 6, 12, 0, 100);
    };

    void onDisconnect(NimBLEServer *pServer, NimBLEConnInfo &connInfo, int reason) override
    {

        D_println("");
        D_println(" *********** onDisconnect   ************");
        D_println("");
        D_printf(" Client disconnected:: %s\n", connInfo.getAddress().toString().c_str());
        D_printf(" onDisconnect Core: %i", xPortGetCoreID());
        D_println("");
        D_println(" ***************************************");
        D_println("");

        blConnected = false;
        pairingActive = false;
        NimBLEDevice::startAdvertising();
    };

    uint32_t onPassKeyDisplay() override {
        D_printf(" Pairing PIN: %u", NimBLEDevice::getSecurityPasskey());
        pairingPin = NimBLEDevice::getSecurityPasskey();
        pairingActive = true;
        return pairingPin;
    };

    void onAuthenticationComplete(NimBLEConnInfo &connInfo) override {
        D_println(" Authentication complete");
        pairingActive = false;
        if(!connInfo.isEncrypted()){
            D_println(" Encryption failed - deleting bond and disconnecting");
            NimBLEDevice::deleteBond(connInfo.getAddress());
            NimBLEDevice::getServer()->disconnect(connInfo.getConnHandle());
        }
    };
};

class BLECharCallbacks : public NimBLECharacteristicCallbacks
{
    void onWrite(NimBLECharacteristic *pCharacteristic, NimBLEConnInfo &connInfo) override
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
            timeCallback(pCharacteristic->getValue().data(), pCharacteristic->getLength());
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
            mediaDataCallback(pCharacteristic->getValue().data(), pCharacteristic->getLength());
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
            handleSettingsCallback(pCharacteristic->getValue().data(), pCharacteristic->getLength());            
        }
        else if (char_uuid == FIRMWARE_UPDATE_UUID){
            
            handleFirmwareUpdateCallback(pCharacteristic->getValue());
        }
        else if (char_uuid == FIRMWARE_RESET_UUID){
            handleFirmwareResetCallback(pCharacteristic->getValue<uint16_t>());
        }
        else if (char_uuid == DISPLAY_REINIT_UUID){
            reinitDisplayCallback();
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
    void onRead(NimBLECharacteristic *pCharacteristic, NimBLEConnInfo &connInfo) override
    {
        String char_uuid = pCharacteristic->getUUID().toString().c_str();
        if (char_uuid == TIME_UUID)
        {
            D_printf(" Read TIME: %s", pCharacteristic->getValue().c_str());
            pCharacteristic->setValue("Set Time Char");
            D_printf(" Read TIME: %s \n", pCharacteristic->getValue().c_str());
        }else if(char_uuid == SETTINGS_UUID){
            D_println(" Read Settings");
            pCharacteristic->setValue(sEEPROM);
        }else if(char_uuid == DISPLAY_BRIGHTNESS_UUID){
            D_println(" Read Brightness");
            pCharacteristic->setValue(sEEPROM.display_brightness);
        }else if(char_uuid == SCREEN_UUID){
            D_println(" Read Screen");
            pCharacteristic->setValue(sEEPROM.screen);
        }else if(char_uuid == FIRMWARE_VERSION_UUID){
            D_println(" Read Firmware Version");
            pCharacteristic->setValue((uint16_t)VERSION);
        }
    }
};

void setupBLEServer()
{
    D_printf("Start Task Core: %i", xPortGetCoreID());
    D_printf("Start Task: %i Micros", micros());
#ifdef DEBUG
    dbg_start = micros();
#endif

    NimBLEDevice::init("RykerConnect-MainUnit");

    NimBLEDevice::setSecurityAuth(true, true, false);
    uint32_t blePin = 100000 + (esp_random() % 900000);
    NimBLEDevice::setSecurityPasskey(blePin);
    NimBLEDevice::setSecurityIOCap(BLE_HS_IO_DISPLAY_ONLY);
    NimBLEServer *pServer = NimBLEDevice::createServer();
    pServer->setCallbacks(new ServerCallbacks());

    /** Optional: set the transmit power in dBm */
    NimBLEDevice::setPower(9); /** +9db */

    // region HID Setup
    /* HID */
    NimBLEHIDDevice *hid = new NimBLEHIDDevice(pServer);
    // input = hid->inputReport(1); // <-- input REPORTID from report map
    // output = hid->outputReport(1); // <-- output REPORTID from report map

    // output->setCallbacks(new MyCharCallbacks());

    hid->setManufacturer("Jan Boerschlein");

    //hid->pnp(0x02, 0xe502, 0xa111, 0x0210);
    hid->setPnp(0x00, 0x00, 0x0100, (VERSION << 8) | ((VERSION >> 8) & 0xFF));
    hid->setHidInfo(0x00, 0x02);

    static const uint8_t report[] = {
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
    hid->setReportMap((uint8_t *)report, sizeof(report));

    /**/
    // endregion

    NimBLEService *pService = pServer->createService(SERVICE_UUID);

    NimBLECharacteristic *timeCharacteristic = pService->createCharacteristic(TIME_UUID, NIMBLE_PROPERTY::READ | NIMBLE_PROPERTY::WRITE);
    NimBLECharacteristic *networkCharacteristic = pService->createCharacteristic(NETWORK_UUID, NIMBLE_PROPERTY::WRITE);
    NimBLECharacteristic *phoneBatteryCharacteristic = pService->createCharacteristic(PHONE_BATTERY_UUID, NIMBLE_PROPERTY::WRITE);
    NimBLECharacteristic *intercomBatteryCharacteristic = pService->createCharacteristic(INTERCOM_BATTERY_UUID, NIMBLE_PROPERTY::WRITE);
    NimBLECharacteristic *mediaDataCharacteristic = pService->createCharacteristic(MEDIA_DATA_UUID, NIMBLE_PROPERTY::WRITE | NIMBLE_PROPERTY::WRITE_NR);
    NimBLECharacteristic *notificationCharacteristic = pService->createCharacteristic(NOTIFICATION_UUID, NIMBLE_PROPERTY::WRITE | NIMBLE_PROPERTY::WRITE_ENC | NIMBLE_PROPERTY::WRITE_AUTHEN);

    NimBLECharacteristic *brightnessCharacteristic = pService->createCharacteristic(DISPLAY_BRIGHTNESS_UUID, NIMBLE_PROPERTY::READ | NIMBLE_PROPERTY::WRITE);
    NimBLECharacteristic *screenCharacteristic = pService->createCharacteristic(SCREEN_UUID, NIMBLE_PROPERTY::READ | NIMBLE_PROPERTY::WRITE);
    NimBLECharacteristic *settingsCharacteristic = pService->createCharacteristic(SETTINGS_UUID, NIMBLE_PROPERTY::READ | NIMBLE_PROPERTY::READ_ENC | NIMBLE_PROPERTY::READ_AUTHEN | NIMBLE_PROPERTY::WRITE | NIMBLE_PROPERTY::WRITE_ENC | NIMBLE_PROPERTY::WRITE_AUTHEN);
    NimBLECharacteristic *firmwareUpdateCharacteristic = pService->createCharacteristic(FIRMWARE_UPDATE_UUID, NIMBLE_PROPERTY::WRITE | NIMBLE_PROPERTY::WRITE_ENC | NIMBLE_PROPERTY::WRITE_AUTHEN);
    NimBLECharacteristic *firmwareResetCharacteristic = pService->createCharacteristic(FIRMWARE_RESET_UUID, NIMBLE_PROPERTY::WRITE | NIMBLE_PROPERTY::WRITE_AUTHEN);
    NimBLECharacteristic *displayReinitCharacteristic = pService->createCharacteristic(DISPLAY_REINIT_UUID, NIMBLE_PROPERTY::WRITE);
    NimBLECharacteristic *firmwareVersionCharacteristic = pService->createCharacteristic(FIRMWARE_VERSION_UUID, NIMBLE_PROPERTY::READ);

    BLECharCallbacks *charCallbacks = new BLECharCallbacks();
    timeCharacteristic->setCallbacks(charCallbacks);
    networkCharacteristic->setCallbacks(charCallbacks);
    phoneBatteryCharacteristic->setCallbacks(charCallbacks);
    intercomBatteryCharacteristic->setCallbacks(charCallbacks);
    mediaDataCharacteristic->setCallbacks(charCallbacks);
    notificationCharacteristic->setCallbacks(charCallbacks);

    brightnessCharacteristic->setCallbacks(charCallbacks);
    screenCharacteristic->setCallbacks(charCallbacks);
    settingsCharacteristic->setCallbacks(charCallbacks);
    firmwareUpdateCharacteristic->setCallbacks(charCallbacks);
    firmwareResetCharacteristic->setCallbacks(charCallbacks);
    displayReinitCharacteristic->setCallbacks(charCallbacks);
    firmwareVersionCharacteristic->setCallbacks(charCallbacks);

    brightnessCharacteristic->setValue(sEEPROM.display_brightness);
    timeCharacteristic->setValue("");
    firmwareVersionCharacteristic->setValue((uint16_t)VERSION);

    pServer->start();

    NimBLEAdvertising *pAdvertising = NimBLEDevice::getAdvertising();
    pAdvertising->setName("RykerConnect-MainUnit");
    pAdvertising->setAppearance(0x0180);
    // pAdvertising->addServiceUUID((uint16_t)0x1849);
    pAdvertising->addServiceUUID(SERVICE_UUID);
    pAdvertising->enableScanResponse(true);
    pAdvertising->start();
    D_println("BLE: Advertising started.");
    D_printf("BLE Address: %s", NimBLEDevice::getAddress().toString().c_str());
    // hid->setBatteryLevel(100);

#ifdef DEBUG
    dbg_end = micros();
#endif
    D_printf("End Task: %i Micros", dbg_end);
    D_printf("End Task Core: %i", xPortGetCoreID());
}
