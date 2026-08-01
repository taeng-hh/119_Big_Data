package androidtown.org.a119_big_data;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.widget.Toast;
import androidx.core.content.ContextCompat;

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

public class NearbySearchManager {

    public static final String TYPE_FIRE_STATION = "fire_station";
    public static final String TYPE_SAFETY_CENTER = "safety_center";
    public static final String TYPE_HOSPITAL = "hospital";

    private final Context context;
    private final KakaoMap kakaoMap;
    private final List<Label> nearbyMarkers = new ArrayList<>();
    private Polygon currentCirclePolygon;

    public NearbySearchManager(Context context, KakaoMap kakaoMap) {
        this.context = context;
        this.kakaoMap = kakaoMap;
    }

    public void showNearbyPlaces(List<SafetyPlace> allPlaces, LatLng userLocation, String type, double radiusMeters) {
        if (kakaoMap == null || userLocation == null || allPlaces == null) return;

        clearNearbyMarkers();

        List<SafetyPlace> filteredPlaces = new ArrayList<>();

        for (SafetyPlace place : allPlaces) {
            if (place.latitude == 0.0 || place.longitude == 0.0) continue;

            if (isCategoryMatch(place, type)) {
                double distance = calculateDistance(userLocation.getLatitude(), userLocation.getLongitude(), place.latitude, place.longitude);
                if (distance <= radiusMeters) {
                    filteredPlaces.add(place);
                }
            }
        }

        int radiusKm = (int) (radiusMeters / 1000);
        if (radiusKm < 1) radiusKm = 1;

        drawRadiusCircle(userLocation, radiusMeters);
        moveCameraForRadius(userLocation, radiusMeters);

        if (filteredPlaces.isEmpty()) {
            Toast.makeText(context, "반경 " + radiusKm + "km 이내에 해당 시설이 없습니다.", Toast.LENGTH_SHORT).show();
        } else {
            displayRedArrowMarkers(filteredPlaces);
            Toast.makeText(context, "반경 " + radiusKm + "km 내 " + filteredPlaces.size() + "곳을 표시했습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isCategoryMatch(SafetyPlace place, String category) {
        if (place == null) return false;

        switch (category) {
            case TYPE_FIRE_STATION:
                return "소방서".equals(place.typeCategory) ||
                        "fire_station".equals(place.type) ||
                        (place.name != null && place.name.endsWith("소방서"));

            case TYPE_SAFETY_CENTER:
                return "안전센터/구조대".equals(place.typeCategory) ||
                        "safety_center".equals(place.type) ||
                        (place.name != null && (place.name.contains("안전센터") || place.name.contains("구조대")));

            case TYPE_HOSPITAL:
                return "병원".equals(place.typeCategory) ||
                        "hospital".equals(place.type) ||
                        (place.name != null && (place.name.contains("병원") || place.name.contains("의원")));

            default:
                return false;
        }
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        float[] results = new float[1];
        Location.distanceBetween(lat1, lon1, lat2, lon2, results);
        return results[0];
    }

    public void drawRadiusCircle(LatLng center, double radiusMeters) {
        if (kakaoMap == null || center == null) return;

        ShapeLayer shapeLayer = kakaoMap.getShapeManager().getLayer();
        if (shapeLayer == null) return;

        if (currentCirclePolygon != null) {
            shapeLayer.remove(currentCirclePolygon);
            currentCirclePolygon = null;
        }

        DotPoints dotPoints = DotPoints.fromCircle(center, (float) radiusMeters);

        PolygonStyles styles = PolygonStyles.from(Color.argb(60, 255, 0, 0));
        PolygonStylesSet stylesSet = PolygonStylesSet.from(styles);

        PolygonOptions options = PolygonOptions.from(dotPoints)
                .setStylesSet(stylesSet);

        currentCirclePolygon = shapeLayer.addPolygon(options);
    }

    private void moveCameraForRadius(LatLng center, double radiusMeters) {
        if (kakaoMap == null) return;

        int zoomLevel = 13;

        if (radiusMeters >= 100000) {
            zoomLevel = 8;
        } else if (radiusMeters >= 50000) {
            zoomLevel = 9;
        } else if (radiusMeters >= 20000) {
            zoomLevel = 10;
        } else if (radiusMeters >= 10000) {
            zoomLevel = 11;
        } else if (radiusMeters >= 5000) {
            zoomLevel = 12;
        }

        kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(center, zoomLevel));
    }

    private void clearNearbyMarkersOnly() {
        if (kakaoMap == null) return;

        LabelLayer layer = kakaoMap.getLabelManager().getLayer();
        if (layer == null) return;

        for (Label label : nearbyMarkers) {
            if (label != null) {
                layer.remove(label);
            }
        }
        nearbyMarkers.clear();
    }

    public void clearNearbyMarkers() {
        clearNearbyMarkersOnly();

        if (currentCirclePolygon != null && kakaoMap != null) {
            ShapeLayer shapeLayer = kakaoMap.getShapeManager().getLayer();
            if (shapeLayer != null) {
                shapeLayer.remove(currentCirclePolygon);
            }
            currentCirclePolygon = null;
        }
    }

    private void displayRedArrowMarkers(List<SafetyPlace> places) {
        if (kakaoMap == null || places == null) return;

        clearNearbyMarkersOnly();

        LabelLayer layer = kakaoMap.getLabelManager().getLayer();
        if (layer == null) return;

        Drawable vectorDrawable = ContextCompat.getDrawable(context, R.drawable.ic_red_arrow);
        if (vectorDrawable == null) return;

        int width = vectorDrawable.getIntrinsicWidth() > 0 ? vectorDrawable.getIntrinsicWidth() : 60;
        int height = vectorDrawable.getIntrinsicHeight() > 0 ? vectorDrawable.getIntrinsicHeight() : 60;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        vectorDrawable.draw(canvas);

        LabelStyle style = LabelStyle.from(bitmap);
        style.setAnchorPoint(0.5f, 1.0f);

        LabelStyles styles = kakaoMap.getLabelManager().addLabelStyles(LabelStyles.from(style));

        for (SafetyPlace place : places) {
            LatLng point = LatLng.from(place.latitude, place.longitude);
            LabelOptions options = LabelOptions.from(point).setStyles(styles);
            Label label = layer.addLabel(options);
            if (label != null) {
                nearbyMarkers.add(label);
            }
        }
    }
}