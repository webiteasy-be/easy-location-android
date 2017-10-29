package be.webiteasy.android.location;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.util.Log;


/**
 * TODO can change passive or not
 */
public class NativeLocationProvider extends BaseLocationProvider implements PassiveLocationProvider {

    private final LocationManager mLocationManager;

    private boolean mUpdateRequired = false;

    LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            //Log.e("EasyLocation","NativeLocationProvider#LocationListner#onLocationChanged("+location+")");

            setLocation(location);

            try {
                mLocationManager.removeUpdates(mLocationListener);
                mLocationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER,
                        0, 0, mLocationListener);

                mUpdateRequired = false;
            } catch (SecurityException e) {
                setState(STATE_ERROR);
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            //Log.e("EasyLocation","NativeLocationProvider#LocationListner#onstatusChanged("+provider+", "+status+")");

            if (status == LocationProvider.OUT_OF_SERVICE || status == LocationProvider.TEMPORARILY_UNAVAILABLE)
                setState(STATE_ERROR);
            else
                setState(STATE_STARTED);
        }

        @Override
        public void onProviderEnabled(String provider) {
            //Log.e("EasyLocation","NativeLocationProvider#LocationListner#onProviderEnabled");

        }

        @Override
        public void onProviderDisabled(String provider) {
            //Log.e("EasyLocation","NativeLocationProvider#LocationListner#onProviderDisabled()");
        }
    };

    /**
     * J'ai ajouté une référence vers un listener car l'initialisation (ou la non initialisation)
     * se fait directement à l'appel du constructeur => il faut pouvoir être informés
     *
     * @param context
     * @param listener
     */
    public NativeLocationProvider(Context context, Listener listener) {
        setListener(listener);

        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        start();
    }

    @Override
    public void requireUpdate() {
        Log.i("EasyLocation", "NativeLocationProvider#requireUpdate()");

        if (mUpdateRequired)
            return;

        Log.i("EasyLocation", "NativeLocationProvider#requireUpdate() no request pending");

        try {
            mLocationManager.removeUpdates(mLocationListener);
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 2, mLocationListener);
            setState(STATE_STARTED);
            mUpdateRequired = true;
        } catch (SecurityException e) {
            setState(STATE_ERROR);
        }
    }

    @Override
    public void stop() {
        try {
            mLocationManager.removeUpdates(mLocationListener);
            setState(STATE_STOPPED);
        } catch (SecurityException e) {
            setState(STATE_ERROR);
        }
    }

    @Override
    public void start() {
        if (getState() == STATE_STARTED)
            return;

        try {
            Location l = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            if (l == null) {
                l = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }

            if (l == null) {
                l = mLocationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
            }

            mLocationManager.removeUpdates(mLocationListener);
            mLocationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 0, 0, mLocationListener);

            setState(STATE_STARTED);
            setLocation(l);
        } catch (SecurityException e) {
            setState(STATE_ERROR);
        }
    }
}
