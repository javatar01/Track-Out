package javatar.com.trackout.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javatar.com.trackout.R;
import javatar.com.trackout.data.SharedPreferencesClient;

public class SetTrackerActivity extends AppCompatActivity {

    private static final String TAG = "SetTrackerActivity";

    EditText tracker_id_text;

    FirebaseDatabase database;

    boolean stopChange = false,stopChange2 = false;
    private final int requestUpload = 111;

    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_tracker);

        progressDialog = new ProgressDialog(this);

        tracker_id_text = findViewById(R.id.tracker_id_text);
        database = FirebaseDatabase.getInstance();
    }

    public void setTracker(View view) {
        final String trId = tracker_id_text.getText().toString();

        if (trId.isEmpty()){
            Toast.makeText(this, "Enter trucker id", Toast.LENGTH_SHORT).show();
            return;
        }
        
        setTracker(trId);
    }

    private void setTracker(final String trId) {
        DatabaseReference myRef = database.getReference("trackers");

        progressDialog.setMessage("Please wait..");
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();

        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (stopChange)return;

                boolean flag = false;

                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    String id = ds.child("id").getValue(String.class);
                    assert id != null;
                    if (id.equals(trId)){
                        SharedPreferencesClient.setTrackerId(SetTrackerActivity.this,id);
                        stopChange = true;
                        setMyId(id);
                        flag = true;
                        break;
                    }
                }

                if (!flag) {
                    Toast.makeText(SetTrackerActivity.this, "This id does not exist", Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
                progressDialog.dismiss();
            }
        });
    }

    void setMyId(String id){
        final String myId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        final DatabaseReference myRef = database.getReference("trackers").child(id).child("outs").child(myId);
        myRef.child("id").setValue(myId);
        myRef.child("location_updates").setValue(false);

        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (stopChange2)return;
                stopChange2 = true;
                progressDialog.dismiss();
                SharedPreferencesClient.setMyId(SetTrackerActivity.this,myId);
                startActivity(new Intent(SetTrackerActivity.this,MainActivity.class));
                finish();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Failed to read value
                progressDialog.dismiss();
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });
    }

    public void openScan(View view) {
        Dexter.withContext(this)
                .withPermissions(Collections.singletonList(
                        Manifest.permission.CAMERA
                ))
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport multiplePermissionsReport) {
                        startActivityForResult(new Intent(SetTrackerActivity.this,ScanActivity.class),requestUpload);
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> list, PermissionToken permissionToken) {

                    }
                }).check();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == requestUpload && resultCode == RESULT_OK) {
            setTracker(data.getStringExtra("trid"));
        }
    }
}