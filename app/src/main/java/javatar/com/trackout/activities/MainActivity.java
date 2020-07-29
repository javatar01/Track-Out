package javatar.com.trackout.activities;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javatar.com.trackout.R;
import javatar.com.trackout.data.Common;
import javatar.com.trackout.data.SendLocationToActivity;
import javatar.com.trackout.services.MyBackgroundService;
import javatar.com.trackout.services.MyFireBaseMessagingService;

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "MainActivity";
    Button locationUpdates,removeUpdates;

    MyBackgroundService mService = null;
    boolean mBound = false;

    public static boolean active = false;

    TextView tracking;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MyBackgroundService.LocationBinder binder = (MyBackgroundService.LocationBinder) service;
            mService = binder.getService();
            mBound = true;
            Log.d(TAG, "onServiceConnected: ");
            boolean updates = getIntent().getBooleanExtra("locationUpdates",false);
            if (updates){
                mService.requestLocationUpdates();
                finish();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            mBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        active = true;

        tracking = findViewById(R.id.tracking);
        locationUpdates = findViewById(R.id.location_updates);
        removeUpdates = findViewById(R.id.remove_updates);

        FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
            @Override
            public void onComplete(@NonNull Task<InstanceIdResult> task) {
                if (!task.isSuccessful()) {
                    Log.w("fail", "getInstanceId failed", task.getException());
                    return;
                }
                // Get new Instance ID token
                String token = Objects.requireNonNull(task.getResult()).getToken();
                MyFireBaseMessagingService.sendToken(MainActivity.this,token);
            }
        });

        Dexter.withContext(this)
            .withPermissions(Arrays.asList(
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            ))
            .withListener(new MultiplePermissionsListener() {
                @Override
                public void onPermissionsChecked(MultiplePermissionsReport multiplePermissionsReport) {

                    locationUpdates.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mService.requestLocationUpdates();
                        }
                    });

                    removeUpdates.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mService.removeLocationUpdates();
                        }
                    });

                    bindService(new Intent(MainActivity.this,MyBackgroundService.class),
                            serviceConnection, Context.BIND_AUTO_CREATE);

                    Log.d(TAG, "onPermissionsChecked: ");
                }

                @Override
                public void onPermissionRationaleShouldBeShown(List<PermissionRequest> list, PermissionToken permissionToken) {

                }
            }).check();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setButtonsState(Common.requestingLocationUpdates(MainActivity.this));
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(Common.KEY_REQUESTING)){
            setButtonsState(sharedPreferences.getBoolean(Common.KEY_REQUESTING,false));
        }
    }

    private void setButtonsState(boolean aBoolean) {
        if (aBoolean){
            tracking.setVisibility(View.VISIBLE);
            locationUpdates.setEnabled(false);
            removeUpdates.setEnabled(true);
        }else {
            tracking.setVisibility(View.INVISIBLE);
            locationUpdates.setEnabled(true);
            removeUpdates.setEnabled(false);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        if (mBound){
            unbindService(serviceConnection);
            mBound= false;
        }
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void omListonLocation(SendLocationToActivity location){
        if (Common.requestingLocationUpdates(MainActivity.this)){
            String text = Common.getLocationText(location.getLocation());
            Toast.makeText(mService, text, Toast.LENGTH_SHORT).show();
            MyBackgroundService.sendLocation(this,location.getLocation());
        }
    }

    @Override
    protected void onDestroy() {
        active = false;
        super.onDestroy();
    }
}