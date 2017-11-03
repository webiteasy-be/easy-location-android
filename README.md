# easy-location-android
The goal of this library is to easy the management of location services on Android.
It provides a singleton with a wide range of `getLocation()` methods to match as many situations as possible.

## Features
- [x] serialisation in shared preferences
- [x] virtual location support
- [x] passive location mode to only get update when availables
- [x] optional use of [`EventBus`](https://github.com/greenrobot/EventBus) to listen to updates of the service
- [x] optional use of AppCompat

## Installation
```
compile 'com.github.webiteasy-be:easy-location-android:v0.0.2'
```
the library requires `com.google.android.gms:play-services-location`. 
A great improvement would be to be able to use the library, even without Google location service

## Use

### Initialization
EasyLocation can be initialized anywhere, with any reference to the context. A good idea is to initialize it during the app launch. 
```
public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        EasyLocation.init(this);
    }
}
```

### Ask for permission, GPS enabled and location
at the same time, from any Activity. This call will ask the user for location permission, then check and ask to 
turn on location service if not activated, and finally return location if available or return it through the callback. 
```
public class MyActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Location location = EasyLocation.getLocation(this, R.string.authorize_location, mCallback);
    }
}
```
The result of both permission request and location service activity should be returned to EasyLocation
```
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // NOTE next update will no more throw error, but only return false
        try {
            EasyLocation.onRequestPermissionsResult(requestCode, permissions, grantResults, this, R.string.authorize_location, mCallback);
        } catch (IllegalArgumentException e) {

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // NOTE next update will no more throw error, but only return false
        try {
            EasyLocation.onActivityResult(requestCode, resultCode, data, this, R.string.authorize_location, mCallback);
        } catch (IllegalArgumentException e) {

        }
    }
```

### Ask for location from anywhere
```
// Get location if available
Location location1 = EasyLocation.getLocation();

// Get location if available or return it to callback when available
Location location2 = EasyLocation.getLocation(callback);

// Get location if available and pass an updated location to callback when available
Location location3 = EasyLocation.getLocation(callback, true);
```
Callback may take a long time due to availability of the location service. 
To help GC, they are stored inside a [`WeakReference`](https://developer.android.com/reference/java/lang/ref/WeakReference.html). 
You should then always keep a strong reference to your callback untill the related View/Activity/... doesn't expect an update

### Use virtual location
Use virtual location to store a raw location
```
Location location = new Location("raw_location");
location.setLatitude(50.1);
location.setLongitude(4.2);

EasyLocation.setVirtualLocation(location);

// Later
Location locationBack = EasyLocation.getVirtualLocation();
```
