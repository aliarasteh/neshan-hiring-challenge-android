package org.neshan.component.location

import android.location.Location

interface LocationListener {

    fun onLocationChange(location: Location)

}