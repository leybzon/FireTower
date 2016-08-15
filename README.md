# FireTower

Android application to control Fire Tower project at the Atomic Cult Camp on Burning Man.

Application talks to the Tower over the BTLE protocol. Commands are issued by the app are in the following format:
time_to_burn_in_ms, channel_to_switch_on [,another channel][,another channel]
Example of command that will turn burners 1 and 2 for 500[ms]: 500, 1, 2 

Fire Tower is running by Arduino board with this code:
https://github.com/leybzon/FireTowerArduino
