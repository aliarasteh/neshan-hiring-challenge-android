## **Choose Location**

This module is implemented for choosing location on map.

### Features

- [x] choose location by scrolling map
- [ ] search location by name
- [ ] suggest common locations

### Usage
Call `ChooseLocationActivity` and get latitude/longitude as result:

```java
ActivityResultLauncher<Intent> startChooseLocationForResult = this.registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
    @Override
    public void onActivityResult(ActivityResult result) {
        if (result.getResultCode() == RESULT_OK) {
            Bundle extras = result.getData().getExtras();
            double latitude = extras.getDouble(KEY_LATITUDE);
            double longitude = extras.getDouble(KEY_LONGITUDE);
            Log.e("Location", "latitude: " + latitude + " - longitude: " + longitude);
        }
    }
});

startChooseLocationForResult.launch(new Intent(this, ChooseLocationActivity.class));
```

