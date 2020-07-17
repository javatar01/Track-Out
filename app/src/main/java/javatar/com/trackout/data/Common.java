package javatar.com.trackout.data;

import android.content.Context;
import android.location.Location;

import androidx.preference.PreferenceManager;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class Common {

    public static final String KEY_REQUESTING = "locationUpdateEnable";

    public static String getLocationText(Location mLocation) {
        return mLocation == null ? "Unknown Location" : mLocation.getLatitude() +
                "/" +
                mLocation.getLongitude();
    }

    public static void setRequestingLocationUpdates(Context context, boolean value) {
        changeStatesUpdates(value,context);
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(KEY_REQUESTING,value)
                .apply();
    }

    public static boolean requestingLocationUpdates(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(KEY_REQUESTING,false);
    }

    public static void changeStatesUpdates(boolean value,Context context){
        String trId = SharedPreferencesClient.getTrackerId(context);
        String myId = SharedPreferencesClient.getMyId(context);

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("trackers").child(trId).
                child("outs").child(myId).child("location_updates");
        myRef.setValue(value);
    }
}
