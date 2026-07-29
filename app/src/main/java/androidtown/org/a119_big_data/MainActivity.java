package androidtown.org.a119_big_data;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
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
import com.kakao.vectormap.KakaoMapSdk;

public class MainActivity extends AppCompatActivity {

    private MapView mapView;
    private KakaoMap kakaoMap;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LatLng currentLatLng; // 내 현재 위치 저장용 변수

    // UI 변수들
    private Button btnCategory1;         // 위험요소 (또는 기존 카테고리 1)
    private Button btnCategory2;         // 위험등급 (또는 기존 카테고리 2)
    private Button btnNearbyFireStation;  // 근처 소방서 (btn_category3)
    private Button btnNearbySafetyCenter; // 근처 안전센터 (btn_category4)
    private Button btnNearbyHospital;     // 근처 병원 (btn_category5)

    // ★ 추가된 메뉴 열기 버튼
    private Button btnOpenMenu;

    private ImageButton btnMyLocation;
    private EditText etSearch;
    private ImageView ivSearchIcon;

    // 매니저 객체들
    private SafetyMapManager mapManager;
    private myLocationMarkerManager locationMarkerManager;
    private NearbySearchManager nearbySearchManager; // 근처 시설 검색 매니저
    private boolean isMyLocationVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        KakaoMapSdk.init(this, "5d3f0a47bed40fc0f67ef9c052865514");
        setContentView(R.layout.activity_main);

        // UI 컴포넌트 연결
        mapView = findViewById(R.id.map_view);
        btnCategory1 = findViewById(R.id.btn_category1);
        btnCategory2 = findViewById(R.id.btn_category2);
        btnNearbyFireStation = findViewById(R.id.btn_category3);
        btnNearbySafetyCenter = findViewById(R.id.btn_category4);
        btnNearbyHospital = findViewById(R.id.btn_category5);

        // ★ 새로 추가할 메뉴 열기 버튼 연결 (activity_main.xml에 이 id의 버튼이 있어야 합니다)
        btnOpenMenu = findViewById(R.id.btn_open_menu);

