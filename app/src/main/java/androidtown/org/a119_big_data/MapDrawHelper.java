package androidtown.org.a119_big_data;

import android.content.Context;
import android.widget.Toast;

import com.kakao.vectormap.KakaoMap;
import com.kakao.vectormap.LatLng;
import com.kakao.vectormap.camera.CameraUpdateFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class MapDrawHelper {

    // 검색한 지역의 위치로 카메라를 이동시키는 헬퍼 메서드
    public static void moveToRegion(KakaoMap kakaoMap, Context context, String keyword) {
        if (kakaoMap == null) return;

        try {
            InputStream is = context.getAssets().open("서울_행정동_경계_2017.geojson");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String jsonStr = new String(buffer, StandardCharsets.UTF_8);

            JSONObject jsonObject = new JSONObject(jsonStr);
            JSONArray features = jsonObject.getJSONArray("features");

            boolean found = false;

            for (int i = 0; i < features.length(); i++) {
                JSONObject feature = features.getJSONObject(i);
                JSONObject properties = feature.getJSONObject("properties");
                String admName = properties.getString("adm_nm");

                if (admName.contains(keyword)) {
                    JSONObject geometry = feature.getJSONObject("geometry");
                    JSONArray coordinates = geometry.getJSONArray("coordinates");

                    // 해당 지역 다각형의 첫 번째 좌표를 중심점으로 활용
                    JSONArray point = coordinates.getJSONArray(0).getJSONArray(0).getJSONArray(0);
                    double lng = point.getDouble(0);
                    double lat = point.getDouble(1);

                    // 카메라 이동 (에러 없이 깔끔하게 동작)
                    kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(LatLng.from(lat, lng)));

                    found = true;
                    break;
                }
            }

            if (!found) {
                Toast.makeText(context, "일치하는 지역을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}