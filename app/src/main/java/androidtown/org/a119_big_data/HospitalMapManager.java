package androidtown.org.a119_big_data;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import com.kakao.vectormap.KakaoMap;
import com.kakao.vectormap.LatLng;
import com.kakao.vectormap.camera.CameraUpdateFactory;
import com.kakao.vectormap.label.Label;
import com.kakao.vectormap.label.LabelLayer;
import com.kakao.vectormap.label.LabelOptions;
import com.kakao.vectormap.label.LabelStyle;
import com.kakao.vectormap.label.LabelStyles;
import com.kakao.vectormap.shape.DotPoints;
import com.kakao.vectormap.shape.Polygon;
import com.kakao.vectormap.shape.PolygonOptions;
import com.kakao.vectormap.shape.PolygonStyles;
import com.kakao.vectormap.shape.PolygonStylesSet;
import com.kakao.vectormap.shape.ShapeLayer;

import java.util.ArrayList;
import java.util.List;

public class HospitalMapManager {
    private final Context context;
    private final KakaoMap kakaoMap;
    private final List<HospitalPlace> departmentHospitals = new ArrayList<>();
    private final List<Label> activeMarkers = new ArrayList<>();
    private LabelStyles markerStyles;
    private Polygon currentCirclePolygon;

    public HospitalMapManager(Context context, KakaoMap kakaoMap) {
        this.context = context;
        this.kakaoMap = kakaoMap;
        initMarkerStyle();
    }

    private void initMarkerStyle() {
        if (kakaoMap == null || kakaoMap.getLabelManager() == null) return;

        Drawable vectorDrawable = ContextCompat.getDrawable(context, R.drawable.ic_red_arrow);
        if (vectorDrawable == null) return;

        int width = vectorDrawable.getIntrinsicWidth() > 0 ? vectorDrawable.getIntrinsicWidth() : 60;
        int height = vectorDrawable.getIntrinsicHeight() > 0 ? vectorDrawable.getIntrinsicHeight() : 60;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        vectorDrawable.draw(canvas);

        LabelStyle style = LabelStyle.from(bitmap).setAnchorPoint(0.5f, 1.0f);
        this.markerStyles = kakaoMap.getLabelManager().addLabelStyles(LabelStyles.from(style));
    }

    // ★ 신경외과 / 신경과 키워드 우선순위 매칭 수정
    private String getNodeNameByDepartment(String dept) {
        if (dept == null || dept.isEmpty()) return "seoul_hospital_내과";

        if (dept.contains("재활")) return "Seoul_hospital_재활의학과";
        if (dept.contains("가정")) return "seoul_hospital_가정의학과";
        if (dept.contains("기타")) return "seoul_hospital_기타병원";
        if (dept.contains("내과")) return "seoul_hospital_내과";
        if (dept.contains("마취") || dept.contains("통증")) return "seoul_hospital_마취통증의학과";
        if (dept.contains("비뇨")) return "seoul_hospital_비뇨기과";
        if (dept.contains("성형")) return "seoul_hospital_성형외과";
        if (dept.contains("소아")) return "seoul_hospital_소아과";

        // ★ 중요: "신경외과" 조건을 "신경과"보다 먼저 체크해야 잘못 매칭되지 않습니다.
        if (dept.contains("신경외과")) return "seoul_hospital_신경외과";
        if (dept.contains("신경과")) return "seoul_hospital_신경과";

        if (dept.contains("안과")) return "seoul_hospital_안과";
        if (dept.contains("영상")) return "seoul_hospital_영상의학과";
        if (dept.contains("외과")) return "seoul_hospital_외과";
        if (dept.contains("요양")) return "seoul_hospital_요양병원";
        if (dept.contains("일반") || dept.contains("의원")) return "seoul_hospital_일반의원";
        if (dept.contains("정신")) return "seoul_hospital_정신의학과";
        if (dept.contains("정형")) return "seoul_hospital_정형외과";
        if (dept.contains("치과")) return "seoul_hospital_치과";
        if (dept.contains("피부")) return "seoul_hospital_피부과";

        return "seoul_hospital_" + dept;
    }

