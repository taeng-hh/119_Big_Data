package androidtown.org.a119_big_data;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
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

    // UI 변수들
    private Button btnFireStation;
    private Button btnSafetyCenter;
    private ImageButton btnMyLocation;
    private EditText etSearch;
    private ImageView ivSearchIcon;

    // 지도 및 파이어베이스 데이터 처리를 전담할 새 매니저 객체 선언
    private SafetyMapManager mapManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI 컴포넌트 연결
        mapView = findViewById(R.id.map_view);
        btnFireStation = findViewById(R.id.btn_category1);
        btnSafetyCenter = findViewById(R.id.btn_category2);
        btnMyLocation = findViewById(R.id.btn_my_location);
        etSearch = findViewById(R.id.et_search);
        ivSearchIcon = findViewById(R.id.iv_search_icon);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // 카카오맵 라이프사이클 및 준비 콜백 설정
        mapView.start(new MapLifeCycleCallback() {
            @Override
            public void onMapDestroy() {}
            @Override
            public void onMapError(Exception e) { e.printStackTrace(); }
        }, new KakaoMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull KakaoMap map) {
                kakaoMap = map;

                // 지도가 준비되면 매니저 인스턴스를 생성하고 초기화해!
                mapManager = new SafetyMapManager(MainActivity.this, kakaoMap);

                // 매니저가 맵의 모든 마커를 지울 때, 메인 액티비티의 내 위치 라벨 참조도 함께 비워주도록 설계
                mapManager.setOnMapClearedListener(new SafetyMapManager.OnMapClearedListener() {
                    @Override
                    public void onMapCleared() {
                        currentLocationLabel = null;
                    }
                });
            }
        });

        // 내 위치 보기 버튼 클릭
        btnMyLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentLocationLabel != null && kakaoMap != null) {
                    kakaoMap.getTrackingManager().startTracking(currentLocationLabel);
                } else {
                    checkLocationPermission();
                }
            }
        });

        // 소방서 카테고리 선택 -> 매니저에게 요청 전달
        btnFireStation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mapManager != null) {
                    mapManager.setCategory("fire_station");
                }
            }
        });

        // 안전센터 카테고리 선택 -> 매니저에게 요청 전달
        btnSafetyCenter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mapManager != null) {
                    mapManager.setCategory("safety_center");
                }
            }
        });

        // 돋보기 검색 버튼 클릭 이벤트 -> 매니저에게 요청 전달
        ivSearchIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String keyword = etSearch.getText().toString().trim();
                if (mapManager == null) return;

                if (!keyword.isEmpty()) {
                    mapManager.searchAndMoveToGu(keyword);
                } else {
                    mapManager.clearGuFilter();
                    Toast.makeText(MainActivity.this, "전체 지역을 표시합니다.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /* ========================================================================= */
    /* 아래 위치 권한 및 실시간 GPS 수신 기능(UI 화면 관리 영역)은 기존 코드 그대로 유지 */
    /* ========================================================================= */

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