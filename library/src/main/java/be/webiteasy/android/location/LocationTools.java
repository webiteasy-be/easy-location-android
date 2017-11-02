package be.webiteasy.android.location;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Process;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;

public class LocationTools {

    public static boolean hasLocationProviderEnabled(Context context, boolean onlyDedicated) {
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        boolean network_enabled = false;

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
        }

        if (!onlyDedicated) {
            try {
                network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            } catch (Exception ex) {
            }
        }

        return gps_enabled || network_enabled;
    }

    public static String toUrlQuery(Location location) {
        if (location == null)
            return "";

        return location.getLatitude() + "," + location.getLongitude();
    }

    public static double distance(Location location1, Location location2) {
        return distanceDegrees(location1.getLatitude(), location1.getLongitude(),
                location2.getLatitude(), location2.getLongitude());
    }

    public static double distanceDegrees(double geo1_lat, double geo1_lon, double geo2_lat, double geo2_lon) {
        int R = 6371000; // metres

        double lat1_rad = Math.toRadians(geo1_lat);
        double lat2_rad = Math.toRadians(geo2_lat);
        double var_lat = Math.toRadians(geo2_lat - geo1_lat);
        double var_lon = Math.toRadians(geo2_lon - geo1_lon);

        double a = Math.sin(var_lat / 2) * Math.sin(var_lat / 2) +
                Math.cos(lat1_rad) * Math.cos(lat2_rad) *
                        Math.sin(var_lon / 2) * Math.sin(var_lon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    /**
     * Using location service require permissions. This returns true if you have required permissions
     * to use it
     *
     * @return
     */
    public static boolean checkPermissions(Context context) {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    static boolean hasActivityCompat() {
        try {
            Class.forName("android.support.v4.app.ActivityCompat");
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }


    public static int checkSelfPermission(@NonNull Context context, @NonNull String permission) {
        if (permission == null) {
            throw new IllegalArgumentException("permission is null");
        }

        return context.checkPermission(permission, android.os.Process.myPid(), Process.myUid());
    }

    public static void requestPermissions(final @NonNull Activity activity,
                                          final @NonNull String[] permissions, final @IntRange(from = 0) int requestCode) {

        if (hasActivityCompat()) {
            ActivityCompat.requestPermissions(activity, permissions, requestCode);
        } else {
            if (Build.VERSION.SDK_INT >= 23) {
                activity.requestPermissions(permissions, requestCode);
            }
        }
    }
}
