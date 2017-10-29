package be.webiteasy.android.location;

import android.location.Location;
import android.support.annotation.Nullable;
import android.util.Log;


public abstract class BaseLocationProvider implements LocationProvider {


    // Current state
    private int mState = STATE_INITIALIZING;

    // Last known location
    private Location mLocation;

    // Listeners List
    private Listener mListener;

    @Nullable
    @Override
    public Location getLocation() {
        return mLocation;
    }

    @Override
    public void setListener(Listener l) {
        mListener = l;
    }


    @Override
    public int getState() {
        return mState;
    }

    protected void setState(int state) {
        if (mState != state) {
            mState = state;
            if (mListener != null)
                mListener.onProviderStateChange(state, this);
        }
    }

    /**
     * Update le status à "WORKING", enregistre la dernière localisation et notifie celle-ci
     *
     * @param location
     */
    protected void setLocation(Location location) {
        Log.i("EasyLocation", "BaseLocationProvider#setLocation(" + location + ")");

        if (location == null && mLocation != null)
            return;

        setState(STATE_STARTED);
        mLocation = location;

        if (mListener != null) {
            mListener.onLocationChanged(location, this);
        }
    }

}