    public void loadHospitalDataByDepartment(String targetDepartment, LatLng userLocation, double radiusMeters, DataLoadCallback callback) {
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://bigdata119-719b2-default-rtdb.asia-southeast1.firebasedatabase.app/");

        String nodeName = getNodeNameByDepartment(targetDepartment);
        DatabaseReference ref = database.getReference(nodeName);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                departmentHospitals.clear();

                if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                    Toast.makeText(context, "'" + nodeName + "' 노드에 데이터가 없습니다.", Toast.LENGTH_SHORT).show();
                    return;
                }

                for (DataSnapshot child : snapshot.getChildren()) {
                    HospitalPlace item = child.getValue(HospitalPlace.class);
                    if (item != null && item.latitude != 0.0 && item.longitude != 0.0) {

                        if (userLocation != null) {
                            double distance = calculateDistance(userLocation.getLatitude(), userLocation.getLongitude(), item.latitude, item.longitude);
                            if (distance <= radiusMeters) {
                                departmentHospitals.add(item);
                            }
                        } else {
                            departmentHospitals.add(item);
                        }
                    }
                }

                if (userLocation != null) {
                    drawRadiusCircle(userLocation, radiusMeters);
                    kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(userLocation, 13));
                }

                displayMarkers(departmentHospitals);

                if (callback != null) callback.onLoaded(departmentHospitals);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(context, "파이어베이스 오류: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void searchHospitals(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            displayMarkers(departmentHospitals);
            return;
        }

        String searchKey = keyword.trim();
        List<HospitalPlace> searchResults = new ArrayList<>();

        for (HospitalPlace item : departmentHospitals) {
            boolean isMatch = (item.name != null && item.name.contains(searchKey)) ||
                    (item.address != null && item.address.contains(searchKey)) ||
                    (item.gu != null && item.gu.contains(searchKey));

            if (isMatch) {
                searchResults.add(item);
            }
        }

        if (!searchResults.isEmpty()) {
            HospitalPlace first = searchResults.get(0);
            if (kakaoMap != null) {
                kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(LatLng.from(first.latitude, first.longitude), 14));
            }
        }

        displayMarkers(searchResults);
        Toast.makeText(context, searchResults.size() + "개의 병원을 찾았습니다.", Toast.LENGTH_SHORT).show();
    }

    public void drawRadiusCircle(LatLng center, double radiusMeters) {
        if (kakaoMap == null || center == null || kakaoMap.getShapeManager() == null) return;

        ShapeLayer shapeLayer = kakaoMap.getShapeManager().getLayer();
        if (shapeLayer == null) return;

        if (currentCirclePolygon != null) {
            shapeLayer.remove(currentCirclePolygon);
            currentCirclePolygon = null;
        }

        DotPoints dotPoints = DotPoints.fromCircle(center, (float) radiusMeters);
        PolygonStyles styles = PolygonStyles.from(Color.argb(50, 255, 0, 0));
        PolygonStylesSet stylesSet = PolygonStylesSet.from(styles);

        PolygonOptions options = PolygonOptions.from(dotPoints).setStylesSet(stylesSet);
        currentCirclePolygon = shapeLayer.addPolygon(options);
    }

    public void displayMarkers(List<HospitalPlace> places) {
        if (kakaoMap == null || places == null || kakaoMap.getLabelManager() == null) return;
        clearMarkers();

        if (markerStyles == null) initMarkerStyle();

        LabelLayer layer = kakaoMap.getLabelManager().getLayer();
        if (layer == null || markerStyles == null) return;

        for (HospitalPlace place : places) {
            if (place.latitude != 0.0 && place.longitude != 0.0) {
                LatLng point = LatLng.from(place.latitude, place.longitude);
                LabelOptions options = LabelOptions.from(point).setStyles(markerStyles);
                Label label = layer.addLabel(options);
                if (label != null) activeMarkers.add(label);
            }
        }
    }

    public void clearMarkers() {
        if (kakaoMap == null || kakaoMap.getLabelManager() == null) return;

        LabelLayer layer = kakaoMap.getLabelManager().getLayer();
        if (layer == null) return;

        for (Label label : activeMarkers) {
            if (label != null) layer.remove(label);
        }
        activeMarkers.clear();
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        float[] results = new float[1];
        Location.distanceBetween(lat1, lon1, lat2, lon2, results);
        return results[0];
    }

    public interface DataLoadCallback {
        void onLoaded(List<HospitalPlace> list);
    }
}