
package com.rodrigoamaro.takearide.activities;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMyLocationChangeListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.rodrigoamaro.takearide.R;
import com.rodrigoamaro.takearide.activities.ClientesFragment.OnReceivedClient;
import com.rodrigoamaro.takearide.serverapi.SmartTaxiAsync;
import com.rodrigoamaro.takearide.serverapi.SmartTaxiResponseAdapter;
import com.rodrigoamaro.takearide.serverapi.models.NotificationModel;
import com.rodrigoamaro.takearide.serverapi.models.TaxiModel;
import com.rodrigoamaro.takearide.service.LocationService;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Spinner;
import android.widget.Toast;

public class MainMapFragment extends FragmentActivity implements OnReceivedClient{

    /**
     * Note that this may be null if the Google Play services APK is not
     * available.
     */
    private GoogleMap mMap;
    private LocationService mBoundService;

    private String TAG = "MainMapFragment";

    

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service. Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            mBoundService = ((LocationService.LocalBinder) service).getService();

            // Tell the user about this for our demo.
            Toast.makeText(getApplicationContext(), "local_service_connected",
                    Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            mBoundService = null;
            Toast.makeText(getApplicationContext(), "local_service_disconnected",
                    Toast.LENGTH_SHORT).show();
        }
    };
    private boolean mIsBound;
    private Spinner mSpinner;
    
    protected boolean isFirstTime = false;

    void doBindService() {
        // Establish a connection with the service. We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
        bindService(new Intent(MainMapFragment.this, LocationService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    void doUnbindService() {
        if (mIsBound) {
            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        doUnbindService();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        //getActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.actionbar_bg));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_fragment_activity);
        setUpMapIfNeeded();
        doBindService();
        setupActionBar();

        mSpinner = (Spinner) findViewById(R.id.status_spinner);
        mSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                Toast.makeText(getApplicationContext(), mSpinner.getSelectedItem().toString() + " "+arg2, Toast.LENGTH_LONG).show();
                SmartTaxiAsync.getInstance(getApplicationContext()).changeStatus(arg2+1, new SmartTaxiResponseAdapter() {
                    @Override
                    public void changeStatusSuccess() {
                        Toast.makeText(getApplicationContext(), "Tu estado fue cambiado ", Toast.LENGTH_LONG).show();

                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(getApplicationContext(), "Error cambiando el estado", Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // TODO Auto-generated method stub

            }
        });

    }

    private void setupActionBar() {
        // setTheme(theme.whatever);

    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        // Disconnecting the client invalidates it.

        super.onStop();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play
     * services APK is correctly installed) and the map has not already been
     * instantiated.. This will ensure that we only ever call
     * {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt
     * for the user to install/update the Google Play services APK on their
     * device.
     * <p>
     * A user can return to this FragmentActivity after following the prompt and
     * correctly installing/updating/enabling the Google Play services. Since
     * the FragmentActivity may not have been completely destroyed during this
     * process (it is likely that it would only be stopped or paused),
     * {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the
        // map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the
     * camera. In this case, we just add a marker near Africa.
     * <p>
     * This should only be called once and when we are sure that {@link #mMap}
     * is not null.
     */
    private void setUpMap() {
        mMap.setMyLocationEnabled(true);
        mMap.setOnMyLocationChangeListener(new OnMyLocationChangeListener() {
            
            @Override
            public void onMyLocationChange(Location loc) {
                if(isFirstTime){
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(loc.getLatitude(), loc.getLongitude()), 15));
                    isFirstTime = !isFirstTime; 
                }
                
            }
        });
        //mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
    }

    @Override
    public void onReceivedClient(NotificationModel travel) {
        // TODO Auto-generated method stub
       
        Intent i=new Intent(this,TravelActivity.class);
        i.putExtra("latitude",travel.client.location.latitude);
        i.putExtra("longitude",travel.client.location.longitude);
        i.putExtra("address_name",travel.client.location.address_name);
        i.putExtra("name",travel.client.name);
        startActivity(i);
        
    }

}
