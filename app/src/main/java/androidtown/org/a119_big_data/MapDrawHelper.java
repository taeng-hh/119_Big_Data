package androidtown.org.a119_big_data;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.kakao.vectormap.KakaoMap;
import com.kakao.vectormap.LatLng;
import com.kakao.vectormap.camera.CameraUpdateFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MapDrawHelper {

    // 검색된 지역들의 모든 경계 좌표 리스트(List<List<LatLng>>)를 전달하도록 콜백 수정
    public interface OnRegionSelectedListener {
        void onRegionFound(String regionName, List<List<LatLng>> allBoundaryCoords);
    }

    public static void searchAndDrawRegion(KakaoMap kakaoMap, Context context, String keyword, OnRegionSelectedListener listener) {
        if (kakaoMap == null) return;

        try {
            InputStream is = context.getAssets().open("seoul_boundaries.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String jsonStr = new String(buffer, StandardCharsets.UTF_8);

            JSONObject jsonObject = new JSONObject(jsonStr);
            JSONArray features = jsonObject.getJSONArray("features");

            List<List<LatLng>> allBoundaryCoords = new ArrayList<>();
            LatLng firstCenter = null;
            boolean found = false;

            // 키워드를 포함하는 모든 feature(동)를 수집 (구 단위 검색 대응)
            for (int i = 0; i < features.length(); i++) {
                JSONObject feature = features.getJSONObject(i);
                JSONObject properties = feature.getJSONObject("properties");
                String admName = properties.getString("adm_nm"); // 예: "서울특별시 종로구 사직동"

                if (admName.contains(keyword)) {
                    JSONObject geometry = feature.getJSONObject("geometry");
                    String type = geometry.getString("type"); // "Polygon" 또는 "MultiPolygon"
                    JSONArray coordinates = geometry.getJSONArray("coordinates");

                    if ("Polygon".equals(type)) {
                        JSONArray ring = coordinates.getJSONArray(0);
                        List<LatLng> coords = new ArrayList<>();
                        for (int j = 0; j < ring.length(); j++) {
                            JSONArray point = ring.getJSONArray(j);
                            double lng = point.getDouble(0);
                            double lat = point.getDouble(1);
                            coords.add(LatLng.from(lat, lng));
                        }
                        if (!coords.isEmpty()) {
                            allBoundaryCoords.add(coords);
                            if (firstCenter == null) firstCenter = coords.get(0);
                        }
                    } else if ("MultiPolygon".equals(type)) {
                        for (int p = 0; p < coordinates.length(); p++) {
                            JSONArray polygon = coordinates.getJSONArray(p);
                            if (polygon.length() > 0) {
                                JSONArray ring = polygon.getJSONArray(0);
                                List<LatLng> coords = new ArrayList<>();
                                for (int j = 0; j < ring.length(); j++) {
                                    JSONArray point = ring.getJSONArray(j);
                                    double lng = point.getDouble(0);
                                    double lat = point.getDouble(1);
                                    coords.add(LatLng.from(lat, lng));
                                }
                                if (!coords.isEmpty()) {
                                    allBoundaryCoords.add(coords);
                                    if (firstCenter == null) firstCenter = coords.get(0);
                                }
                            }
                        }
                    }
                    found = true;
                }
            }

            if (found && !allBoundaryCoords.isEmpty()) {
                // 첫 번째 좌표를 기준으로 카메라 이동
                kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(firstCenter, 13));

                if (listener != null) {
                    listener.onRegionFound(keyword, allBoundaryCoords);
                }
            } else {
                Toast.makeText(context, "일치하는 지역을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            e.printStackTrace();
            Log.e("MapDrawHelper", "GeoJSON 파싱 에러: " + e.getMessage());
            Toast.makeText(context, "GeoJSON 파싱 오류 발생", Toast.LENGTH_SHORT).show();
        }
    }
}