        btnMyLocation = findViewById(R.id.btn_my_location);
        etSearch = findViewById(R.id.et_search);
        ivSearchIcon = findViewById(R.id.iv_search_icon);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // 카카오맵 라이프사이클 및 준비 콜백
        mapView.start(new MapLifeCycleCallback() {
            @Override
            public void onMapDestroy() {}
            @Override
            public void onMapError(Exception e) { e.printStackTrace(); }
        }, new KakaoMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull KakaoMap map) {
                kakaoMap = map;

                // 매니저 인스턴스 초기화
                mapManager = new SafetyMapManager(MainActivity.this, kakaoMap);
                locationMarkerManager = new myLocationMarkerManager(MainActivity.this, kakaoMap);
                nearbySearchManager = new NearbySearchManager(MainActivity.this, kakaoMap);

                checkLocationPermission();
            }
        });

        // 1. 내 위치 버튼
        btnMyLocation.setOnClickListener(v -> {

            isMyLocationVisible = true;

            if (locationMarkerManager != null) {
                locationMarkerManager.moveToCurrentLocation();
            } else {
                checkLocationPermission();
            }
        });

        // ==========================================
        // ★ 추가된 기능: 통합 메뉴 (바텀 시트) 띄우기
        // ==========================================
        if (btnOpenMenu != null) {
            btnOpenMenu.setOnClickListener(v -> {
                CategoryMenuDialog menuDialog = new CategoryMenuDialog();

                menuDialog.setOnCategorySelectedListener(new CategoryMenuDialog.OnCategorySelectedListener() {
                    @Override
                    public void onCategorySelected(String mainCategory, String subCategory) {

                        // 기존 마커를 지우는 로직이 필요하다면 mapManager를 통해 초기화
                        // if (mapManager != null) { mapManager.clearMarkers(); }

                        if (mainCategory.equals("소방서")) {
                            if (mapManager != null) {
                                mapManager.setCategory("fire_station");
                                Toast.makeText(MainActivity.this, "소방서를 표시합니다.", Toast.LENGTH_SHORT).show();
                            }
                        } else if (mainCategory.equals("안전센터")) {
                            if (mapManager != null) {
                                mapManager.setCategory("safety_center");
                                Toast.makeText(MainActivity.this, "안전센터/구조대를 표시합니다.", Toast.LENGTH_SHORT).show();
                            }
                        } else if (mainCategory.equals("병원")) {
                            // subCategory(일반의원, 안과 등)를 받아서 처리하는 전용 메서드 호출
                            loadHospitalData(subCategory);
                        }
                    }
                });

                menuDialog.show(getSupportFragmentManager(), "CategoryMenu");
            });
        }
        // ==========================================

        // 기존 2. 카테고리 1 (소방서 목록)
        btnCategory1.setOnClickListener(v -> {
            if (mapManager != null) {
                mapManager.setCategory("fire_station");
            }
        });

        // 기존 3. 카테고리 2 (안전센터 목록)
        btnCategory2.setOnClickListener(v -> {
            if (mapManager != null) {
                mapManager.setCategory("safety_center");
            }
        });

        // 4. [근처 소방서] 버튼 클릭
        btnNearbyFireStation.setOnClickListener(v -> {
            if (nearbySearchManager != null && mapManager != null) {
                if (currentLatLng != null) {
                    nearbySearchManager.showNearbyPlaces(
                            mapManager.getSafetyList(),
                            currentLatLng,
                            NearbySearchManager.TYPE_FIRE_STATION,
                            3000 // 100km (테스트 후 3000m로 변경 가능)
                    );
                } else {
                    Toast.makeText(MainActivity.this, "현재 위치 정보를 가져오는 중입니다.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // 5. [근처 안전센터] 버튼 클릭
        btnNearbySafetyCenter.setOnClickListener(v -> {
            if (nearbySearchManager != null && mapManager != null) {
                if (currentLatLng != null) {
                    nearbySearchManager.showNearbyPlaces(
                            mapManager.getSafetyList(),
                            currentLatLng,
                            NearbySearchManager.TYPE_SAFETY_CENTER,
                            3000 // 100km
                    );
                } else {
                    Toast.makeText(MainActivity.this, "현재 위치 정보를 가져오는 중입니다.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // 6. [근처 병원] 버튼 클릭
        btnNearbyHospital.setOnClickListener(v -> {
            if (nearbySearchManager != null && mapManager != null) {
                if (currentLatLng != null) {
                    nearbySearchManager.showNearbyPlaces(
                            mapManager.getSafetyList(),
                            currentLatLng,
                            NearbySearchManager.TYPE_HOSPITAL,
                            3000 // 100km
                    );
                } else {
                    Toast.makeText(MainActivity.this, "현재 위치 정보를 가져오는 중입니다.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // 7. 돋보기 검색 버튼
        ivSearchIcon.setOnClickListener(v -> {
            String keyword = etSearch.getText().toString().trim();
            if (mapManager == null) return;

            if (nearbySearchManager != null) {
                nearbySearchManager.clearNearbyMarkers();
            }

            if (!keyword.isEmpty()) {
                mapManager.searchAndMoveToGu(keyword);
            } else {
                mapManager.clearGuFilter();
                Toast.makeText(MainActivity.this, "전체 지역을 표시합니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ★ 병원 세부 카테고리를 처리하는 메서드 추가
    private void loadHospitalData(String subCategory) {
        Toast.makeText(MainActivity.this, subCategory + " 데이터를 불러옵니다.", Toast.LENGTH_SHORT).show();

        if (mapManager != null) {
            // mapManager 내부에 파이어베이스 또는 JSON에서 진료과에 맞게
            // 데이터를 불러오는 로직(예: mapManager.setCategory("hospital_" + subCategory))을
            // 구현해주시면 됩니다.
            // mapManager.setCategory("hospital_" + subCategory);
        }
    }

    /* 위치 권한 및 GPS 제어 로직 */
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
        currentLatLng = LatLng.from(lat, lng);

        if(locationMarkerManager != null && isMyLocationVisible){
            locationMarkerManager.updateLocation(lat, lng, false);
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