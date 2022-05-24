package com.google.ar.core.examples.kotlin.common.helpers

import android.media.Image
import android.util.Log
import com.google.ar.core.examples.kotlin.exceptions.IncompatibleKernelHeightException
import com.google.ar.core.examples.kotlin.exceptions.IncompatibleKernelWidthException
import com.google.ar.core.examples.kotlin.exceptions.UnattachedDepthImageException
import java.nio.ByteOrder

class EcuityDepthProcessor {
    private var DEPTH_IMAGE_WIDTH = 0
    private var DEPTH_IMAGE_HEIGHT = 0
    private lateinit var depthImage: Image
    private var mathTools = EcuityMathTools()

    /**
     * Assigns the passed [depthImage] to [this.depthImage] of this instance of
     * [EcuityDepthProcessor], and assigns values to [DEPTH_IMAGE_WIDTH] and [DEPTH_IMAGE_HEIGHT]
     * based on the dimensions of the [depthImage].
     * @param depthImage obtained from an ARCore [Frame]
     */
    fun attachDepthImage(depthImage: Image) {
        this.depthImage = depthImage
        DEPTH_IMAGE_HEIGHT = this.depthImage.height
        DEPTH_IMAGE_WIDTH = this.depthImage.width
    }

    /**
     * Determines the starting top-left coordinates for each placement of the kernel,
     * applies the kernel to the given region and returns and array [kernelCover] with dimensions
     * equal to that of [this.depthImage]. For each pixel of [this.depthImage], [kernelCover]
     * stores the result of applying the kernel to the region that the pixel is contained in.
     * @param kernelWidth the width of the kernel
     * @param kernelHeight the height of the kernel
     * @param function the function to apply to each kernel
     * @return [kernelCover] the result of applying the kernel to [this.depthImage]
     * @throws IncompatibleKernelWidthException if the width of the depth image is not divisible
     *                                          by the width of the kernel
     * @throws IncompatibleKernelHeightException analogous for height
     * @throws UnattachedDepthImageException if [attachDepthImage] wasn't run beforehand
     */
    fun computeKernelCover(kernelWidth: Int, kernelHeight: Int, function: String):
            Array<DoubleArray> {

        if (DEPTH_IMAGE_WIDTH.rem(kernelWidth) != 0) {
            throw IncompatibleKernelWidthException()
        } else if (DEPTH_IMAGE_HEIGHT.rem(kernelHeight) != 0) {
            throw IncompatibleKernelHeightException()
        } else if (!this.depthImageAttached()) {
            throw UnattachedDepthImageException()
        }

        val numOfKernelsHorizontally: Int = DEPTH_IMAGE_WIDTH.div(kernelWidth)
        val numOfKernelsVertically: Int = DEPTH_IMAGE_HEIGHT.div(kernelHeight)
        val kernelCover = Array(DEPTH_IMAGE_WIDTH) {DoubleArray(DEPTH_IMAGE_HEIGHT)}

        for (x in 0 until numOfKernelsHorizontally) {
            for (y in 0 until numOfKernelsVertically) {
                val startingX = x * kernelWidth
                val startingY = y * kernelHeight
                val descriptor = applyKernel(startingX, startingY, kernelWidth, kernelHeight,
                    kernelCover, function)
                writeToKernelCover(kernelCover, descriptor, startingX, startingY, kernelWidth,
                    kernelHeight)
            }
        }

        return kernelCover
    }

    private fun depthImageAttached() : Boolean {
        return this::depthImage.isInitialized
    }

    /**
     * Applies the specified [function] to the region covered by the kernel, as determined by
     * [startingX], [startingY], [kernelWidth] and [kernelHeight].
     * @return [descriptor]
     * @throws IllegalArgumentException if [function] is not recognized
     */
    private fun applyKernel(startingX: Int, startingY: Int, kernelWidth: Int,
                kernelHeight: Int, kernelCover: Array<DoubleArray>, function: String): Double {

        val currentKernelValues = Array(kernelWidth * kernelHeight) { _ -> -1 }
        var index = 0

        for (x in startingX until startingX + kernelWidth) {
            for (y in startingY until startingY + kernelHeight) {
                currentKernelValues[index] = getMillimetersDepth(x, y)
                index += 1
            }
        }

        var descriptor: Double?

        when(function) {
            "MEDIAN" -> descriptor = mathTools.median(currentKernelValues)
            else -> throw IllegalArgumentException()
        }

        return descriptor
    }

    /**
     * Assigns the [descriptor] value to each pixel in a region that resulted from applying a
     * kernel to that region, as determined by [startingX], [startingY], [kernelWidth] and
     * [kernelHeight].
     * @modifies [kernelCover]
     */
    private fun writeToKernelCover(kernelCover: Array<DoubleArray>, descriptor: Double,
                                   startingX: Int, startingY: Int, kernelWidth: Int,
                                   kernelHeight: Int) {

        for (x in startingX until startingX + kernelWidth) {
            for (y in startingY until startingY + kernelHeight) {
                kernelCover[x][y] = descriptor
            }
        }
    }

    private fun getMillimetersDepth(x: Int, y: Int): Int {
        // The depth image has a single plane, which stores depth for each
        // pixel as 16-bit unsigned integers.
        val plane = this.depthImage.planes[0]
        val byteIndex = x * plane.pixelStride + y * plane.rowStride
        val buffer = plane.buffer.order(ByteOrder.nativeOrder())
        val depthSample = buffer.getShort(byteIndex)
        return depthSample.toInt()
    }

}