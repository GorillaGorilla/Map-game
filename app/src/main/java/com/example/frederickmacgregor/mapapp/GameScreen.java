package com.example.frederickmacgregor.mapapp;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class GameScreen extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks , GoogleApiClient.OnConnectionFailedListener {

    private static final int REQUEST_LOGIN = 0;
    private GoogleMap mMap;
    private Socket mSocket;
    private Boolean isConnected = true;
    private String mUsername;
    private String mUserId;
    private String mGameId = null;
    private LocationManager mLocationManager;
    private Long LOCATION_REFRESH_TIME = 1l;
    private float LOCATION_REFRESH_DISTANCE = 0.5f;
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private Location playerLocation;
    private Marker playerMarker;
    private GoogleApiClient mGoogleApiClient;
    private ArrayList<LatLng> entities = new ArrayList<LatLng>();
    private final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(final Location location) {
            //your code here
            Log.d("%%% location listened", "still here...");
            playerLocation = location;
            if ( mGameId == null){
                sendLocation(playerLocation);
            }

            Log.d("%%%","Sent player location");
            render();
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle b){
            Log.d("%%% umm", "status changed?");
        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_game_screen); //orriginal from template
          setContentView(R.layout.test_layout);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        SocketApplication app = (SocketApplication) this.getApplication();
        mSocket = app.getSocket();
        mSocket.on("new message", onNewMessage);

        mSocket.connect();
        setupLocation();
        startSignIn();

        Handler handler = new Handler(Looper.getMainLooper());
        boolean post = handler.post(new Runnable() {
            @Override
            public void run() {
                // your UI code here
                mSocket.on("gameState", onGameState);
            }

        });

    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        // Add a marker in Sydney and move the camera
    }

    private Emitter.Listener onConnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.d("%%% onConnect", "nothing");
            if(!isConnected) {
                if(null!=mUsername){
                    mSocket.emit("add user", mUsername);
                }
                isConnected = true;
            }
        }
    };

    private Emitter.Listener onDisconnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.d("%%% onDisconnect", "nothing");
        }
    };

    private Emitter.Listener onConnectError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.d("%%% onConnectError", "nothing");
        }
    };

    private Emitter.Listener onNewMessage = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            Log.d("%%% onNewMessage", "nothing");
            JSONObject data = (JSONObject) args[0];
            String username;
            String message;
            try {
                username = data.getString("username");
                message = data.getString("message");
                Log.d("%%% onNewMessage", username);
                Log.d("%%% onNewMessage", message);
            } catch (JSONException e) {
                return;
            }
        }
    };

    private Emitter.Listener onGameState = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            Log.d("%%% onGameState", "nothing");
            JSONObject data = (JSONObject) args[0];
            JSONArray players;
            JSONArray assets;
            try {
                players = data.getJSONArray("players");
                assets = data.getJSONArray("assets");

                HashMap<String, String> gamePlayers = new HashMap<String,String>();
                entities.clear();
                for(int i=0; i<players.length(); i++){
                    String x = players.getJSONObject(i).getString("x");
                    String y = players.getJSONObject(i).getString("y");
                    LatLng tempLocation = new LatLng(Double.parseDouble(x),Double.parseDouble(y));
                    entities.add(tempLocation);
                }

                for(int i=0; i<assets.length(); i++){
                    String x = assets.getJSONObject(i).getString("x");
                    String y = assets.getJSONObject(i).getString("y");
                    LatLng tempLocation = new LatLng(Double.parseDouble(x),Double.parseDouble(y));
                    entities.add(tempLocation);
                }


            } catch (JSONException e) {
                return;
            }
            render();
        }
    };

    private void startSignIn() {
        mUsername = null;
        Intent intent = new Intent(this, LoginActivity.class);
        startActivityForResult(intent, REQUEST_LOGIN);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (Activity.RESULT_OK != resultCode) {
            Log.d("%%% onActivtyResult", "Result not ok");
            this.finish();
            return;
        }

        mUsername = data.getStringExtra("username");
        mUserId = data.getStringExtra("userId");
        mGameId = data.getStringExtra("gameId");
        int numUsers = data.getIntExtra("numUsers", 1);
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        mGoogleApiClient.connect();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        Log.d("%%%", "onRequestPermissionResult");
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("%%% requested", "permission receivedd..... ");
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    try{
                        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_REFRESH_TIME,
                                LOCATION_REFRESH_DISTANCE,  mLocationListener);
                        Log.d("%%% LocMan", "Location Updates requested");
                    }catch(SecurityException e){
                        Log.d("%%% Sec Excpt", "helooooo");
                    }

                } else {
                    Log.d("%%% requested", "permission not..... ");
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }



            // other 'case' lines to check for other
            // permissions this app might request
        }
    }



    private void setupLocation(){

        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        Boolean test;
        test = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED;
        Log.d("%%% ", String.valueOf(test));
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d("%%% contextCom", "checking contextCompat");
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                Log.d("%%% contextCom", "checking activty");
                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {
                Log.d("%%% contextCom", "requesting permission");
                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }

        }
        //            request location updates as we have permission. Otherwise the listener wont be set
