package androidtown.org.a119_big_data;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import com.kakao.vectormap.KakaoMap;
import com.kakao.vectormap.KakaoMapReadyCallback;
import com.kakao.vectormap.MapLifeCycleCallback;
import com.kakao.vectormap.MapView;
import com.kakao.vectormap.LatLng;
import com.kakao.vectormap.label.Label;
import com.kakao.vectormap.label.LabelOptions;
import com.kakao.vectormap.label.LabelStyle;
import com.kakao.vectormap.label.LabelStyles;

public class MainActivity extends AppCompatActivity {

    private MapView mapView;
    private KakaoMap kakaoMap;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private Label currentLocationLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mapView = findViewById(R.id.map_view);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mapView.start(new MapLifeCycleCallback() {
            @Override
            public void onMapDestroy() {
            }

            @Override
            public void onMapError(Exception e) {
                e.printStackTrace();
            }
        }, new KakaoMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull KakaoMap map) {
                kakaoMap = map;
                checkLocationPermission();
            }
        });
    }

    private void checkLocationPermission(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED){
            startLocationUpdates();
        } else{
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == LOCATION_PERMISSION_REQUEST_CODE){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else{
                Toast.makeText(this, "현재 위치 기능을 사용하려면 위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startLocationUpdates(){
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED){
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setMinUpdateIntervalMillis(5000)
                .build();

        locationCallback = new LocationCallback(){
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult){
                for (Location location : locationResult.getLocations()){
                    if(location != null && kakaoMap != null) {
                        updateCurrentLocation(location.getLatitude(), location.getLongitude());
                    }
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void updateCurrentLocation(double lat, double lng){
        LatLng currentLatLng = LatLng.from(lat, lng);

        if(currentLocationLabel == null){
            LabelStyles styles = kakaoMap.getLabelManager()
                    .addLabelStyles(LabelStyles.from(LabelStyle.from(android.R.drawable.ic_menu_mylocation)));
            LabelOptions options = LabelOptions.from(currentLatLng).setStyles(styles);
            currentLocationLabel = kakaoMap.getLabelManager().getLayer().addLabel(options);
            kakaoMap.getTrackingManager().startTracking(currentLocationLabel);
        } else {
            currentLocationLabel.moveTo(currentLatLng);
        }
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        if(fusedLocationClient != null && locationCallback != null){
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}