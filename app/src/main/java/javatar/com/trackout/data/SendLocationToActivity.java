package javatar.com.trackout.data;

import android.location.Location;

public class SendLocationToActivity {
    private Location location;

    public SendLocationToActivity(Location mLocation) {
        location = mLocation;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }
}
