# Ecuity Glasses Software
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

The final result is an array with the same dimensions as the depth image but with a reduced amount of information - every kernel-sized segment contains the median of the depth values in that segment.

### `EcuitySensoryAdapter`

|  Access |            Method name           |                                                                    Parameters                                                                   |    Return type    |
|:-------:|:--------------------------------:|:-----------------------------------------------------------------------------------------------------------------------------------------------:|:-----------------:|
| public  | `constructor`                    | `hapticDeviceRows: Int` `hapticDeviceColumns: Int`   | `EcuitySensoryAdapter`
| public  | `attach`                         | `depthImage: Image` `thresholdDistance: Int` `numOfDistanceCategories: Int`                                                                     | `Array<IntArray>` |
| public  | `motorValuesToString`            | `motorValues: Array<IntArray>` `rowLimit: Int` `columnLimit: Int`                                                                               | `String`          |
| private | `computeSegmentation`            | `segments: Int` `segmentation: Array<Int>` `dimension: String`                                                                                  | `void`            |
| private | `hapticFrequencies`              | `thresholdDistance: Int` `numOfDistanceCategories: Int`                                                                                         | `Array<IntArray>` |
| private | `distanceCategoryCountsInRegion` | `thresholdDistance: Int` `numOfDistanceCategories: Int` `rowLowBorder: Int` `columnRowBorder: Int` `rowHighBorder: Int` `columnHighBorder: Int` | `IntArray`        |

#### `motorValuesToString`
For our prototype, we used a 3x3 vibration motor layout, so a generated string `S = ABCDEFGHI` (with `A,B ... I` integers denoting vibration frequency) maps to the following vibration motor layout:   
`A B C`   
`D E F`   
`G H I`   
So just like reading the motor layout left to right, top to bottom.

### `EcuityMathTools`

|  Access  |         Method name         |                                  Parameters                                  | Return type |
|:--------:|:---------------------------:|:----------------------------------------------------------------------------:|:-----------:|
| internal | `median`                    | `array: Array<Int>`                                                          | `Double`    |
| internal | `distanceToCategory`        | `distance: Double` `thresholdDistance: Int` `numOfDistanceCategories: Int`   | `Int`       |
| internal | `categoryCountsToFrequency` | `numOfDistanceCategories: Int` `categoryCounts: IntArrays` `multiplier: Int` | `Int`       |
| private  | `argmax`                    | `array: IntArray`                                                            | `Int`       |


### Lifecycle:

1. An instance of `EcuitySensoryAdapter` gets initialized in `HelloArRenderer`, defining the number of rows and columns of vibration motors we are using.
2. `EcuitySensoryAdapter` creates its own, local instance of `EcuityDepthProcessor`.
3. Once a depth image is obtained, we call `attach` on the instance of `EcuitySensoryAdapter`.
4. `attach` calls `attachDepthImage`.
5. `attach` calls `computeKernelCover`, defining the kernel dimensions (hardcoded).
6. `computeKernelCover` loops over the depth image in kernel-sized chunks.
7. `computeKernelCover` calls `applyKernel` on each chunk - applies a function (in this case a median but it's flexible) to all depth values in that chunk and returns the result `M`.
8. `computeKernelCover` calls `writeToKernelCover` to write `M` to all pixels that were covered by the kernel.
9. `computeKernelCover` returns a `kernelCover`.
10. `attach` calls `computeSegmentation` on both rows and columns to divide the `kernelCover` into a grid corresponding to the layout of the vibration motors.
11. `attach` calls `hapticFrequencies`.
12. `hapticFrequencies` loops over the `kernelCover` in chunks corresponding to the segmentation created by `computeSegmentation`.
13. `hapticFrequencies` calls `distanceCategoryCountsInRegion` to count how many pixels fall into each distance category.
14. `hapticFrequencies` calls `categoryCountsToFrequency` to determine which distance category occurs most frequently and obtain the vibration frequency `F` corresponding to the distance cateogory.
15. `hapticFrequencies` writes `F` to the corresponding segment.
16. `hapticFrequencies` returns an array `A` where each element contains the vibration frequency for its corresponding motor.
17. `attach` returns `A`.
18. Back in `HelloArRenderer`, we call `motorValuesToString` on `A` to get a string `S` to send to the microcontroller via Bluetooth.
19. Transmit `S` to the microcontroller.
20. Wait for the next depth image and go to step 3.

## Additional notes
To whoever carries on with the project, please keep up a decent standard of code commenting and documentation. If you don't, when you come home, I'll be under your bed.
