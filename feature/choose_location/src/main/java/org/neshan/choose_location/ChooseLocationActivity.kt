package org.neshan.choose_location

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.neshan.choose_location.databinding.ActivityChooseLocationBinding

class ChooseLocationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChooseLocationBinding

    companion object {
        const val KEY_LATITUDE = "latitude"
        const val KEY_LONGITUDE = "longitude"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChooseLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.submit.setOnClickListener {
            setResult(RESULT_OK, Intent().apply {
                putExtra(KEY_LATITUDE, 35.667057)
                putExtra(KEY_LONGITUDE, 51.508333)
            })
            finish()
        }
    }
}