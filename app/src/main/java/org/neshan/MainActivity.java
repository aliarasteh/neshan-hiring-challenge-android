package org.neshan;

import static org.neshan.choose_location.ChooseLocationActivity.KEY_LATITUDE;
import static org.neshan.choose_location.ChooseLocationActivity.KEY_LONGITUDE;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.neshan.choose_location.ChooseLocationActivity;
import org.neshan.common.model.LatLng;
import org.neshan.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    ActivityResultLauncher<Intent> startChooseLocationForResult = this.registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getResultCode() == RESULT_OK) {
                Bundle extras = result.getData().getExtras();
                double latitude = extras.getDouble(KEY_LATITUDE);
                double longitude = extras.getDouble(KEY_LONGITUDE);
                Log.e("Location", "latitude: " + latitude + " - longitude: " + longitude);
                Toast.makeText(MainActivity.this, "latitude: " + latitude, Toast.LENGTH_SHORT).show();
            }
        }
    });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initMap();

        binding.start.setOnClickListener(view -> {
            startChooseLocationForResult.launch(new Intent(this, ChooseLocationActivity.class));
        });
    }

    private void initMap() {
        // Setting map focal position to a fixed position and setting camera zoom
        binding.mapview.moveCamera(new LatLng(35.767234, 51.330743), 0);
        binding.mapview.setZoom(14, 0);
    }
}
