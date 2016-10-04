# FireTower

Android application to control Fire Tower project at the Atomic Cult Camp on Burning Man.

Fire tower has 22 natural gas puffers mounted at the top. Each puffer is connected to gas expansion tank with the gas relay controlled by Arduino board. Arduino is listening to commands issued by mobile app over BTLE. 

This project contains the mobile app source code.

Commands to control burners has the  following format:
time_to_burn_in_ms, channel_to_switch_on [,another channel][,another channel]
Example of command that will turn burners 1 and 2 for 500[ms]: 500, 1, 2 

Separate repository contains the code for the Arduino controller:
https://github.com/leybzon/FireTowerArduino

You can install this application from the Google Playstore:
https://play.google.com/store/apps/details?id=com.stream11.puffer

![Completed project](https://github.com/leybzon/FireTower/blob/master/videotogif_2016.10.04_15.01.57.gif "Completed Project")
