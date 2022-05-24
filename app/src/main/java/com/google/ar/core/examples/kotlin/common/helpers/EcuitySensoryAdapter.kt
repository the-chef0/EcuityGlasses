package com.google.ar.core.examples.kotlin.common.helpers

import android.media.Image
import android.util.Log
import java.lang.StringBuilder
import kotlin.math.ceil
import kotlin.math.floor

/*
 * DISTANCE CATEGORIES
 *
 * The [attach] funciton of this class takes arguments [thresholdDistance] and
 * [numOfDistanceCategories]. [thresholdDistance - 1] is the furthest obstacle distance (mm) that we
 * consider relevant for the haptic devices. For example, if [ [thresholdDistance] == 3000], then
 * any obstacle closer than 3 meters will be reported to the haptic devices.
 *
 * Next, [numOfDistanceCategories] defines how many segments (of equal lengths) we divide our
 * monitored range into. We index them from 0 to [numOfDistanceCategories - 1]
 * For example, if [ [thresholdDistance] == 3000] and [ [numOfDistanceCategories] == 3],
 * then we divide the 3 meter range into 3 segments:
 * [0, 1000] (index 0), [1000, 2000] (index 1) and [2000, 3000] (index 2).
 *
 * To retain information about what is beyond the monitored range, i.e. at a distance of
 * [thresholdDistance] or more, we define this to include an extra category at index
 * [numOfDistanceCategories]. Coming back to the example, this would be category [3000+]
 * at index 3.
 *
 * Since we want the closer obstacles to vibrate faster, distance categories are converted into
 * frequencies. For implementation details, see the EcuityMathTools class.
 *
 * CONVERSION TO VIBRATION FREQUENCIES
 *
 * Let [numOfDistanceCategories] be the number of distance categories.
 * This maps to a range of frequencies [ 1 <= [f] <= [numOfDistanceCategories] ] where [f] is an
 * integer. Suppose we have an obstacle belonging to distance category [c].
 * If  [ [c] == [numOfDistanceCategories] ], i.e. is the most distant category, we want the slowest
 * vibration in the range, i.e. 1 Hz. If [ [c] == 0 ], i.e. is the closest category, we want the
 * fastest vibration, i.e. [numOfDistanceCategories] Hz. So in general, the frequency is given by
 * [ [f] = [numOfDistanceCategories] - [c] ]
 */

class EcuitySensoryAdapter() {
    private var depthProcessor = EcuityDepthProcessor()
    private lateinit var kernelCover: Array<DoubleArray>
    private var HAPTIC_DEVICE_ROWS = 0
    private var HAPTIC_DEVICE_COLUMNS = 0
    private lateinit var rowSegmentation: Array<Int>
    private lateinit var columnSegmentation: Array<Int>
    private val mathTools = EcuityMathTools()

    constructor(hapticDeviceRows: Int, hapticDeviceColumns: Int) : this() {
        if (hapticDeviceRows < 1 || hapticDeviceColumns < 1) {
            throw IllegalArgumentException()
        }
        HAPTIC_DEVICE_ROWS = hapticDeviceRows
        HAPTIC_DEVICE_COLUMNS = hapticDeviceColumns
        rowSegmentation = Array(HAPTIC_DEVICE_ROWS + 1) { _ -> -1 }
        columnSegmentation = Array(HAPTIC_DEVICE_COLUMNS + 1) { _ -> -1 }
    }

    /**
     * Obtains a [kernelCover] from a [depthImage] using [depthProcessor], defines borders of the
     * [depthImage] segmentation and returns an [obstacleMap] corresponding to the layout of
     * haptic devices as determined by [HAPTIC_DEVICE_ROWS] and [HAPTIC_DEVICE_COLUMNS].
     */
    fun attach(depthImage: Image, thresholdDistance: Int, numOfDistanceCategories: Int):
            Array<IntArray> {

        if (numOfDistanceCategories < 0 || thresholdDistance < 0) {
            throw IllegalArgumentException()
        }

        depthProcessor.attachDepthImage(depthImage)
        this.kernelCover = depthProcessor.computeKernelCover(5, 5, "MEDIAN")

        if (this.rowSegmentation[0] == -1) {
            this.rowSegmentation[0] = 0
            computeSegmentation(HAPTIC_DEVICE_ROWS, rowSegmentation, "ROWS")
        }
        if (this.columnSegmentation[0] == -1) {
            this.columnSegmentation[0] = 0
            computeSegmentation(HAPTIC_DEVICE_COLUMNS, columnSegmentation, "COLUMNS")
        }

        return hapticFrequencies(thresholdDistance, numOfDistanceCategories)
    }

