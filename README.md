# ResPiRadar
### Heart and Respiration rate from Radar on a Raspberry Pi
This application is made in a Bachelor's thesis project at Chalmers University of Technology in 2019. 
The purpose is to measure heart rate and respiration rate with radar and display the results in an interactive application.
The application is currently available on Google Play at https://play.google.com/store/apps/details?id=com.chalmers.respiradar.

#### How to use the application
The application is supposed to connect to a Raspberry Pi 3 Model B+ with a mounted Radar on it. 
The radar, A111 from Acconeer is a 60 GHz pulsed coherent radar system on a sensor board with an associated connector board 
(R112 and XC112 from Acconeer). The Python program that runs on the Raspberry Pi is available at 
https://github.com/Kandidatarbete-Chalmers-MCCX02-19-06/RaspberryPiRadarProgram. 
The application can, of course, connect to other devices as well, given that it has equivalent communication scripts. 
To do this, change the Bluetooth name or MAC-address. 
All data processing is made in the Python scripts, the application is just for visualizing the data.

The application will automatically try to search and connect to a Raspberry Pi. 
The Bluetooth button in the action bar can be used to connect/disconnect and to indicate the Bluetooth status. 
It flashed blue when it tries to connect. Press the START MEASURE button to start the measuring if connected. 
The graphs can be scrolled and zoomed in the x-direction and also be tapped on to show values. 
Rotate the device to landscape orientation to view real-time breathing amplitude. 
In settings, there are different Bluetooth settings that can be changed and controlled. 
There is also a virtual command terminal that can be used to get some special features from the application.
The Raspberry Pi can be turned off from the application using the command `poweroff` 
and can show a list of bonded Bluetooth devices with `list`.
The application can also simulate data with `simulate` to demonstrate the application if the Raspberry Pi is not available.

#### About the code
The MainActivity hosts the graphs to show heart rate and respiration rate. 
The BluetoothService is started simultaneously as the MainActivity and controls everything linked to Bluetooth.
The Bluetooth Service will automatically start Bluetooth on startup. 
The Bluetooth connection is made in a separate thread to not slow down the MainThread. 
The Settings includes all settings including Bluetooth settings. 
The Bluetooth settings are actually just for visual and control, all variables are stored in the BluetoothService. 
When the device is rotated to landscape orientation and the measurement is running, a new activity, RealTimeBreahting is started. 
It is also stopped when the device is rotated to portrait orientation again. 

#### Licence
MIT License

Copyright © 2019 Albin Warnicke and Erik Angert Svensson

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.