// in the "already have permission" case
        try{
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_REFRESH_TIME,
                    LOCATION_REFRESH_DISTANCE,  mLocationListener);
            Log.d("%%% LocMan", "Location Updates requested");
        }catch(SecurityException e){
            Log.d("%%% Sec Excpt", "helooooo");
        }

    }

    @Override
    public void onConnected(Bundle bundle) {
        playerLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (playerLocation != null) {
            LatLng temp = new LatLng(playerLocation.getLatitude(), playerLocation.getLongitude());
            playerMarker = mMap.addMarker(new MarkerOptions().position(temp).title("Player"));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(temp));
            sendLocation(playerLocation);
        }
    }

    private synchronized void sendLocation(Location loc){
        Log.d("%%%","send location called");
        JSONObject location = new JSONObject();
        JSONObject event = new JSONObject();
        try {
            event.put("gameId", mGameId);
            location.put("userId", mUserId);
            location.put("x", loc.getLatitude());
            location.put("y", loc.getLongitude());
            event.put("location", location);
            Log.d("%%%","emitting location");
            mSocket.emit("location", event);
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    protected void onDestroy() {
        super.onDestroy();

        mSocket.disconnect();

        mSocket.off("new message", onNewMessage);
        mSocket.off("gameState", onGameState);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    private synchronized void  render(){
        Log.d("%%% Render called", "hi");
        mMap.clear();
        LatLng playerLatLng = new LatLng(playerLocation.getLatitude(),playerLocation.getLongitude());
        renderPlayer(playerLatLng);
        for (LatLng entity: entities) {
            Log.d("%%%", "Render entity");
            renderEntity(entity);
        }
    }

    private void renderPlayer(LatLng playerLocation){
        Log.d("%%%", "Render player");
        renderEntity(playerLocation);
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(temploc));
    }

    public void renderEntity(LatLng entityLocation){
        mMap.addMarker(new MarkerOptions().position(entityLocation).title("Current Location"));
    }

    private ArrayList<Marker> createMarkerArray(ArrayList<Location> entities){
        ArrayList<Marker> markers = new ArrayList<Marker>();

        for (Location loc : entities){
            LatLng temploc = new LatLng(playerLocation.getLatitude(),playerLocation.getLongitude());
            Marker m = mMap.addMarker(new MarkerOptions().position(temploc).title("Current Location"));
            markers.add(m);
        }
        return markers;
    }

    private Marker createPlayerMarker(Location location){
        LatLng temploc = new LatLng(playerLocation.getLatitude(),playerLocation.getLongitude());
        return mMap.addMarker(new MarkerOptions().position(temploc).title("Current Location"));
    }
}
