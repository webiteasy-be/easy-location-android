package be.webiteasy.android.location;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

/**
 * TODO This is a passive location provider ... we should be able to switch to active mode
 */
public class GoogleLocationProvider extends BaseLocationProvider implements PassiveLocationProvider {

    private final LocationRequest passiveLocationRequest = new LocationRequest()
            .setPriority(LocationRequest.PRIORITY_NO_POWER);

    private final LocationRequest activeLocationRequest = new LocationRequest()
            .setInterval(4000)
            .setFastestInterval(1000)
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

    private final Context mContext;

    // The GoogleApiClient ref
    private final GoogleApiClient mGoogleApiClient;

    private boolean mUpdateRequestPending = false;

    /**
     * Prévient si une update request a été émise mais n'a pas pu être lancée car l'API n'était
     * pas prêt
     */
    private boolean mPreConnectionUpdateRequired = false;

    final LocationListener mGoogleLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            Log.i("EasyLocation", "GoogleLocationProvider.LocationListener#onLocationChanged(" + location + ")");

            setLocation(location);

            mUpdateRequestPending = false;
            mPreConnectionUpdateRequired = false;

            try {
                LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, mGoogleLocationListener);
                LocationServices.FusedLocationApi.requestLocationUpdates(
                        mGoogleApiClient, passiveLocationRequest, mGoogleLocationListener);

                setState(STATE_STARTED);
            } catch (SecurityException e) {
                setState(STATE_ERROR);
            }
        }
    };

    /**
     *
     */
    public GoogleLocationProvider(Context context, Listener listener) {
        mContext = context;

        setListener(listener);

        final ConnectionCallback connectionCallback = new ConnectionCallback();

        mGoogleApiClient = new GoogleApiClient.Builder(mContext)
                .addConnectionCallbacks(connectionCallback)
                .addOnConnectionFailedListener(connectionCallback)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void requireUpdate() {
        Log.i("EasyLocation", "GoogleLocationProvider#requireUpdate()");

        if (mUpdateRequestPending)
            return;

        Log.i("EasyLocation", "GoogleLocationProvider#requireUpdate() no request pending");

        if (!mGoogleApiClient.isConnected()) {
            Log.i("EasyLocation", "GoogleLocationProvider#requireUpdate() not connected");
            mPreConnectionUpdateRequired = true;
            return;
        }
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, mGoogleLocationListener);

        try {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, mGoogleLocationListener);
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, activeLocationRequest, mGoogleLocationListener);

            mUpdateRequestPending = true;

            Log.i("EasyLocation", "GoogleLocationProvider#requireUpdate() update started");

            setState(STATE_STARTED);
        } catch (SecurityException e) {
            Log.i("EasyLocation", "GoogleLocationProvider#requireUpdate() SecurityException " + e);

            setState(STATE_ERROR);
        }
    }

    @Override
    public void stop() {
        if (mGoogleApiClient.isConnected())
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, mGoogleLocationListener);

        setState(STATE_STOPPED);
    }

    @Override
    public void start() {
        Log.i("EasyLocation", "GoogleLocationProvider#start()");

        if (getState() == STATE_STARTED)
            return;

        try {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, mGoogleLocationListener);
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, passiveLocationRequest, mGoogleLocationListener);

            Log.i("EasyLocation", "GoogleLocationProvider#start() Started !");

            setState(STATE_STARTED);
        } catch (SecurityException e) {
            Log.e("EasyLocation", "GoogleLocationProvider#start() SecurityException " + e);

            setState(STATE_ERROR);
        }
    }

    public class ConnectionCallback implements GoogleApiClient.ConnectionCallbacks,
            GoogleApiClient.OnConnectionFailedListener {
        @Override
        public void onConnected(@Nullable Bundle bundle) {
            Log.i("EasyLocation", "GoogleLocationProvider.ConnectionCallback#onConnected()");

            try {
                Location l = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

                if (l != null) {
                    setLocation(l);
                    mPreConnectionUpdateRequired = false;
                } else if (mPreConnectionUpdateRequired) {
                    requireUpdate();
                }

                start();
            } catch (SecurityException e) {
                Log.e("EasyLocation", "GoogleLocationProvider.ConnectionCallback#onConnected() SecurityException " + e);
                setState(STATE_ERROR);
            }
        }

        @Override
        public void onConnectionSuspended(int i) {
            stop();
        }

        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            Log.e("EasyLocation", "GoogleLocationProvider.ConnectionCallback#onConnectionFailed(" + connectionResult.toString() + ")");
            setState(STATE_ERROR);
        }
    }
}
