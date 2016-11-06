package com.example.frederickmacgregor.mapapp;

import android.app.Activity;
import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONException;
import org.json.JSONObject;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class GameScreen extends FragmentActivity implements OnMapReadyCallback {

    private static final int REQUEST_LOGIN = 0;
    private GoogleMap mMap;
    private Socket mSocket;
    private Boolean isConnected = true;
    private String mUsername;
    private String mUserId;
    private String mGameId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_game_screen); //orriginal from template
          setContentView(R.layout.activity_game_screen);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        SocketApplication app = (SocketApplication) this.getApplication();
        mSocket = app.getSocket();
        mSocket.on("new message", onNewMessage);
        mSocket.connect();
        startSignIn();
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
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
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
            } catch (JSONException e) {
                return;
            }
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

    }
}
