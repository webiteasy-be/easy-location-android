package be.webiteasy.android.location;

import android.location.Location;
import android.support.annotation.Nullable;

/**
 * Le location provider permet de fournir des localisations
 */
public interface LocationProvider {
    /**
     *  INIT ------->     STARTED   <----------- start() -----
     *                   |       ^                            |
     *                   |       |                            |      when
     *                 stop()  start()      when error ---> ERROR    completely ---> DOWN
     *                   |       |                                   down
     *                   v       |
     *                    STOPPED
     *
     */
    int STATE_INITIALIZING = 0;
    int STATE_STARTED = 1;
    int STATE_STOPPED = 2;
    int STATE_ERROR = 3;
    int STATE_DOWN = 4;

    interface Listener {
        void onLocationChanged(Location location, LocationProvider locationProvider);

        void onProviderStateChange(int state, LocationProvider locationProvider);
    }

    /**
     * return the last known location ... or null if no location
     *
     * @return the last known location
     */
    @Nullable
    Location getLocation();

    /**
     * return the status
     *
     * @return
     */
    int getState();

    /**
     * Add a listener to this provider
     *
     * @param l the listener to add
     */
    void setListener(Listener l);

    void start();

    void stop();
}
