
package com.rodrigoamaro.takearide.activities;

import java.io.IOException;
import java.sql.Timestamp;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.rodrigoamaro.takearide.R;
import com.rodrigoamaro.takearide.R.id;
import com.rodrigoamaro.takearide.R.layout;
import com.rodrigoamaro.takearide.serverapi.SmartTaxiResponseAdapter;
import com.rodrigoamaro.takearide.serverapi.SmartTaxiAsync;
import com.rodrigoamaro.takearide.serverapi.models.TastypieResponse;
import com.rodrigoamaro.takearide.serverapi.models.TaxiModel;
import com.rodrigoamaro.takearide.serverapi.models.TokenResponse;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class LoginActivity extends Activity {

    private static final String TAG = "LoginActivity";
    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private static final String PROPERTY_ON_SERVER_EXPIRATION_TIME = "onServerExpirationTimeMs";
    /**
     * Default lifespan (7 days) of a reservation until it is considered
     * expired.
     */
    public static final long REGISTRATION_EXPIRY_TIME_MS = 1000 * 3600 * 24 * 7;

    public static final String SENDER_ID = "1028559923355";

    /**
     * @see android.app.Activity#onCreate(Bundle)
     */
    private Button mLoginOkButton;
    private EditText mUser;
    private EditText mPassword;
    private GoogleCloudMessaging gcm;
    private String regid;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
        mLoginOkButton = (Button) findViewById(R.id.login_button);
        mUser = (EditText) findViewById(R.id.login_username);
        mPassword = (EditText) findViewById(R.id.login_password);
        mLoginOkButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                doLogin(mUser.getText().toString(), mPassword.getText().toString());
            }
        });
        context = getApplicationContext();
        regid = getRegistrationId(getApplicationContext());

        if (regid.length() == 0) {
            registerBackground();
        }
        gcm = GoogleCloudMessaging.getInstance(this);

    }

    /**
     * Gets the current registration id for application on GCM service.
     * <p>
     * If result is empty, the registration has failed.
     * 
     * @return registration id, or empty string if the registration is not
     *         complete.
     */
    private String getRegistrationId(Context context) {
        final SharedPreferences prefs = getGCMPreferences(context);
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.length() == 0) {
            Log.v(TAG, "Registration not found.");
            return "";
        }
        // check if app was updated; if so, it must clear registration id to
        // avoid a race condition if GCM sends a message
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion || isRegistrationExpired()) {
            Log.v(TAG, "App version changed or registration expired.");
            return "";
        }
        return registrationId;
    }

    /**
     * @return Application's {@code SharedPreferences}.
     */
    private SharedPreferences getGCMPreferences(Context context) {
        return getSharedPreferences(LoginActivity.class.getSimpleName(),
                Context.MODE_PRIVATE);
    }

    /**
     * @return Application's version code from the {@code PackageManager}.
     */
    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    /**
     * Checks if the registration has expired.
     * <p>
     * To avoid the scenario where the device sends the registration to the
     * server but the server loses it, the app developer may choose to
     * re-register after REGISTRATION_EXPIRY_TIME_MS.
     * 
     * @return true if the registration has expired.
     */
    private boolean isRegistrationExpired() {
        final SharedPreferences prefs = getGCMPreferences(getApplicationContext());
        // checks if the information is not stale
        long expirationTime =
                prefs.getLong(PROPERTY_ON_SERVER_EXPIRATION_TIME, -1);
        return System.currentTimeMillis() > expirationTime;
    }

    /**
     * Registers the application with GCM servers asynchronously.
     * <p>
     * Stores the registration id, app versionCode, and expiration time in the
     * application's shared preferences.
     */
    private void registerBackground() {

        new AsyncTask<Void, String, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String msg = "";
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(context);
                    }
                    regid = gcm.register(SENDER_ID);
                    msg = "Device registered, registration id=" + regid;
                    Log.d(TAG, "reg_id: " + regid);
                    setRegistrationId(context, regid);
                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                }
                return msg;
            }

            @Override
            protected void onPostExecute(String msg) {
                // mDisplay.append(msg + "\n");
                Log.d(TAG, "reg_id: " + msg);
            }

        }.execute();
    }

    /**
     * Stores the registration id, app versionCode, and expiration time in the
     * application's {@code SharedPreferences}.
     * 
     * @param context application's context.
     * @param regId registration id
     */
    private void setRegistrationId(Context context, String regId) {
        final SharedPreferences prefs = getGCMPreferences(context);
        int appVersion = getAppVersion(context);
        Log.v(TAG, "Saving regId on app version " + appVersion);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        long expirationTime = System.currentTimeMillis() + REGISTRATION_EXPIRY_TIME_MS;

        Log.v(TAG, "Setting registration expiry time to " +
                new Timestamp(expirationTime));
        editor.putLong(PROPERTY_ON_SERVER_EXPIRATION_TIME, expirationTime);
        editor.commit();
    }

    private void doLogin(String username, String password) {

        SmartTaxiAsync.getInstance(getApplicationContext()).doLogin(username, password, new SmartTaxiResponseAdapter() {
            @Override
            public void gotLoginSuccess(TokenResponse response) {
                SmartTaxiAsync.getInstance(getApplicationContext()).getTaxisDetail(new SmartTaxiResponseAdapter() {
                    @Override
                    public void gotTaxis(TastypieResponse<TaxiModel> taxis) {
                        Toast.makeText(getApplicationContext(), "Bienvenido " + taxis.objects.get(0).license_plate, Toast.LENGTH_LONG).show();

                        SmartTaxiAsync.getInstance(getApplicationContext()).changeStatus(TaxiModel.STATUS_AVAILABLE, new SmartTaxiResponseAdapter() {
                            @Override
                            public void changeStatusSuccess() {
                                Toast.makeText(getApplicationContext(), "Tu estado es disponible", Toast.LENGTH_LONG).show();

                            }

                            @Override
                            public void onError(Exception e) {
                                Toast.makeText(getApplicationContext(), "Error cambiando el estado", Toast.LENGTH_LONG).show();
                            }
                        });
                        String deviceId = Secure.getString(getApplicationContext().getContentResolver(),Secure.ANDROID_ID);
                        SmartTaxiAsync.getInstance(getApplicationContext()).setDeviceData(regid, deviceId, new SmartTaxiResponseAdapter() {

                            @Override
                            public void addDeviceSuccess() {
                                Toast.makeText(getApplicationContext(), "Registrado a mensajeria push", Toast.LENGTH_LONG).show();
                            }

                            @Override
                            public void onError(Exception e) {
                                Toast.makeText(getApplicationContext(), "error registrando mensajeria push", Toast.LENGTH_LONG).show();
                            }
                        });

                        Intent i = new Intent(getApplicationContext(), MainMapFragment.class);
                        startActivity(i);
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(getApplicationContext(), "Error obteniendo taxis", Toast.LENGTH_LONG).show();
                    }
                });

            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(getApplicationContext(), "Error en login", Toast.LENGTH_LONG).show();
            }
        });
    }

}
