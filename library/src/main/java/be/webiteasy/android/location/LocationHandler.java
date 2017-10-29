package be.webiteasy.android.location;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import java.util.LinkedList;

/**
 * manage 2 LocationProviders and Listeners
 * https://developer.android.com/reference/android/location/GpsStatus.Listener.html
 * https://developer.android.com/reference/android/location/GnssStatus.Callback.html
 */
public class LocationHandler {

    public static final int MAX_UPDATE_PERIOD = 1000 * 60 * 10;

    // The app context
    final Context mContext;

    // The last recorded location
    private Location mLastLocation;

    // The openListener notifier
    protected LinkedList<Listener> mListeners = new LinkedList<>();

    // The current LocationProvider
    PassiveLocationProvider mPassiveLocationProvider;

    /**
     * Crée un service de localisation passif dans le context courrant. Ce service est également
     * un service de localisation avec adresses.
     *
     * @param context le context dans lequel est crée le service
     */
    public LocationHandler(Context context) {
        if (context == null)
            throw new NullPointerException();

        mContext = context.getApplicationContext();

        setupGoogleProvider();
    }

    /**
     * Let's use a PlayGoogle Location provider
     */
    private void setupGoogleProvider() {
        Log.i("EasyLocation", "LocationHandler#setupGoogleProvider()");
        mPassiveLocationProvider = new GoogleLocationProvider(mContext, new LocationProvider.Listener() {
            @Override
            public void onLocationChanged(Location location, LocationProvider locationProvider) {
                Log.i("EasyLocation", "LocationHandler GoogleProvider => onLocationChanged()");

                setLocation(location);
            }

            @Override
            public void onProviderStateChange(int state, LocationProvider locationProvider) {
                if (state == LocationProvider.STATE_ERROR) {
                    Log.e("EasyLocation", "LocationHandler GoogleProvider error");

                    // TODO only switch to native when I have something
                    mPassiveLocationProvider.setListener(null);
                    setupAndroidProvider();
                }
            }
        });
    }

    /**
     * Let's stop previous location provider and setup a native Android location provider
     */
    private void setupAndroidProvider() {
        Log.i("EasyLocation", "LocationHandler#setupAndroidProvider()");

        mPassiveLocationProvider = new NativeLocationProvider(mContext, new LocationProvider.Listener() {
            @Override
            public void onLocationChanged(Location location, LocationProvider locationProvider) {
                //Log.e("EasyLocation","LocationHandler NativeProvider => onLocationChanged("+location+")");

                setLocation(location);
            }

            @Override
            public void onProviderStateChange(int state, LocationProvider locationProvider) {
                //Log.e("EasyLocation","LocationHandler NativeProvider => on provider state change : "+state);
            }
        });
    }

    /**
     * Puisque le noyeau de localisation est passif, il est possible qu'on veuille recevoir une
     * update. L'appel à cette fonction permet d'envoyer une requête de localisation active au
     * service de localisation. Si aucun listener n'écoute le bean, la requête de localisation
     * active ne sera exécutée que lorsqu'un listener est ajouté.
     * <p>
     * La requête de localisation active n'est exécutée qu'une seule fois
     */
    public void requireUpdate() {
        Log.i("EasyLocation", "LocationHandler#requireUpdate()");

        mPassiveLocationProvider.requireUpdate();
    }

    public Location getLocation() {
        //Log.e("EasyLocation","LocationHandler#getLocation() => "+mLastLocation);

        return mLastLocation;
    }

    public boolean hasLocation() {
        //Log.e("EasyLocation","LocationHandler#hasLocation() => "+(mLastLocation != null));

        return mLastLocation != null;
    }

    /**
     * Met à jour la localisation du bean et alerte tous les listeners à condition que la nouvelle
     * localisation soit plus neuve que l'ancienne localisation.
     *
     * @param location
     */
    protected void setLocation(Location location) {
        //Log.e("EasyLocation","BaseLocationService::setLocation("+location+")");
        if (location == null)
            return;

        if (mLastLocation == null || mLastLocation.getTime() < location.getTime()) {
            mLastLocation = location;
            triggerLocationChanged();
        } else {
            //Log.e("EasyLocation","BaseLocationService::setLocation() not kept");
        }
    }

    /**
     * Ajoute un listener au noyeau de localisation et démarre les updates
     *
     * @param listener
     */
    public void addLocationListener(Listener listener) {
        if (listener == null)
            throw new NullPointerException("Listener cannot be null");

        if (!mListeners.contains(listener))
            mListeners.add(listener);
    }

    /**
     * retire un listener au noueau de localisation et arrête les updates s'il n'y a plus de
     * listener
     *
     * @param listener
     */
    public void removeLocationListener(Listener listener) {
        mListeners.remove(listener);
    }

    /**
     * Active les listeners à propos d'une erreur du noyeau de localisation
     *
     * @param message le message d'erreur
     */
    protected void triggerLocationError(final String message) {
        LocationErrorEvent event = new LocationErrorEvent() {
            @Override
            public LocationHandler getLocationService() {
                return LocationHandler.this;
            }

            @Override
            public String getMessage() {
                return message;
            }
        };

        for (Listener listener : mListeners) {
            listener.onLocationError(event);
        }
    }


    /**
     * active les listeners à propos d'une nouvelle localisation disponible
     */
    protected void triggerLocationChanged() {
        //Log.e("EasyLocation","BaseLocationService::triggerLocationChanged()");

        LocationChangedEvent event = new LocationChangedEvent() {
            @Override
            public LocationHandler getLocationService() {
                return LocationHandler.this;
            }

            @Override
            public Location getLocation() {
                return LocationHandler.this.getLocation();
            }
        };

        for (Listener listener : mListeners) {
            listener.onLocationChanged(event);
        }
    }


    public interface Listener {
        void onLocationChanged(LocationChangedEvent event);

        void onLocationError(LocationErrorEvent event);
    }

    public interface OpenEvent {

    }


    public interface LocationErrorEvent extends OpenEvent {
        LocationHandler getLocationService();

        String getMessage();
    }

    public interface LocationChangedEvent extends OpenEvent {
        LocationHandler getLocationService();

        Location getLocation();
    }
}
