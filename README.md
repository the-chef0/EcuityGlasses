# Ecuity Smart Glasses Software
This repo was created to share the code created for the smart glasses concept in the first year of the Autonomous Walking project in the Honors Academy at the Eindhoven University of Technology. If you're reading this, I'm assuming you are one of the people continuing the Autonomous Walking initiative after the 2021/2022 academic year. I'm also assuming you have read the report written by Team Ecuity in May 2022.

This project is essentially an Android app which is eventually meant to be run on smart glasses with a camera, ARCore support and optionally a ToF sensor. Our prototype was implemented on a Huawei P30 Pro which should still be property of the TU/e Honors Academy so you should be able to get access to it.

## Repo contents
The app is made out of a combination of two different open source projects: ARCore's [Hello AR sample](https://github.com/google-ar/arcore-android-sdk/tree/master/samples/hello_ar_kotlin) and kai-morich's [SimpleBluetoothTerminal](https://github.com/kai-morich/SimpleBluetoothTerminal). The app uses the Hello AR sample to obtain (and render) a depth image from ARCore, the Ecuity software we added does intermediary processing, and the Bluetooth terminal sends the data to the ESP32 microcontroller which then controls the vibration motors.

### Hello AR sample
This is what the app is built around. By default, the entire program demonstrates ARCore functionality - plane detection, object placement and tracking, depth perception etc. I stripped it down to where it only gets the depth image, renders it on the screen and passes it to the Ecuity code (see line 230 in `HelloArRenderer`).

### Ecuity software
Our custom-made code can be found in the `java/kotlin/common.helpers` directory. It's responsible for segmenting the depth image, processing the segments, computing distance categories, and generating the final string with vibration frequencies that gets sent to the microcontroller. Detailed documention of classes and functions can be found below.

### Bluetooth terminal
By default, this thing lets you connect to a Bluetooth device and send and receive serial data from it. Here it's hardcoded to automatically connect to the MAC address (see `TerminalFragment`) of the ESP32 as soon as the app starts, and it runs in parallel with the ARCore code. Yes, there are much more elegant ways to do it but we didn't have time. Feel free to improve this.

### Microcontroller code
The code that runs on the microcontroller can be found in a [separate repo]().

# Ecuity code documentation
This is only meant to provide an overview. The methods themselves are extensively commented in the code files.

## Class overview
|      Class name      |                                          Description                                         |
|:--------------------:|:--------------------------------------------------------------------------------------------:|
| `EcuityDepthProcessor` | Processes the depth image by sliding the processing kernel/window over it.                   |
| `EcuitySensoryAdapter` | Takes the processed depth image and generates data that can be sent to the vibration motors. |
| `EcuityMathTools`      | Contains auxiliary methods for some mathematical routines.                                   |

### `EcuityDepthProcessor`
|  Access |      Method name      |                                                            Parameters                                                           |      Return type     |
|:-------:|:---------------------:|:-------------------------------------------------------------------------------------------------------------------------------:|:--------------------:|
| public  | `attachDepthImage`    | `depthImage: Image`                                                                                                             | `void`               |
| public  | `depthImageAttached`  | none                                                                                                                            | `Boolean`            |
| public  | `computeKernelCover`  | `kernelWidth: Int` `kernelHeight: Int` `function: String`                                                                       | `Array<DoubleArray>` |
| private | `applyKernel`         | `startingX: Int` `startingY: Int` `kernelWidth: Int` `kernelHeight: Int` `function: String`                                     | `Double`             |
| private | `writeToKernelCover`  | `kernelCover: Array<DoubleArray>` `descriptor: Double` `startingX: Int` `startingY: Int` `kernelWidth: Int` `kernelHeight: Int` | `void`               |
| private | `getMillimetersDepth` | `x: Int` `y: Int`                                                                                                               | `Int`                |

### Lifecycle:

<ol>
  <li> `attachDepthImage` initializes class variables
  <li> `computeKernelCover` segments depth image into kernel-sized segments
    <ol>
      <li> Runs `applyKernel` on each segment
      <li> Runs `writeToKernelCover` to write the value computed in the previous step to the output
    </ol>
</ol>
