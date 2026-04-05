# RykerConnect
Addon Media Display for CanAm Ryker

> [!WARNING]
> Current Hardware Version (REV01) of the MainUnit has a hardware bug.
In my test, I did not connect the reset line of the display to a pin on the esp32. 
I did only connect it to the reset/enable pin of the esp. This seemed to be fine while testing, but with the production boards I am having problems with the initialization.
I would ***NOT*** recommend getting these boards produced right now.
> 
> **Update 05.04.2026**: It's fixed in software for the most part, it seems like it's working now, just a bit buggy on startup (you see weird or old images).
> I have created a Rev2 that uses a gpio as reset line, but this board is not tested, i have not ordered it and you will need to build the firmware yourseld, because you need to change the pinout from using no pin to IO15 for the display reset.

This Project contains a firmware for a esp32s3 that is build on PlattformIO and the Arduino Core, an Android Companion App to get data on the Display and a simple ESP32S3 Board design to fit within a 3D printed case.
I will publish all the projects; however, please keep in mind that I am neither a hardware designer nor a software engineer, so pretty much all of this is messy.
It works, but that is a miracle.
Originally I did intend for this to have many features (e.g., show the weather, maybe map navigation, and more), but my development skills are not good enough right now.

The current features are only:
- Display the current time
- Keep the time while turned off
- Display the current temperature (if Hardware is installed on the board)
- Display current battery percentage of the connected phone
- Display current battery percentage of the connected intercom
- Display 'Spotify' media information
- Display Youtube Music media information
- Display incoming notifications (mostly)
- Display current network status
- ota updated from github or with firmware.bin file.
- Settings for: Brightness, Batteries to show, notification display timeout
- Quick Screen Reset (in case it will still bug out with the newest firmware and only display on one side of the oled)

Things I want to add/fix:
- Notifications are currently mostly working, but sometimes apps do random things and I think the android app needs some work to handle that.

Things to add to REV3 Hardware:
- similar to REV2 extend "empty" space around and under temp sensor, to help it not warm up because of the casing and/or ESP32
- add 3 wire (V, GND, S) connector for external DS18B20 (technically possible with REV1 through the exposed pins


REV3 is most likely (if at all) the next hardware rev i am going to build, as my REV1 is working good enough now, and there is no reason to build rev2 without the external sensor now.


> [!NOTE]
> You will need Programming and Hardware knowlage to get this to work right now, I don't think I can really support this right now, so please ho ahead on your own risk.
> If you can and are willing to help with the board design and programming, feel free to help, I am glad for every help on this!
