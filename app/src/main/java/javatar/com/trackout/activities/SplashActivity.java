package javatar.com.trackout.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.google.firebase.auth.FirebaseAuth;

import javatar.com.trackout.R;
import javatar.com.trackout.data.SharedPreferencesClient;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

       new Handler().postDelayed(new Runnable() {
           @Override
           public void run() {
               if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                   if (SharedPreferencesClient.getTrackerId(SplashActivity.this) == null){
                       startActivity(new Intent(SplashActivity.this,SetTrackerActivity.class));
                   }else {
                       startActivity(new Intent(SplashActivity.this,MainActivity.class));
                   }
               }else {
                   startActivity(new Intent(SplashActivity.this, SignInActivity.class));
               }
               finish();
           }
       },2000);
    }
}