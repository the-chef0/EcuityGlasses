# Ecuity Smart Glasses Software
This repo was created to share the code created for the smart glasses concept in the first year of the Autonomous Walking project in the Honors Academy at the Eindhoven University of Technology. If you're reading this, I'm assuming you are one of the people continuing the Autonomous Walking initiative after the 2021/2022 academic year. I'm also assuming you have read the report written by Team Ecuity in May 2022.

This project is essentially an Android app which is eventually meant to be run on smart glasses with a camera, ARCore support and optionally a ToF sensor. Our prototype was implemented on a Huawei P30 Pro which should still be property of the TU/e Honors Academy so you should be able to get access to it.

## Repo contents
The app is made out of a combination of two different open source projects: ARCore's [Hello AR sample](https://github.com/google-ar/arcore-android-sdk/tree/master/samples/hello_ar_kotlin) and kai-morich's [SimpleBluetoothTerminal](https://github.com/kai-morich/SimpleBluetoothTerminal). The app uses the Hello AR sample to obtain (and render) a depth image from ARCore, the Ecuity software we added does intermediary processing, and the Bluetooth terminal sends the data to the ESP32 microcontroller which then controls the vibration motors.
