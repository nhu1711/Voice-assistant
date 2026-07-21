package com.example.voiceassistant.location;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;

import androidx.core.content.ContextCompat;

import java.util.concurrent.Executor;

public class LocationManagerHelper {

    public interface LocationCallback {
        void onLocationAvailable(double latitude, double longitude);

        void onLocationUnavailable();

        void onError();
    }

    private static final long LOCATION_TIMEOUT_MS = 10000L;

    private final Context appContext;
    private final LocationManager locationManager;

    public LocationManagerHelper(Context context) {
        appContext = context.getApplicationContext();
        locationManager = (LocationManager) appContext.getSystemService(Context.LOCATION_SERVICE);
    }

    public void getCurrentLocation(LocationCallback callback) {
        if (callback == null) {
            return;
        }
        if (locationManager == null || !hasLocationPermission()) {
            callback.onError();
            return;
        }

        String provider = chooseProvider();
        if (provider == null) {
            reportLastKnownOrUnavailable(callback);
            return;
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                requestCurrentLocation(provider, callback);
            } else {
                requestSingleLocationUpdate(provider, callback);
            }
        } catch (SecurityException exception) {
            callback.onError();
        } catch (Exception exception) {
            reportLastKnownOrUnavailable(callback);
        }
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private String chooseProvider() {
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            return LocationManager.GPS_PROVIDER;
        }
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            return LocationManager.NETWORK_PROVIDER;
        }
        return null;
    }

    private void requestCurrentLocation(String provider, LocationCallback callback) {
        CancellationSignal cancellationSignal = new CancellationSignal();
        Executor executor = command -> new Handler(Looper.getMainLooper()).post(command);
        locationManager.getCurrentLocation(provider, cancellationSignal, executor, location -> {
            if (location != null) {
                callback.onLocationAvailable(location.getLatitude(), location.getLongitude());
            } else {
                reportLastKnownOrUnavailable(callback);
            }
        });
    }

    private void requestSingleLocationUpdate(String provider, LocationCallback callback) {
        Handler handler = new Handler(Looper.getMainLooper());
        LocationListener listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                locationManager.removeUpdates(this);
                handler.removeCallbacksAndMessages(null);
                if (location != null) {
                    callback.onLocationAvailable(location.getLatitude(), location.getLongitude());
                } else {
                    reportLastKnownOrUnavailable(callback);
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(String provider) {
            }

            @Override
            public void onProviderDisabled(String provider) {
            }
        };

        handler.postDelayed(() -> {
            locationManager.removeUpdates(listener);
            reportLastKnownOrUnavailable(callback);
        }, LOCATION_TIMEOUT_MS);
        locationManager.requestSingleUpdate(provider, listener, Looper.getMainLooper());
    }

    private void reportLastKnownOrUnavailable(LocationCallback callback) {
        Location location = getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location == null) {
            location = getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }

        if (location != null) {
            callback.onLocationAvailable(location.getLatitude(), location.getLongitude());
        } else {
            callback.onLocationUnavailable();
        }
    }

    private Location getLastKnownLocation(String provider) {
        try {
            if (locationManager.isProviderEnabled(provider)) {
                return locationManager.getLastKnownLocation(provider);
            }
        } catch (SecurityException exception) {
            return null;
        } catch (Exception exception) {
            return null;
        }
        return null;
    }
}
