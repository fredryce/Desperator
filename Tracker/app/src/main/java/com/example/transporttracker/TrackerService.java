package com.example.transporttracker;



import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.ValueEventListener;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.Manifest;
import android.nfc.Tag;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class TrackerService extends Service {

    private String unique_id;
    private boolean already_assigned = false;

    private String unique_id_happy;
    private boolean already_assigned_happy = false;

    private String userID;
    private DatabaseReference ref = FirebaseDatabase.getInstance().getReference();

    private static final String TAG = TrackerService.class.getSimpleName();

    private FirebaseAuth mAuth;

    @Override
    public IBinder onBind(Intent intent) {return null;}

    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
        loginToFirebase();
    }
    @Override
    public int onStartCommand(Intent intent, int flag, int startId){
        super.onStartCommand(intent, flag, startId);

        userID = intent.getStringExtra("state");

        Toast.makeText(this, userID,Toast.LENGTH_SHORT).show();

        return START_STICKY;

    }
    @Override
    public void onDestroy() {
        if(already_assigned){
            ref.getRoot().child("need_list").child(unique_id).removeValue();
        }

        if(already_assigned_happy){
            ref.getRoot().child("user_list").child(unique_id_happy).removeValue();
        }
        super.onDestroy();
    }


    protected BroadcastReceiver stopReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "received stop broadcast");
            // Stop the service when the notification is tapped
            unregisterReceiver(stopReceiver);
            stopSelf();
        }
    };

    private void loginToFirebase() {
        // Authenticate with Firebase, and request location updates
        String email = getString(R.string.firebase_email);
        String password = getString(R.string.firebase_password);
        FirebaseAuth.getInstance().signInWithEmailAndPassword(
                email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>(){
            @Override
            public void onComplete(Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    Log.d(TAG, "firebase auth success");

                    requestLocationUpdates();

                } else {
                    Log.d(TAG, "firebase auth failed");
                }
            }
        });



    }
    private void sendMessageToActivity(String msg) {
        Intent intent = new Intent("intentKey");
        // You can also include some extra data.
        intent.putExtra("key", msg);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }


    private void requestLocationUpdates() {
        LocationRequest request = new LocationRequest();
        request.setInterval(10000);
        request.setFastestInterval(5000);
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(this);
        final String path = getString(R.string.firebase_path) + "/" + getString(R.string.transport_id);
        int permission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        if (permission == PackageManager.PERMISSION_GRANTED) {
            // Request location updates and when an update is
            // received, store the location in Firebase
            client.requestLocationUpdates(request, new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    ref = ref.getRoot();
                    final Location location = locationResult.getLastLocation();
                    if (location != null) {


                        if(userID.equals("sad")){
                            if(already_assigned_happy){
                                ref.child("user_list").child(unique_id_happy).removeValue();
                            }
                            ref = ref.child("need_list");
                            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    if(!already_assigned){
                                        unique_id = ref.push().getKey();
                                        already_assigned = true;

                                    }

                                    ref.child(unique_id).child("long").setValue(Double.toString(location.getLongitude()));
                                    ref.child(unique_id).child("lat").setValue(Double.toString(location.getLatitude()));

                                    /**

                                    for(DataSnapshot child: dataSnapshot.getChildren()) {
                                        sendMessageToActivity(Double.toString(location.getLatitude()) + "," + Double.toString(location.getLongitude()));
                                    }
                                     **/


                                }
                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            });




                        }
                        if(userID.equals("happy")){
                            if(already_assigned){
                                ref.child("need_list").child(unique_id).removeValue();
                            }

                            ref = ref.child("user_list");
                            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    if(!already_assigned_happy){
                                        unique_id_happy = ref.push().getKey();
                                        already_assigned_happy=true;
                                    }
                                    ref.child(unique_id_happy).child("long").setValue(Double.toString(location.getLongitude()));
                                    ref.child(unique_id_happy).child("lat").setValue(Double.toString(location.getLatitude()));
                                }
                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            });

                            DatabaseReference temp = FirebaseDatabase.getInstance().getReference();
                            temp.child("need_list").addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    for(DataSnapshot child: dataSnapshot.getChildren()) {
                                        sendMessageToActivity(Double.toString(location.getLatitude()) + "," + Double.toString(location.getLongitude()));
                                    }

                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });


                        }










                    }
                }
            }, null);
        }
    }


}