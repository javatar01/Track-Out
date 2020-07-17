package javatar.com.trackout.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import javatar.com.trackout.activities.MainActivity;

public class MyReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.

        Intent newIntent = new Intent(context, MainActivity.class);
        newIntent.putExtra("locationUpdates",true);
        newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK );
        context.startActivity(newIntent);
    }
}