    /**
     * Divides the number of pixels in a given [dimension] of [kernelCover] into (almost) equal
     * [segments], corresponding to the number of haptic devices in that [dimension] of the
     * haptic device layout. If the number of pixels is not divisible by [segments], the remainder
     * gets added to the first segment.
     * @return [segmentation] where [ segmentation[i] ] and [ segmentation[i+1] ] are the borders of
     *                        segment i such that [ 0 <= i < [segments] ]
     */
    private fun computeSegmentation(segments: Int, segmentation: Array<Int>, dimension: String) {
        var pixelsPerSegment = 0
        var remainder = 0

        when (dimension) {
            "ROWS" -> {
                pixelsPerSegment = kernelCover[0].size.div(segments)
                remainder = kernelCover[0].size.rem(segments)
            }
            "COLUMNS" -> {
                pixelsPerSegment = kernelCover.size.div(segments)
                remainder = kernelCover.size.rem(segments)
            }
        }

        for (index in 1 until segmentation.size) {
            if (index == 1) {
                segmentation[index] = pixelsPerSegment + remainder
            } else {
                segmentation[index] = segmentation[index - 1] + pixelsPerSegment
            }
        }
    }

    /**
     * Returns an array [obstacleMap] of dimensions [HAPTIC_DEVICE_ROWS] by [HAPTIC_DEVICE_COLUMNS]
     * where [ obstacleMap[r][c] == [f] ] with [f] being the vibration frequency in Hz for the
     * vibration motor at row [r] and column [c].
     * If [ [f] == 0 ], we define this to mean "do not vibrate", i.e. there is no relevant
     * obstacle that is close enough.
     */
    private fun hapticFrequencies(thresholdDistance: Int, numOfDistanceCategories: Int):
            Array<IntArray> {

        val hapticFrequencies = Array(HAPTIC_DEVICE_ROWS) {IntArray(HAPTIC_DEVICE_COLUMNS)}

        for (rowIndex in 1 until rowSegmentation.size) {
            for (columnIndex in 1 until columnSegmentation.size) {
                val rowLowBorder = rowSegmentation[rowIndex - 1]
                val columnLowBorder = columnSegmentation[columnIndex - 1]
                val rowHighBorder = rowSegmentation[rowIndex]
                val columnHighBorder = columnSegmentation[columnIndex]

                val categoryCounts = distanceCategoryCountsInRegion(thresholdDistance,
                    numOfDistanceCategories, rowLowBorder, columnLowBorder, rowHighBorder,
                    columnHighBorder)

                hapticFrequencies[rowIndex - 1][columnIndex - 1] =
                    mathTools.categoryCountsToFrequency(numOfDistanceCategories, categoryCounts, 1)
            }
        }

        return hapticFrequencies
    }

    /**
     * For a given region in the [kernelCover] defined by [rowLowBorder], [columnLowBorder],
     * [rowHighBorder] and [columnHighBorder], returns an array [distanceCategoryCounts] where,
     * for some distance category [c], [ distanceCategoryCounts[c] ] contains the number of
     * pixels that fall into that distance category.
     */
    private fun distanceCategoryCountsInRegion(thresholdDistance: Int, numOfDistanceCategories: Int,
                                               rowLowBorder: Int, columnLowBorder: Int,
                                               rowHighBorder: Int, columnHighBorder: Int):
            IntArray {

        var distanceCategoryCounts = IntArray(numOfDistanceCategories + 1) {0}

        for (x in columnLowBorder until columnHighBorder) {
            for (y in rowLowBorder until rowHighBorder) {
                val category = mathTools.distanceToCategory(kernelCover[x][y], thresholdDistance,
                    numOfDistanceCategories)
                distanceCategoryCounts[category] += 1
            }
        }

        return distanceCategoryCounts
    }

    public fun motorValuesToString(motorValues: Array<IntArray>, rowLimit: Int, columnLimit: Int):
    String {

        val bobTheBuilder = StringBuilder()
        for (row in 0 .. rowLimit) {
            for (column in 0 .. columnLimit) {
                bobTheBuilder.append(motorValues[row][column])
            }
        }
        return bobTheBuilder.toString()
    }
}