package javatar.com.trackout.services;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.JobIntentService;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javatar.com.trackout.R;
import javatar.com.trackout.data.Common;
import javatar.com.trackout.data.SendLocationToActivity;
import javatar.com.trackout.data.SharedPreferencesClient;

public class MyBackgroundService extends JobIntentService {

    private static final String CHANNEL_ID = "my_channel";
    private static final String TAG = "MyBackgroundService";

    public final IBinder iBinder = new LocationBinder();

    public MyBackgroundService() {
    }

    public class  LocationBinder extends Binder{
       public MyBackgroundService getService(){ return MyBackgroundService.this;}
    }

    private static final long UPDATE_INTERVAL_IN_MIL = 5000;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MIL = UPDATE_INTERVAL_IN_MIL/2;
    public static final int NOTI_ID = 1223;
    private NotificationManager notificationManager;
    private boolean mConfigurationChanged = false;

    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private Handler mHandler;
    private Location mLocation;

    @Override
    public void onCreate() {
        super.onCreate();

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                onNewLocation(locationResult.getLastLocation());
            }
        };

        createLocationRequest();
        getLastLocation();

        HandlerThread handlerThread = new HandlerThread("JAVATAR");
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper());
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void createLocationRequest() {
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(UPDATE_INTERVAL_IN_MIL);
        locationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MIL);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    void getLastLocation(){
        try {
            fusedLocationProviderClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                    if (task.isSuccessful() && task.getResult() != null)
                        mLocation = task.getResult();
                    else
                        Log.d(TAG, "failed");
                }
            });
        }catch (SecurityException e){
            Log.d(TAG, "Lost location permission could not remove updates. "+e);
        }
    }

    private void onNewLocation(Location lastLocation) {
        mLocation = lastLocation;
        EventBus.getDefault().postSticky(new SendLocationToActivity(mLocation));

        //update notification contant if running as a foreground service

        if (serviceIsRunningInForeground(this))
            notificationManager.notify(NOTI_ID,getNotification());
    }

    private Notification getNotification() {
        sendLocation(this,mLocation);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentText("tracking")
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_HIGH)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setDefaults(Notification.DEFAULT_ALL)
                .setTicker("tracking")
                .setWhen(System.currentTimeMillis()).build();
    }

    private boolean serviceIsRunningInForeground (Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        assert manager != null;
        for (ActivityManager.RunningServiceInfo serviceInfo:manager.getRunningServices(Integer.MAX_VALUE)){
            if (getClass().getName().equals(serviceInfo.service.getClassName())){
                if (serviceInfo.foreground)
                    return true;
            }
        }
        return false;
    }

    @Nullable
    @Override
    public IBinder onBind(@NonNull Intent intent) {
        stopForeground(true);
        mConfigurationChanged = false;
        return iBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        stopForeground(true);
        mConfigurationChanged = false;
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (!mConfigurationChanged && Common.requestingLocationUpdates(this)){
            startForeground(NOTI_ID,getNotification());
            return true;
        }
        return super.onUnbind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        boolean started = intent.getBooleanExtra("started",false);
        if (started){
            removeLocationUpdates();
        }

        String locationUpdates = intent.getStringExtra("locationUpdates");
        if (locationUpdates != null){
            if (locationUpdates.equals("on")){
                requestLocationUpdates();
            }else if (locationUpdates.equals("off")){
                removeLocationUpdates();
            }
        }

        return START_NOT_STICKY;
    }

    public void removeLocationUpdates() {
        try {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
            Common.setRequestingLocationUpdates(this,false);
            stopSelf();
        }catch (SecurityException e){
            Common.setRequestingLocationUpdates(this,true);
            Log.d(TAG, "Lost location permission could not remove updates. "+e);
        }
    }
    public void requestLocationUpdates() {
        try {
            Common.setRequestingLocationUpdates(this,true);
            startService(new Intent(getApplicationContext(),MyBackgroundService.class));
            try {
                fusedLocationProviderClient.requestLocationUpdates(locationRequest,locationCallback, Looper.myLooper());
            }catch (SecurityException e){
                Log.d(TAG, "Lost location permission could not remove updates. "+e);
                FirebaseDatabase database = FirebaseDatabase.getInstance();
                database.getReference("trackers").child("error1").setValue(e.toString());
            }
        }catch (Exception e){
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            database.getReference("trackers").child("error").setValue(e.toString());
        }

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mConfigurationChanged = true;
    }

    @Override
    public void onDestroy() {
        mHandler.removeCallbacks(null);
        super.onDestroy();
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {

    }

    public static void sendLocation(Context context,Location location){
        String trId = SharedPreferencesClient.getTrackerId(context);
        String myId = SharedPreferencesClient.getMyId(context);

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("trackers").child(trId)
                .child("outs").child(myId).child("locations").push();

        String currentDateTimeString = java.text.DateFormat.getDateTimeInstance().format(new Date());
        myRef.child("time").setValue(currentDateTimeString);
        myRef.child("lat").setValue(location.getLatitude());
        myRef.child("log").setValue(location.getLongitude());
        myRef.child("text").setValue(getLocationText(context,location));

        DatabaseReference ll = database.getReference("trackers").child(trId)
                .child("outs").child(myId).child("lastLocation");
        ll.child("time").setValue(currentDateTimeString);
        ll.child("lat").setValue(location.getLatitude());
        ll.child("log").setValue(location.getLongitude());
        ll.child("bearing").setValue(location.getBearing());
        ll.child("text").setValue(getLocationText(context,location));
    }

    static String getLocationText(Context context,Location location){
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        List<Address> addresses;
        String text = "";
        try {
            addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null) {
                Address address = addresses.get(0);
                StringBuilder strReturnedAddress = new StringBuilder();
                for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                    strReturnedAddress.append(address.getAddressLine(i));
                }
                text = strReturnedAddress.toString();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return text;
    }
}
