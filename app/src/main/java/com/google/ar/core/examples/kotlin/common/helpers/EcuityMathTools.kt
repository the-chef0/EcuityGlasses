package com.google.ar.core.examples.kotlin.common.helpers

import java.lang.IllegalArgumentException
import kotlin.math.floor

class EcuityMathTools {

    internal fun median(array: Array<Int>): Double {
        if (array.isEmpty()) {
            throw IllegalArgumentException()
        }

        array.sort()
        val half = array.size.div(2)

        return if (array.size.rem(2) == 0) {
            (array[half - 1].toDouble() + array[half].toDouble()) / 2
        } else {
            array[half].toDouble()
        }
    }

    /**
     * To understand how distance categories are indexed, see the EcuitySensoryAdapter class.
     * Returns the index of the distance category that the given [distance] belongs to.
     */
    internal fun distanceToCategory(distance: Double, thresholdDistance: Int,
                           numOfDistanceCategories: Int): Int {
        return if (distance >= thresholdDistance) {
            numOfDistanceCategories
        } else {
            floor((distance/thresholdDistance.toDouble()) * numOfDistanceCategories).toInt()
        }
    }

    /**
     * To understand the logic behind the calculation, see the EcuitySensoryAdapter class.
     * In an array [categoryCounts], converts the index of the distance category with the
     * highest number of pixels belonging to it into a frequency in Hz.
     * Returns that value multiplied by [multiplier] for additional control.
     */
    internal fun categoryCountsToFrequency(numOfDistanceCategories: Int,
                                           categoryCounts: IntArray, multiplier: Int): Int {
        return (numOfDistanceCategories - argmax(categoryCounts)) * multiplier
    }

    private fun argmax(array: IntArray): Int {
        var max = Int.MIN_VALUE
        var maxArg = -1

        for (i in array.indices) {
            if (array[i] > max) {
                max = array[i]
                maxArg = i
            }
        }

        return maxArg
    }

}