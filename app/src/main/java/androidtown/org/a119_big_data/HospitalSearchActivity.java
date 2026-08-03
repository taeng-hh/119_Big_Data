package androidtown.org.a119_big_data;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.kakao.vectormap.KakaoMap;
import com.kakao.vectormap.KakaoMapReadyCallback;
import com.kakao.vectormap.MapLifeCycleCallback;
import com.kakao.vectormap.MapView;
import com.kakao.vectormap.LatLng;
import com.kakao.vectormap.KakaoMapSdk;

public class HospitalSearchActivity extends AppCompatActivity {

    private MapView mapView;
    private KakaoMap kakaoMap;
    private HospitalMapManager hospitalMapManager;

    private TextView tvDepartmentTitle;
    private EditText etHospitalSearch;
    private ImageView ivHospitalSearchBtn;
    private String selectedDepartment = "내과";

    private FusedLocationProviderClient fusedLocationClient;
    private LatLng currentLatLng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        KakaoMapSdk.init(this, "cc76924995f59841ce76e2bb75d39a65");
        setContentView(R.layout.activity_hospital_search);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (getIntent() != null && getIntent().hasExtra("DEPARTMENT_NAME")) {
            selectedDepartment = getIntent().getStringExtra("DEPARTMENT_NAME");
        }

        tvDepartmentTitle = findViewById(R.id.tv_department_title);
        etHospitalSearch = findViewById(R.id.et_hospital_search);
        ivHospitalSearchBtn = findViewById(R.id.iv_hospital_search_btn);
        mapView = findViewById(R.id.map_view);

        tvDepartmentTitle.setText(selectedDepartment + " 검색 (반경 3km)");

        mapView.start(new MapLifeCycleCallback() {
            @Override
            public void onMapDestroy() {}

            @Override
            public void onMapError(Exception e) {
                e.printStackTrace();
            }
        }, new KakaoMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull KakaoMap map) {
                HospitalSearchActivity.this.kakaoMap = map;
                hospitalMapManager = new HospitalMapManager(HospitalSearchActivity.this, HospitalSearchActivity.this.kakaoMap);

                fetchCurrentLocationAndLoadHospitals();
            }
        });

        ivHospitalSearchBtn.setOnClickListener(v -> {
            String keyword = etHospitalSearch.getText().toString().trim();
            if (hospitalMapManager != null) {
                hospitalMapManager.searchHospitals(keyword);
            }
        });
    }

    private void fetchCurrentLocationAndLoadHospitals() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    currentLatLng = LatLng.from(location.getLatitude(), location.getLongitude());
                } else {
                    currentLatLng = LatLng.from(37.5665, 126.9780);
                }
                loadData();
            }).addOnFailureListener(e -> {
                currentLatLng = LatLng.from(37.5665, 126.9780);
                loadData();
            });
        } else {
            currentLatLng = LatLng.from(37.5665, 126.9780);
            loadData();
        }
    }

    private void loadData() {
        if (hospitalMapManager != null) {
            hospitalMapManager.loadHospitalDataByDepartment(selectedDepartment, currentLatLng, 3000, list -> {
                Toast.makeText(HospitalSearchActivity.this, "반경 3km 내 " + selectedDepartment + " " + list.size() + "곳을 찾았습니다.", Toast.LENGTH_SHORT).show();
            });
        }
    }
}