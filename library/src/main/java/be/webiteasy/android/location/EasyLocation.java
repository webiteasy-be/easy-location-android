package be.webiteasy.android.location;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import org.greenrobot.eventbus.EventBus;

import java.lang.ref.WeakReference;
import java.util.LinkedList;

public class EasyLocation {

    public static int INTENT_PERMISSION_LOCATION = 7348;
    public static int ACTIVITY_RESULT_ENABLE_PROVIDER = 7349;

    private static final String PREFERENCES_NAME = "EasyLocation$Preferences";
    private static final String PREFERENCES_LAST_LOCATION = "EasyLocation$Preferences.last_location";
    private static final String PREFERENCES_VIRTUAL_LOCATION = "EasyLocation$Preferences.virtual_location";

    @Nullable
    private static EasyLocation instance;

    private final LocationHandler handlerInstance;

    private final SharedPreferences mPreferences;

    private Location mLastLocation;

    private Location mVirtualLocation;

    private LinkedList<WeakReference<Callback>> mUpdateCallbacks = new LinkedList<>();
    private final Object mUpdateCallbacksLock = new Object();

    /**
     * Initialize WipLocation singleton holder. This method has to be called before any call to
     * static functions of {@link EasyLocation} or you will get a {@link IllegalStateException}
     */
    public synchronized static void init(Context context) {
        if (instance == null) {
            instance = new EasyLocation(context);
        }
    }

    private EasyLocation(Context context) {
        handlerInstance = new LocationHandler(context);
        handlerInstance.addLocationListener(new LocationHandler.Listener() {
            @Override
            public void onLocationChanged(LocationHandler.LocationChangedEvent event) {
                updateLocation(event.getLocation());
            }

            @Override
            public void onLocationError(LocationHandler.LocationErrorEvent event) {

            }
        });

        mPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);

