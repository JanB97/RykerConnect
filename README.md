# RykerConnect
Addon Media Display for CanAm Ryker

> [!CAUTION]
> Current Hardware Version (REV01) of the MainUnit has a hardware bug.
In my test, I did not connect the reset line of the display to a pin on the esp32. 
I did only connect it to the reset/enable pin of the esp. This seemed to be fine while testing, but with the production boards I am having problems with the initialization.
I would ***NOT*** recommend getting these boards produced right now.

This Project contains a firmware for a esp32s3 that is build on PlattformIO and the Arduino Core, an Android Companion App to get data on the Display and a simple ESP32S3 Board design to fit within a 3D printed case.
I will publish all the projects; however, please keep in mind that I am neither a hardware designer nor a software engineer, so pretty much all of this is messy.
It works, but that is a miracle.
Originally I did intend for this to have many features (e.g., show the weather, maybe map navigation, and more), but my development skills are not good enough right now.

The current features are only:
- Display the current time
- Keep the time while turned off
- Display the current temperature (if Hardware is installed on the board)
- Display current battery percentage of the connected phone
- Display 'Spotify' media information
- Display incoming notifications (mostly)
- Display current network status (mostly)

Things I want to add/fix:
- Notifications are currently mostly working, but sometimes apps do random things and I think the android app needs some work to handle that.
- Network status currently only goes up to 4G/LTE; again, the Android app needs some work to get 5G status as well.
- Display battery information of connected Intercom device (This is working within the app, but only rudimentary and it doesn't get sent to the esp)
- OTA Firmware updates (This technically is already possible with the current firmware, but the app doesn't support it yet)

> [!NOTE]
> You will need Programming and Hardware knowlage to get this to work right now, I don't think I can really support this right now, so please ho ahead on your own risk.
> If you can and are willing to help with the board design and programming, feel free to help, I am glad for every help on this!
