package be.webiteasy.android.location;

import android.content.Context;
import android.location.Location;


public class AddressedLocationHandler extends LocationHandler {

    GeoAddressingServer.Callback mAddressCodingCallback;

    GeoAddressingServer mGeoAddressingServer;

    private AddressedLocation mAddressedLocation;
    private int mAddressCodePeriod = 1000 * 60 * 10;
    private boolean mShouldCodeAddress = false;
    private boolean mIsCoding = false;

    /**
     * Crée un service de localisation passif dans le context courrant. Ce service est également
     * un service de localisation avec adresses.
     *
     * @param context le context dans lequel est crée le service
     */
    public AddressedLocationHandler(Context context, GeoAddressingServer geoAddressingServer) {
        super(context);

        mGeoAddressingServer = geoAddressingServer;

        mAddressCodingCallback = new GeoAddressingServer.Callback() {
            @Override
            public void onSuccess(AddressedLocation location) {
                //Log.e("EasyLocation", "AddressedLocationHandler server callback success : " + location);
                setAddressedLocation(location);
                mIsCoding = false;
            }

            @Override
            public void onError(String message) {
                //Log.e("EasyLocation","AddressedLocationHandler server callback error : "+message);

                mIsCoding = false;
            }
        };
    }

    /**
     * L'appel de cette fonction démarre tous les processus nécessaires afin d'obtenir à terme
     * une mise à jour de l'AddressedLocation
     */
    public void requireAddressUpdate() {
        //Log.e("EasyLocation","AddressedLocationHandler::requireAddressUpdate()");

        if (mGeoAddressingServer == null)
            throw new IllegalStateException("I have no reverse geocoding server");

        final Location location = mPassiveLocationProvider.getLocation();
        if (location == null) {
            shouldCodeAddress(true);
            mPassiveLocationProvider.requireUpdate();
        } else {
            shouldCodeAddress(false);
            codeAddress();
        }
    }

    public AddressedLocation getAddressedLocation() {
        //Log.e("EasyLocation","AddressedLocationHandler::getAddressedLocation() => "+mAddressedLocation);
        return mAddressedLocation;
    }

    public void shouldCodeAddress(boolean b) {
        //Log.e("EasyLocation","AddressedLocationHandler::shouldCodeAddress() => "+b);
        mShouldCodeAddress = b;
    }

    @Override
    protected void setLocation(Location location) {
        //Log.e("EasyLocation","AddressedLocationHandler::setLocation()");

        if (location == null)
            return;

        super.setLocation(location);

        if (mAddressedLocation == null ||
                mShouldCodeAddress ||
                System.currentTimeMillis() - mAddressedLocation.getTime() > mAddressCodePeriod) {

            shouldCodeAddress(false);
            codeAddress();
        }
    }

    protected void setAddressedLocation(AddressedLocation location) {
        if (mAddressedLocation == null || mAddressedLocation.getTime() < location.getTime()) {
            shouldCodeAddress(false);
            mAddressedLocation = location;
            triggerNewAddressedLocationAvailable();
        }
    }

    /**
     * Call the addressCodeService
     */
    private void codeAddress() {
        //Log.e("EasyLocation","AddressedLocationHandler::codeAddress()");

        if (!mIsCoding) {
            mIsCoding = true;
            mGeoAddressingServer.code(getLocation(), mAddressCodingCallback);
        }
    }

    protected void triggerNewAddressedLocationAvailable() {
        AddressedLocationChangedEvent event = new AddressedLocationChangedEvent() {
            @Override
            public AddressedLocationHandler getAddressedLocationService() {
                return AddressedLocationHandler.this;
            }

            @Override
            public AddressedLocation getAddressedLocation() {
                return AddressedLocationHandler.this.getAddressedLocation();
            }
        };

        for (LocationHandler.Listener listener : mListeners) {
            if (listener instanceof Listener)
                ((Listener) listener).onAddressedLocationChanged(event);
        }
    }

    public interface Listener extends LocationHandler.Listener {
        void onAddressedLocationChanged(AddressedLocationChangedEvent event);
    }

    public interface AddressedLocationChangedEvent extends OpenEvent {
        AddressedLocationHandler getAddressedLocationService();

        AddressedLocation getAddressedLocation();
    }
}