        mLastLocation = deserializeLocation(mPreferences.getString(PREFERENCES_LAST_LOCATION, null));
        mVirtualLocation = deserializeLocation(mPreferences.getString(PREFERENCES_VIRTUAL_LOCATION, null));
    }

    /**
     * Virtual location is the location set by the user. It can be used by the application to show
     * contents for a given location, different from the real location of the user.
     *
     * @return the current virtual location
     */
    @Nullable
    public static Location getVirtualLocation() {
        if (instance == null)
            throw new IllegalStateException("EasyLocation must be implemented");

        return instance.mVirtualLocation;
    }

    /**
     * Sets the current virtual location. It can be used by the application to show
     * contents for a given location, different from the real location of the user.
     * When virtual location is set, a {@link NewVirtualLocationEvent} event is posted through
     * {@link EventBus} if library is available
     *
     * @param location the new virtual location
     */
    public static void setVirtualLocation(@Nullable Location location) {

        if (instance == null)
            throw new IllegalStateException("EasyLocation must be implemented");

        instance.mVirtualLocation = location;
        instance.mPreferences.edit().putString(PREFERENCES_VIRTUAL_LOCATION, serializeLocation(location)).apply();

        if (hasEventBus()) {
            org.greenrobot.eventbus.EventBus.getDefault().post(new NewVirtualLocationEvent(location));
        }
    }

    /**
     * Returns the current location of the device, if available, and will not try to get a refreshed
     * value even if no location is available.
     *
     * @return the last known location
     */
    @Nullable
    public static Location getLocation() {
        return getLocation(null);
    }

    /**
     * If a location is available, it will be returned and passed back to the callback, if non null.
     * Otherwise, it will return null and if the callback is not null, an updated request will be
     * asked to the system and the result will be returned to the callback
     *
     * @param callback a callback to get the location as soon as it's available
     * @return the last known location
     */
    @Nullable
    public static Location getLocation(@Nullable Callback callback) {
        return getLocation(callback, false);
    }

    /**
     * Returns the last known location if available, null otherwise. If {@param forceUpdate} is set
     * to true, the {@param callback} will not get the last known location, even if available, but
     * will anyway wait for an updated value. If {@param forceUpdate} is set to false, the callback
     * will get the last known location if available. It will get the updated value later otherwise.
     *
     * The update of the location is therefore triggered when {@param forceUpdate} is set to true
     * or when the callback is provided and the last known location is null.
     *
     * @param callback a callback to get the location when available or when updated
     * @param forceUpdate force an update and force callback to wait for updated value
     * @return the last known location
     */
    @Nullable
    public static Location getLocation(@Nullable Callback callback, boolean forceUpdate) {
        if (instance == null)
            throw new IllegalStateException("EasyLocation has not been initialized");

        if (callback != null || forceUpdate) {
            if (instance.mLastLocation == null || forceUpdate) {
                synchronized (instance.mUpdateCallbacksLock) {
                    instance.mUpdateCallbacks.add(new WeakReference<>(callback));
                }

                requireUpdate();
            } else {
                callback.onLocationUpdated(instance.mLastLocation);
            }
        }

        return instance.mLastLocation;
    }

    /**
     * Require an update of the last known location. When the location is updated, a
     * {@link NewLocationEvent} is posted if {@link EventBus} package is available
     */
    public static void requireUpdate() {
        if (instance == null)
            throw new IllegalStateException("EasyLocation has not been initialized");

        instance.handlerInstance.requireUpdate();
    }

    /**
     *
     * @param activity
     * @param stringId
     * @param callback
     * @return
     */
    public static Location getLocation(final Activity activity, @StringRes int stringId, @Nullable Callback callback) {
        if (LocationTools.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (stringId == 0) {
                requestPermission(activity);

                return null;
            } else {
                new AlertDialog.Builder(activity).setMessage(stringId)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                requestPermission(activity);
                                dialog.dismiss();
                            }
                        })
                        .setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).create().show();

                return null;
            }
        }

        LocationManager lm = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled;

        try {
            gps_enabled = lm != null && lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
            gps_enabled = false;
        }

        if (!gps_enabled) {
            new AlertDialog.Builder(activity)
                    .setMessage("Location service is disabled")
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                            Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            activity.startActivityForResult(myIntent, ACTIVITY_RESULT_ENABLE_PROVIDER);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                            paramDialogInterface.dismiss();
                        }
                    })
                    .create().show();

            return null;
        }

        return getLocation(callback);
    }

    public static Location onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults,
                                                      Activity activity, @StringRes int stringId, Callback callback)
            throws IllegalArgumentException {

        if (requestCode == INTENT_PERMISSION_LOCATION) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                return getLocation(activity, stringId, callback);
            } else {
                return null;
            }
        }

        throw new IllegalArgumentException();
    }

    public static Location onActivityResult(int requestCode, int resultCode, Intent data,
                                            Activity activity, @StringRes int stringId, Callback callback)
            throws IllegalArgumentException {

        if (requestCode == ACTIVITY_RESULT_ENABLE_PROVIDER) {
            return getLocation(activity, stringId, callback);
        }

        throw new IllegalArgumentException();
    }

    public interface Callback {
        void onLocationUpdated(Location location);
        void onError(Location location);
    }

    static boolean hasEventBus() {
        try {
            Class.forName("org.greenrobot.eventbus.EventBus");
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

    public static class NewLocationEvent {
        public final Location location;

        NewLocationEvent(Location location) {
            this.location = location;
        }
    }

    public static class NewVirtualLocationEvent {
        public final Location location;

        NewVirtualLocationEvent(Location location) {
            this.location = location;
        }
    }

    private static void requestPermission(Activity activity) {
        LocationTools.requestPermissions(activity,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                INTENT_PERMISSION_LOCATION);
    }

    private void updateLocation(Location location){
        mLastLocation = location;

        mPreferences.edit().putString(PREFERENCES_VIRTUAL_LOCATION, serializeLocation(location)).apply();

        synchronized (mUpdateCallbacksLock) {
            for (WeakReference<Callback> callbackR : mUpdateCallbacks) {
                final Callback callback = callbackR.get();

                if (callback != null) {
                    callback.onLocationUpdated(location);
                }
            }

            mUpdateCallbacks.clear();
        }

        if (hasEventBus()) {
            EventBus.getDefault().post(new NewLocationEvent(location));
        }
    }

    private static String serializeLocation(@Nullable Location location) {
        if (location == null)
            return "";

        return "v1/" +
                location.getProvider() + "/" +
                location.getTime() + "/" +
                location.getLatitude() + "/" +
                location.getLongitude() + "/" +
                location.getAccuracy() + "/" +
                location.getAltitude() + "/" +
                location.getSpeed();
    }

    @Nullable
    private static Location deserializeLocation(@Nullable String locationStr) {
        if (locationStr == null || locationStr.equals(""))
            return null;

        String[] els = locationStr.split("/");

        if (els.length != 8 || !els[0].equals("v1")) {
            return null;
        }

        Location location = new Location(els[1]);
        location.setTime(Long.valueOf(els[2]));
        location.setLatitude(Double.valueOf(els[3]));
        location.setLatitude(Double.valueOf(els[4]));
        location.setAccuracy(Float.valueOf(els[5]));
        location.setAltitude(Double.valueOf(els[6]));
        location.setSpeed(Float.valueOf(els[7]));

        return location;
    }
}
