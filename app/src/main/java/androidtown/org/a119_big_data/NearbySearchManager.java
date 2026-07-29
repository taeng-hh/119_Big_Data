package androidtown.org.a119_big_data;

import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.widget.Toast;

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

/**
 * 사용자 현재 위치 기준 반경(거리) 내 근처 시설(소방서, 안전센터/구조대, 병원 등)을
 * 계산하고 카카오 지도 위에 반경 원(Polygon) 및 빨간색 화살표 마커로 표시하는 매니저 클래스
 */
public class NearbySearchManager {

    // 카테고리 상수 정의
    public static final String TYPE_FIRE_STATION = "fire_station";   // 소방서
    public static final String TYPE_SAFETY_CENTER = "safety_center"; // 안전센터 + 구조대 (통합)
    public static final String TYPE_HOSPITAL = "hospital";           // 병원

    private final Context context;
    private final KakaoMap kakaoMap;
    private final List<Label> nearbyMarkers = new ArrayList<>();
    private Polygon currentCirclePolygon; // 반경 표시용 원 다각형 객체

    public NearbySearchManager(Context context, KakaoMap kakaoMap) {
        this.context = context;
        this.kakaoMap = kakaoMap;
    }

    /**
     * 반경 내 시설 검색, 반경 원 표시, 마커 표시 및 카메라 자동 줌 조절
     */
    public void showNearbyPlaces(List<SafetyPlace> allPlaces, LatLng userLocation, String category, double maxDistanceMeters) {
        if (userLocation == null) {
            Toast.makeText(context, "현재 위치 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (allPlaces == null || allPlaces.isEmpty()) {
            Toast.makeText(context, "데이터를 불러오는 중입니다. 잠시 후 다시 시도해 주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<SafetyPlace> filteredPlaces = new ArrayList<>();
        float[] distanceResults = new float[1];

        for (SafetyPlace place : allPlaces) {
            if (place.latitude == 0 || place.longitude == 0) continue;

            // 1. 카테고리 조건 확인
            boolean isTarget = isCategoryMatch(place, category);

            if (isTarget) {
                // 2. 직선거리 계산 (m 단위)
                Location.distanceBetween(
                        userLocation.getLatitude(), userLocation.getLongitude(),
                        place.latitude, place.longitude,
                        distanceResults
                );

                float distanceInMeters = distanceResults[0];

                // 3. 설정한 반경 내 들어오는지 검사
                if (distanceInMeters <= maxDistanceMeters) {
                    filteredPlaces.add(place);
                }
            }
        }

        int radiusKm = (int) (maxDistanceMeters / 1000);

        // 1. 내 위치 중심 반경 원 다각형 그리기
        drawRadiusCircle(userLocation, maxDistanceMeters);

        // 2. 검색 거리에 맞게 지도 카메라 축소 및 위치 이동
        moveCameraForRadius(userLocation, maxDistanceMeters);

        // 3. 지도에 마커 표시 및 결과 토스트 안내
        if (filteredPlaces.isEmpty()) {
            clearNearbyMarkersOnly();
            Toast.makeText(context, "반경 " + radiusKm + "km 이내에 해당 시설이 없습니다.", Toast.LENGTH_SHORT).show();
        } else {
            displayRedArrowMarkers(filteredPlaces);
            Toast.makeText(context, "반경 " + radiusKm + "km 내 " + filteredPlaces.size() + "곳을 표시했습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 카테고리 매칭 로직
     */
    private boolean isCategoryMatch(SafetyPlace place, String category) {
        if (place == null) return false;

        switch (category) {
            case TYPE_FIRE_STATION: // 소방서
                return "소방서".equals(place.typeCategory) ||
                        "fire_station".equals(place.type) ||
                        (place.name != null && place.name.endsWith("소방서"));

            case TYPE_SAFETY_CENTER: // 안전센터 + 구조대 통합
                return "안전센터/구조대".equals(place.typeCategory) ||
                        "safety_center".equals(place.type) ||
                        (place.name != null && (place.name.contains("안전센터") || place.name.contains("구조대")));

            case TYPE_HOSPITAL: // 병원
                return "병원".equals(place.typeCategory) ||
                        "hospital".equals(place.type) ||
                        (place.name != null && (place.name.contains("병원") || place.name.contains("의원")));

            default:
                return false;
        }
    }

    /**
     * 중심점(center)과 반지름(radiusMeters)을 받아 원 형태의 좌표(72개 지점)를 계산하는 수학 함수
     */
    private List<LatLng> createCirclePoints(LatLng center, double radiusMeters) {
        List<LatLng> points = new ArrayList<>();
        double EARTH_RADIUS = 6371000.0; // 지구 반지름 (m)

        double latRad = Math.toRadians(center.getLatitude());
        double lngRad = Math.toRadians(center.getLongitude());
        double d = radiusMeters / EARTH_RADIUS; // 거리 비율

        for (int i = 0; i < 360; i += 5) {
            double bearing = Math.toRadians(i);

            double pointLatRad = Math.asin(Math.sin(latRad) * Math.cos(d) +
                    Math.cos(latRad) * Math.sin(d) * Math.cos(bearing));

            double pointLngRad = lngRad + Math.atan2(Math.sin(bearing) * Math.sin(d) * Math.cos(latRad),
                    Math.cos(d) - Math.sin(latRad) * Math.sin(pointLatRad));

            points.add(LatLng.from(Math.toDegrees(pointLatRad), Math.toDegrees(pointLngRad)));
        }
        return points;
    }

    /**
     * 내 위치를 중심으로 지정된 반경(m) 크기의 반투명 빨간 원 다각형 그리기
     */
    public void drawRadiusCircle(LatLng center, double radiusMeters) {
        if (kakaoMap == null || center == null) return;

        ShapeLayer shapeLayer = kakaoMap.getShapeManager().getLayer();

        // 기존에 그려진 원 다각형 지우기
        if (currentCirclePolygon != null) {
            shapeLayer.remove(currentCirclePolygon);
            currentCirclePolygon = null;
        }

        // 1. 카카오 SDK 내장 기능을 사용하여 반경에 맞는 원 좌표 생성
        DotPoints dotPoints = DotPoints.fromCircle(center, (float) radiusMeters);

        // ★ 2. 수정된 부분: 단일 채우기 색상으로 안전하게 지정 (투명도 60의 반투명 빨간색)
        PolygonStyles styles = PolygonStyles.from(Color.argb(60, 255, 0, 0));
        PolygonStylesSet stylesSet = PolygonStylesSet.from(styles);

        // 3. PolygonOptions 생성 및 지도에 추가
        PolygonOptions options = PolygonOptions.from(dotPoints)
                .setStylesSet(stylesSet);

        currentCirclePolygon = shapeLayer.addPolygon(options);
    }

    /**
     * 검색 반경 크기에 맞게 카카오 지도 카메라 줌 레벨 자동 조절
     */
    private void moveCameraForRadius(LatLng center, double radiusMeters) {
        if (kakaoMap == null) return;

        int zoomLevel = 13; // 기본 3km용 줌 레벨

        if (radiusMeters >= 100000) {      // 100km -> 광역 축소
            zoomLevel = 8;
        } else if (radiusMeters >= 50000) { // 50km
            zoomLevel = 9;
        } else if (radiusMeters >= 20000) { // 20km
            zoomLevel = 10;
        } else if (radiusMeters >= 10000) { // 10km
            zoomLevel = 11;
        } else if (radiusMeters >= 5000) {  // 5km
            zoomLevel = 12;
        }

        kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(center, zoomLevel));
    }

    /**
     * 마커만 지우기
     */
    private void clearNearbyMarkersOnly() {
        if (kakaoMap == null) return;

        LabelLayer layer = kakaoMap.getLabelManager().getLayer();
        for (Label label : nearbyMarkers) {
            if (label != null) {
                layer.remove(label);
            }
        }
        nearbyMarkers.clear();
    }

    /**
     * 마커와 반경 원 전체 지우기
     */
    public void clearNearbyMarkers() {
        clearNearbyMarkersOnly();

        if (currentCirclePolygon != null && kakaoMap != null) {
            ShapeLayer shapeLayer = kakaoMap.getShapeManager().getLayer();
            shapeLayer.remove(currentCirclePolygon);
            currentCirclePolygon = null;
        }
    }

    /**
     * 필터링된 장소들을 지도에 빨간색 화살표 마커로 그리기
     */
    private void displayRedArrowMarkers(List<SafetyPlace> places) {
        if (kakaoMap == null || places == null) return;

        clearNearbyMarkersOnly();
        LabelLayer layer = kakaoMap.getLabelManager().getLayer();

        LabelStyle style = LabelStyle.from(R.drawable.ic_red_arrow);
        style.setAnchorPoint(0.5f, 0.5f);

        LabelStyles styles = kakaoMap.getLabelManager().addLabelStyles(LabelStyles.from(style));

        for (SafetyPlace place : places) {
            LatLng point = LatLng.from(place.latitude, place.longitude);
            LabelOptions options = LabelOptions.from(point).setStyles(styles);
            Label label = layer.addLabel(options);
            nearbyMarkers.add(label);
        }
    }
}