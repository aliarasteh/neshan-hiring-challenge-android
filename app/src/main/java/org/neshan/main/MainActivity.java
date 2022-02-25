package org.neshan.main;

import static org.neshan.choose_location.ChooseLocationActivity.EXTRA_LATITUDE;
import static org.neshan.choose_location.ChooseLocationActivity.EXTRA_LONGITUDE;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.carto.graphics.Bitmap;
import com.carto.graphics.Color;
import com.carto.styles.LineStyle;
import com.carto.styles.LineStyleBuilder;
import com.carto.styles.MarkerStyleBuilder;
import com.carto.utils.BitmapUtils;

import org.neshan.R;
import org.neshan.choose_location.ChooseLocationActivity;
import org.neshan.common.model.LatLng;
import org.neshan.component.util.FunctionExtensionKt;
import org.neshan.data.util.EventObserver;
import org.neshan.databinding.ActivityMainBinding;
import org.neshan.mapsdk.model.Marker;
import org.neshan.mapsdk.model.Polyline;

import java.util.ArrayList;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding mBinding;

    private MainActivityViewModel mViewModel;

    // a marker for selected location to be shown on map
    private Marker mDestinationMarker;

    // poly line for the path from start point to end point on map
    private Polyline mRoutingPathPolyLine;

    private final ActivityResultLauncher<Intent> mStartChooseLocationForResult = this.registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {

        // check if location selected
        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
            // handle selected location
            Bundle extras = result.getData().getExtras();
            double latitude = extras.getDouble(EXTRA_LATITUDE);
            double longitude = extras.getDouble(EXTRA_LONGITUDE);
            onDestinationSelected(new LatLng(latitude, longitude));
        }

    });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        mViewModel = new ViewModelProvider(this).get(MainActivityViewModel.class);
        // observe ViewModel live data objects changes
        observeViewModelChange(mViewModel);

        setViewListeners();

        initMap();

    }

    private void observeViewModelChange(MainActivityViewModel viewModel) {

        viewModel.getLocationAddressDetailLiveData().observe(this, address -> {
            if (address != null) {
                // show location detail bottom sheet
                LocationDetailBottomSheet bottomSheet = new LocationDetailBottomSheet();
                bottomSheet.show(getSupportFragmentManager(), "LocationDetail");
                bottomSheet.setOnDismissListener(dialogInterface -> {
                    // if user closed address detail then remove location marker from map
                    if (mDestinationMarker != null) {
                        mBinding.mapview.removeMarker(mDestinationMarker);
                        mViewModel.setEndPoint(null);
                    }
                    // if user closed address detail then remove drawn path from map
                    if (mRoutingPathPolyLine != null) {
                        mBinding.mapview.removePolyline(mRoutingPathPolyLine);
                    }
                });
            }
        });

        viewModel.getRoutePoints().observe(this, routePoints -> {
            showPathOnMap(routePoints);
        });

        viewModel.getGeneralErrorLiveData().observe(this, new EventObserver<>(error -> {
            FunctionExtensionKt.showError(mBinding.getRoot(), error);
            return null;
        }));

    }

    private void setViewListeners() {

        mBinding.chooseLocation.setOnClickListener(view -> {
            // open Choose Location Activity to choose destination location
            mStartChooseLocationForResult.launch(new Intent(this, ChooseLocationActivity.class));
        });

    }

    private void initMap() {

        // Setting map focal position to a fixed position and setting camera zoom
        // TODO: show current user location instead
        mBinding.mapview.moveCamera(new LatLng(35.767234, 51.330743), 0);
        mBinding.mapview.setZoom(14, 0);

    }

    // does required actions when destination location has been chosen
    private void onDestinationSelected(LatLng latLng) {

        // remove previously added marker from map and add new marker to selected location
        if (mDestinationMarker != null) {
            mBinding.mapview.removeMarker(mDestinationMarker);
        }
        mDestinationMarker = createMarker(latLng);

        mBinding.mapview.moveCamera(latLng, 0.25f);
        mBinding.mapview.setZoom(15f, 0.25f);
        mBinding.mapview.addMarker(mDestinationMarker);

        // load address detail for selected location
        mViewModel.loadAddressForLocation(latLng);
        mViewModel.setEndPoint(latLng);

    }

    // This method gets a LatLng as input and adds a marker on that position
    private Marker createMarker(LatLng latLng) {

        MarkerStyleBuilder markStCr = new MarkerStyleBuilder();
        markStCr.setSize(30f);

        Drawable drawable = ContextCompat.getDrawable(this, R.drawable.ic_location_marker);
        if (drawable != null) {
            Bitmap markerBitmap = BitmapUtils.createBitmapFromAndroidBitmap(FunctionExtensionKt.toBitmap(drawable));
            markStCr.setBitmap(markerBitmap);
        }

        return new Marker(latLng, markStCr.buildStyle());

    }

    private void showPathOnMap(ArrayList<LatLng> routePoints) {

        if (mRoutingPathPolyLine != null) {
            mBinding.mapview.removePolyline(mRoutingPathPolyLine);
        }
        mRoutingPathPolyLine = new Polyline(routePoints, getLineStyle());
        mBinding.mapview.addPolyline(mRoutingPathPolyLine);

        LatLng startPoint = mViewModel.getStartPoint();
        LatLng endPoint = mViewModel.getStartPoint();

        // set map camera zoom to have a better view of path
        double centerFocalPositionX = (startPoint.getLatitude() + endPoint.getLatitude()) / 2;
        double centerFocalPositionY = (startPoint.getLongitude() + endPoint.getLongitude()) / 2;
        mBinding.mapview.moveCamera(new LatLng(centerFocalPositionX, centerFocalPositionY), 0.5f);
        mBinding.mapview.setZoom(15, 0.5f);

    }

    private LineStyle getLineStyle() {
        LineStyleBuilder lineStCr = new LineStyleBuilder();
        Color color = new Color(ContextCompat.getColor(this, R.color.colorPrimaryDim75));
        lineStCr.setColor(color);
        lineStCr.setWidth(10f);
        lineStCr.setStretchFactor(0f);
        return lineStCr.buildStyle();
    }

}
