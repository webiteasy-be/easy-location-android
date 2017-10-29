package be.webiteasy.android.location;


import android.location.Location;

public interface GeoAddressingServer {
    void code(final Location location, final Callback callback);

    interface Callback {
        /**
         * Return a clone of the init location, calling new Location(Location location)
         *
         * @param location
         */
        void onSuccess(AddressedLocation location);

        void onError(String message);
    }
}
