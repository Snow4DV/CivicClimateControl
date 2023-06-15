# Civic Climate Control - Android app for android headunit to show cliamte control's state of Civic 5D (hatchback, 8th gen)
This app shows climate control's state of Honda Civic 5D-Hatchback (2006-2011) with the help of arduino adapter [with such firmware](https://github.com/Snow4DV/civic-adapter-platformio). 
</br>
![Pic1](/images/pic1.png)
![Pic2](/images/pic2.png)
# What can it do
* Shows climate control's state on your android head unit using arduino adapter to obtain data
* There's overlay that is drawn on top of other applications. It shows new climate control's state when it has changed
* Autostart is supported even on headunits that don't bother broadcasting BOOT\_COMPLETED
# Installation
[Download last version of applcation here {clickable}](https://github.com/Snow4DV/CivicClimateControl/releases/latest)
# WARNING - for MTK headunits' owners
If your headunit has МТК cpu (or AutoChips like AC8257) - there's huge probability that it will kill overlay's service soon to free memory. There are two ways: a) add application to the whitelist b) Disable duraspeed if it possible
# How to flash Arduino Uno adapter right from the application
Warning! Works on Arduino Uno only at the moment!
* Connect Android headunit to the internet
* [Get latest app version and install it](https://github.com/Snow4DV/CivicClimateControl/releases/latest)
* Connect adapter to the climate control's connector using [this diagram:](https://github.com/Snow4DV/civic-adapter-platformio/#%D0%BF%D0%BE%D0%B4%D0%BA%D0%BB%D1%8E%D1%87%D0%B5%D0%BD%D0%B8%D0%B5-%D0%B1%D0%B5%D0%B7-%D0%BE%D0%B1%D0%B2%D1%8F%D0%B7%D0%BA%D0%B8)  
![Connection diagram](https://github.com/Snow4DV/civic-adapter-platformio/raw/master/connection-example.png)
* Connected unflashed adapter via usb to the headunit
* Give permission to use adapter usb device (tick the checkbox "Always open this app")  
![Give permission](https://www.virtualhere.com/sites/default/files/android_allow_use.png)
* Go to settings and choose adapter from the drop down list 
* Click on "Flash adapter" item in settings
* If device is used as main one (works in MASTER mode) - leave checkbox ticked, in parallel - untick the "Master" checkbox
* Click "flash adapter" button in a corner. Try again if it doesn't work on the first try.
* Adapter is ready - you can start using it now!  
### Video instruction:
[![Video instruction](https://img.youtube.com/vi/Pt0uY-M1ryU/0.jpg)](https://www.youtube.com/watch?v=Pt0uY-M1ryU)
# How to autostart if headunit doesnt spread BOOT\_COMPLETED broadcast
In that case you can use another launcher like [FCC](https://4pda.to/forum/index.php?showtopic=882604) or [Agama](https://4pda.to/forum/index.php?showtopic=835814). Just add another special launcher (icon in launcher's apps' menu) called "Autostart overlay" to the headunit launcher's autostart. Now launcher will start dummy activity that will start overlay and close itself.  Here's how the process looks like in FCC Launcher:
![Pic4](/images/pic4.png)  
![Pic5](/images/pic5.png)  
# TODO
* \[Arduino Nano flashing problem - Pull Requests are welcome\] While Arduino Uno can easily be flashed, there are problems while flashing Arduino Nano: 1200BPS reset is not fully implemented. Gotta fix it and implement permissions' request: Arduino Nano's PID is changed while flashing (product id). [Issue in flasher\'s repo](https://github.com/t2t-sonbui/ArduinoHexUploadExample/issues/2)
# Adapter's firmware source code
To use this app adapter is needed, which can be flashed from this app or [built from sources.](https://github.com/Snow4DV/civic-adapter-platformio)
