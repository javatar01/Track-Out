package javatar.com.trackout.data;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesClient {

    private static final String PATH ="TRACKIN";
    private static final String myIdKay ="myId";
    private static final String trackerIdKay ="trackerId";
    private static final String tokenKay ="token";
    private static String myId,trackerId,token;

    public static String getMyId(Context context) {
        if (myId == null){
            SharedPreferences sharedPreferences = context.getSharedPreferences(PATH, Context.MODE_PRIVATE);
            myId =  sharedPreferences.getString(myIdKay,null);
        }
        return myId;
    }

    public static void setMyId(Context context, String myId) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PATH, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(myIdKay, myId);
        editor.apply();
        SharedPreferencesClient.myId = myId;
    }

    public static String getTrackerId(Context context) {
        if (trackerId == null){
            SharedPreferences sharedPreferences = context.getSharedPreferences(PATH, Context.MODE_PRIVATE);
            trackerId =  sharedPreferences.getString(trackerIdKay,null);
        }
        return trackerId;
    }

    public static void setTrackerId(Context context, String trackerId) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PATH, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(trackerIdKay, trackerId);
        editor.apply();
        SharedPreferencesClient.trackerId = trackerId;
    }

    public static String getToken(Context context) {
        if (token == null){
            SharedPreferences sharedPreferences = context.getSharedPreferences(PATH, Context.MODE_PRIVATE);
            token =  sharedPreferences.getString(tokenKay,null);
        }
        return token;
    }

    public static void setToken(Context context, String token) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PATH, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(tokenKay, token);
        editor.apply();
        SharedPreferencesClient.token = token;
    }
}
