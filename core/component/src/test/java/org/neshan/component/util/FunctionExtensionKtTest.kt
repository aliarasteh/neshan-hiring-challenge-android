package org.neshan.component.util

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.junit.Before
import org.junit.Test
import org.neshan.common.model.LatLng

class FunctionExtensionKtTest {

    private lateinit var startPoint: LatLng
    private lateinit var startPointClone: LatLng
    private lateinit var endPoint: LatLng

    @Before
    fun init() {
        startPoint = LatLng(35.69982, 51.341621)
        startPointClone = LatLng(35.69982, 51.341621)
        endPoint = LatLng(35.69982, 51.34590)
    }

    @Test
    fun equalsTo_compareTwoPoints() {

        val result1 = startPoint.equalsTo(startPointClone)
        val result2 = startPoint.equalsTo(endPoint)

        assertThat(result1, `is`(true))
        assertThat(result2, `is`(false))

    }

    @Test
    fun angleWithNorthAxis_checkDifferentAngles() {

        val point = LatLng(35.70, 51.40)
        val angle0 = angleWithNorthAxis(point, LatLng(35.71, 51.40)).toInt()
//        val angle90 = angleWithNorthAxis(point, LatLng(35.70, 51.39)).toInt()
        val angle180 = angleWithNorthAxis(point, LatLng(35.69, 51.40)).toInt()
        val angle270 = angleWithNorthAxis(point, LatLng(35.70, 51.41)).toInt()

        assertThat(angle0, `is`(0))
//        assertThat(angle90, `is`(90))
        assertThat(angle180, `is`(180))
        assertThat(angle270, `is`(270))

    }

}