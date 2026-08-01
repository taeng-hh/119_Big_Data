package androidtown.org.a119_big_data;

import android.content.Intent;
import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

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
    private LatLng currentLatLng;

    private Button btnCategory1;
    private Button btnCategory2;
    private Button btnNearbyFireStation;
    private Button btnNearbySafetyCenter;
    private Button btnNearbyHospital;
    private Button btnOpenMenu;

    private ImageButton btnMyLocation;
    private EditText etSearch;
    private ImageView ivSearchIcon;

    private SafetyMapManager mapManager;
    private myLocationMarkerManager locationMarkerManager;
    private NearbySearchManager nearbySearchManager;
    private boolean isMyLocationVisible = false;

    private DrawerLayout drawerLayout;
    private GridLayout layoutHospitalSub;
    private boolean isHospitalExpanded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        KakaoMapSdk.init(this, "5d3f0a47bed40fc0f67ef9c052865514");
        setContentView(R.layout.activity_main);

        drawerLayout = findViewById(R.id.drawer_layout);
        mapView = findViewById(R.id.map_view);
        btnCategory1 = findViewById(R.id.btn_category1);
        btnCategory2 = findViewById(R.id.btn_category2);
        btnNearbyFireStation = findViewById(R.id.btn_category3);
        btnNearbySafetyCenter = findViewById(R.id.btn_category4);
        btnNearbyHospital = findViewById(R.id.btn_category5);
        View btnOpenMenu = findViewById(R.id.btn_open_menu);

        btnMyLocation = findViewById(R.id.btn_my_location);
        etSearch = findViewById(R.id.et_search);
        ivSearchIcon = findViewById(R.id.iv_search_icon);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        setupDrawerMenu();

        mapView.start(new MapLifeCycleCallback() {
            @Override
            public void onMapDestroy() {}
            @Override
            public void onMapError(Exception e) { e.printStackTrace(); }
        }, new KakaoMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull KakaoMap map) {
                kakaoMap = map;

                mapManager = new SafetyMapManager(MainActivity.this, kakaoMap);
                locationMarkerManager = new myLocationMarkerManager(MainActivity.this, kakaoMap);
                nearbySearchManager = new NearbySearchManager(MainActivity.this, kakaoMap);

                checkLocationPermission();
            }
        });

        btnMyLocation.setOnClickListener(v -> {
            isMyLocationVisible = true;

            if (locationMarkerManager != null) {
                locationMarkerManager.moveToCurrentLocation();
            } else {
                checkLocationPermission();
            }
        });

        if (btnOpenMenu != null) {
            btnOpenMenu.setOnClickListener(v -> {
                if (drawerLayout != null) {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            });
        }

        btnCategory1.setOnClickListener(v -> {
            if (mapManager != null) {
                mapManager.setCategory("fire_station");
            }
        });

        btnCategory2.setOnClickListener(v -> {
            if (mapManager != null) {
                mapManager.setCategory("safety_center");
            }
        });

        btnNearbyFireStation.setOnClickListener(v -> {
            if (nearbySearchManager != null && mapManager != null) {
                if (currentLatLng != null) {
                    nearbySearchManager.showNearbyPlaces(
                            mapManager.getSafetyList(),
                            currentLatLng,
                            NearbySearchManager.TYPE_FIRE_STATION,
                            3000
                    );
                } else {
                    Toast.makeText(MainActivity.this, "현재 위치 정보를 가져오는 중입니다.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnNearbySafetyCenter.setOnClickListener(v -> {
            if (nearbySearchManager != null && mapManager != null) {
                if (currentLatLng != null) {
                    nearbySearchManager.showNearbyPlaces(
                            mapManager.getSafetyList(),
                            currentLatLng,
                            NearbySearchManager.TYPE_SAFETY_CENTER,
                            3000
                    );
                } else {
                    Toast.makeText(MainActivity.this, "현재 위치 정보를 가져오는 중입니다.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnNearbyHospital.setOnClickListener(v -> {
            if (nearbySearchManager != null && mapManager != null) {
                if (currentLatLng != null) {
                    nearbySearchManager.showNearbyPlaces(
                            mapManager.getSafetyList(),
                            currentLatLng,
                            NearbySearchManager.TYPE_HOSPITAL,
                            3000
                    );
                } else {
                    Toast.makeText(MainActivity.this, "현재 위치 정보를 가져오는 중입니다.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        ivSearchIcon.setOnClickListener(v -> {
            String keyword = etSearch.getText().toString().trim();
            if (mapManager == null) return;

            if (nearbySearchManager != null) {
                nearbySearchManager.clearNearbyMarkers();
            }

            if (!keyword.isEmpty()) {
                mapManager.clearMarkers();
                mapManager.searchAndMoveToGu(keyword);
            } else {
                mapManager.clearGuFilter();
                mapManager.clearMarkers();
                Toast.makeText(MainActivity.this, "전체 지역을 표시합니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupDrawerMenu() {
        Button btnFireStation = findViewById(R.id.btn_fire_station);
        Button btnSafetyCenter = findViewById(R.id.btn_safety_center);
        Button btnHospitalMain = findViewById(R.id.btn_hospital_main);
        layoutHospitalSub = findViewById(R.id.layout_hospital_sub);

        Button btnSubGeneral = findViewById(R.id.btn_sub_general);
        Button btnSubDental = findViewById(R.id.btn_sub_dental);
        Button btnSubPediatrics = findViewById(R.id.btn_sub_pediatrics);
        Button btnSubInternal = findViewById(R.id.btn_sub_internal);
        Button btnSubOrtho = findViewById(R.id.btn_sub_ortho);
        Button btnSubOphthal = findViewById(R.id.btn_sub_ophthal);
        Button btnSubAnpa = findViewById(R.id.btn_sub_anpa);
        Button btnSubDerma = findViewById(R.id.btn_sub_derma);
        Button btnSubFamily = findViewById(R.id.btn_sub_family);
        Button btnSubGs = findViewById(R.id.btn_sub_gs);
        Button btnSubMC = findViewById(R.id.btn_sub_mentalClinic);
        Button btnSubNs = findViewById(R.id.btn_sub_ns);
        Button btnSubPs = findViewById(R.id.btn_sub_ps);
        Button btnSubRadio = findViewById(R.id.btn_sub_radio);
        Button btnSubEtc = findViewById(R.id.btn_sub_etc);
        Button btnSubNeuron = findViewById(R.id.btn_sub_neuron);
        Button btnSubNursing = findViewById(R.id.btn_sub_nursing);
        Button btnSubRehabilitation = findViewById(R.id.btn_sub_rehabilitation);
        Button btnSubUro = findViewById(R.id.btn_sub_uro);

        if (btnFireStation != null) {
            btnFireStation.setOnClickListener(v -> onCategorySelected("소방서", null));
        }

        if (btnSafetyCenter != null) {
            btnSafetyCenter.setOnClickListener(v -> onCategorySelected("안전센터", null));
        }

        if (btnHospitalMain != null) {
            btnHospitalMain.setOnClickListener(v -> {
                isHospitalExpanded = !isHospitalExpanded;
                if (layoutHospitalSub != null) {
                    layoutHospitalSub.setVisibility(isHospitalExpanded ? View.VISIBLE : View.GONE);
                }
                btnHospitalMain.setText(isHospitalExpanded ? "병원 (접기 ▲)" : "병원 (진료과 선택 ▼)");
            });
        }

        if (btnSubGeneral != null) btnSubGeneral.setOnClickListener(v -> onCategorySelected("병원", "일반의원"));
        if (btnSubDental != null) btnSubDental.setOnClickListener(v -> onCategorySelected("병원", "치과"));
        if (btnSubPediatrics != null) btnSubPediatrics.setOnClickListener(v -> onCategorySelected("병원", "소아청소년과"));
        if (btnSubInternal != null) btnSubInternal.setOnClickListener(v -> onCategorySelected("병원", "내과"));
        if (btnSubOrtho != null) btnSubOrtho.setOnClickListener(v -> onCategorySelected("병원", "정형외과"));
        if (btnSubOphthal != null) btnSubOphthal.setOnClickListener(v -> onCategorySelected("병원", "안과"));
        if (btnSubAnpa != null) btnSubAnpa.setOnClickListener(v -> onCategorySelected("병원", "마취통증의학과"));
        if (btnSubDerma != null) btnSubDerma.setOnClickListener(v -> onCategorySelected("병원", "피부과"));
        if (btnSubFamily != null) btnSubFamily.setOnClickListener(v -> onCategorySelected("병원", "가정의학과"));
        if (btnSubGs != null) btnSubGs.setOnClickListener(v -> onCategorySelected("병원", "외과"));
        if (btnSubMC != null) btnSubMC.setOnClickListener(v -> onCategorySelected("병원", "정신건강의학과"));
        if (btnSubNeuron != null) btnSubNeuron.setOnClickListener(v -> onCategorySelected("병원", "신경과"));
        if (btnSubNs != null) btnSubNs.setOnClickListener(v -> onCategorySelected("병원", "신경외과"));
        if (btnSubNursing != null) btnSubNursing.setOnClickListener(v -> onCategorySelected("병원", "요양병원"));
        if (btnSubPs != null) btnSubPs.setOnClickListener(v -> onCategorySelected("병원", "성형외과"));
        if (btnSubRadio != null) btnSubRadio.setOnClickListener(v -> onCategorySelected("병원", "영상의학과"));
        if (btnSubEtc != null) btnSubEtc.setOnClickListener(v -> onCategorySelected("병원", "기타(흉부외과, 방사선과)"));
        if (btnSubRehabilitation != null) btnSubRehabilitation.setOnClickListener(v -> onCategorySelected("병원", "재활의학과"));
        if (btnSubUro != null) btnSubUro.setOnClickListener(v -> onCategorySelected("병원", "비뇨의학과"));

    }

    private void onCategorySelected(String mainCategory, String subCategory) {
        if (mainCategory.equals("소방서")) {
            if (mapManager != null) {
                mapManager.setCategory("fire_station");
                Toast.makeText(this, "소방서를 표시합니다.", Toast.LENGTH_SHORT).show();
            }
        } else if (mainCategory.equals("안전센터")) {
            if (mapManager != null) {
                mapManager.setCategory("safety_center");
                Toast.makeText(this, "안전센터/구조대를 표시합니다.", Toast.LENGTH_SHORT).show();
            }
        } else if (mainCategory.equals("병원")) {
            loadHospitalData(subCategory);
        }

        if (drawerLayout != null) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
    }

    private void loadHospitalData(String subCategory) {
        Intent intent = new Intent(MainActivity.this, HospitalSearchActivity.class);
        intent.putExtra("DEPARTMENT_NAME", subCategory);
        startActivity(intent);
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