package javatar.com.trackout.services;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.media.RingtoneManager;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Objects;

import javatar.com.trackout.R;
import javatar.com.trackout.data.SharedPreferencesClient;
import javatar.com.trackout.activities.MainActivity;

public class MyFireBaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFireBaseMessagingServ";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String title = remoteMessage.getData().get("Title");
        String message = remoteMessage.getData().get("Message");

        if (title == null || message == null){
            return;
        }

        if (title.equals("locationUpdates")){
            Intent intent = new Intent(this,MyBackgroundService.class);
            switch (message) {
                case "1":
                    if (MainActivity.active) {
                        intent.putExtra("locationUpdates", "on");
                        startService(intent);
                        Log.d(TAG, "onMessageReceived: startService");
                    } else {
                        pushNotification();                    }
                    break;
                case "0":
                    intent.putExtra("locationUpdates", "off");
                    startService(intent);
                    break;
                case "ll":
                    try {
                        FusedLocationProviderClient fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
                        fusedLocationProviderClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
                            @Override
                            public void onComplete(@NonNull Task<Location> task) {
                                if (task.isSuccessful() && task.getResult() != null) {
                                    Location location = task.getResult();

                                    MyBackgroundService.sendLocation(MyFireBaseMessagingService.this,location);
                                } else
                                    Log.d(TAG, "failed");
                            }
                        });
                    } catch (SecurityException e) {
                        Log.d(TAG, "Lost location permission could not remove updates. " + e);
                    }
                    break;
            }
        }
    }

    public void onNewToken(@NonNull String token) {
        if (SharedPreferencesClient.getTrackerId(this) != null){
            sendToken(this, token);
        }
    }

    public static void sendToken(Context context, String token){
        SharedPreferencesClient.setToken(context,token);
        String trId = SharedPreferencesClient.getTrackerId(context);
        String myId = SharedPreferencesClient.getMyId(context);

        FirebaseDatabase database = FirebaseDatabase.getInstance();

        DatabaseReference myRef = database.getReference("trackers").child(trId).child("outs").child(myId);
        myRef.child("token").setValue(token);
    }

    void pushNotification(){
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("default",
                    "YOUR_CHANNEL_NAME",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("YOUR_NOTIFICATION_CHANNEL_DISCRIPTION");
            if (mNotificationManager != null) {
                mNotificationManager.createNotificationChannel(channel);
            }
        }

        Intent newIntent =new Intent(getApplicationContext(), MainActivity.class);
        newIntent.putExtra("locationUpdates",true);
        newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK );

        NotificationCompat.Builder notification = new NotificationCompat.Builder(getApplicationContext(), "default")
                .setContentTitle("Tracking Request")
                .setContentText("Click to enable")
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_HIGH)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE))
                .setContentIntent(PendingIntent.getActivity(this, 0,newIntent , 0))
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        if (mNotificationManager != null) {
            mNotificationManager.notify(MyBackgroundService.NOTI_ID, notification.build());
        }
    }
}
