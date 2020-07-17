package javatar.com.trackout.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Objects;

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

        Log.d(TAG, title);
        Log.d(TAG, message);
        if (title.equals("locationUpdates")){
            Intent intent = new Intent(this,MyBackgroundService.class);
            if (message.equals("1")){
                if (MainActivity.active){
                    intent.putExtra("locationUpdates","on");
                    startService(intent);
                    Log.d(TAG, "onMessageReceived: startService");
                }else {
                    pushActivity();
                }
            }else if (message.equals("0")) {
                intent.putExtra("locationUpdates","off");
                startService(intent);
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

    void pushActivity(){
        Intent myIntent = new Intent(getApplicationContext(), MyReceiver.class);
        AlarmManager manager = (AlarmManager)
                Objects.requireNonNull(this).getSystemService(Context.ALARM_SERVICE);
        assert manager != null;
        manager.set(AlarmManager.RTC,java.text.DateFormat.getDateTimeInstance().getCalendar().getTimeInMillis(),
                PendingIntent.getBroadcast(getApplicationContext(), 0, myIntent, 0));
    }
}
