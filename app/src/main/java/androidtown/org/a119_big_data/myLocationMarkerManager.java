package androidtown.org.a119_big_data;

import android.content.Context;

import com.kakao.vectormap.KakaoMap;
import com.kakao.vectormap.LatLng;
import com.kakao.vectormap.camera.CameraUpdateFactory;
import com.kakao.vectormap.label.Label;
import com.kakao.vectormap.label.LabelLayer;
import com.kakao.vectormap.label.LabelLayerOptions;
import com.kakao.vectormap.label.LabelOptions;
import com.kakao.vectormap.label.LabelStyle;
import com.kakao.vectormap.label.LabelStyles;

public class myLocationMarkerManager {

    private final Context context;
    private final KakaoMap kakaoMap;

    private Label myLocationLabel; // 내 위치 핀 마커
    private LatLng currentPosition; // 현재 내 위치 좌표

    public myLocationMarkerManager(Context context, KakaoMap kakaoMap) {
        this.context = context;
        this.kakaoMap = kakaoMap;
    }

    /*
     * GPS 위치가 갱신될 때마다 마커를 재등록하여 확실하게 표시합니다.
     */
    public void updateLocation(double lat, double lng, boolean moveCamera) {
        if (kakaoMap == null) return;

        currentPosition = LatLng.from(lat, lng);

        // 1. 라벨 매니저를 통해 레이어 안전하게 가져오기 (없으면 새로 생성)
        LabelLayer layer = kakaoMap.getLabelManager().getLayer();
        if (layer == null) {
            layer = kakaoMap.getLabelManager().addLayer(LabelLayerOptions.from("my_location_layer"));
        }

        if (layer != null) {
            // 2. 기존에 그려져 있던 내 위치 마커가 있다면 레이어에서 완전히 제거
            if (myLocationLabel != null) {
                layer.remove(myLocationLabel);
                myLocationLabel = null;
            }

            // 3. 혹시 모를 ID 중복 충돌을 방지하기 위해 동일 ID의 마커가 있다면 강제 제거
            Label existingLabel = layer.getLabel("my_location_marker");
            if (existingLabel != null) {
                layer.remove(existingLabel);
            }

            // 4. 마커 스타일 설정 (위치 핀 모양이므로 앵커를 하단 중앙(0.5, 1.0)으로 설정하여 좌표를 정확히 가리키게 함)
            LabelStyle style = LabelStyle.from(R.drawable.ic_red_arrow)
                    .setAnchorPoint(0.5f, 1.0f);

            LabelStyles styles = kakaoMap.getLabelManager().addLabelStyles(LabelStyles.from(style));

            // 5. 새로운 위치에 마커 옵션 생성 및 추가 (ZOrder를 높여서 항상 위에 보이도록 설정)
            LabelOptions options = LabelOptions.from("my_location_marker", currentPosition)
                    .setStyles(styles)
                    .setRank(10000);

            myLocationLabel = layer.addLabel(options);
        }

        // 6. 카메라 이동이 필요할 때만 이동
        if (moveCamera) {
            moveToCurrentLocation();
        }
    }

    /*
     * 내 위치로 카메라(지도 화면)를 줌 인(Zoom In)하며 이동합니다.
     */
    public void moveToCurrentLocation() {
        if (kakaoMap != null && currentPosition != null) {
            kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(currentPosition, 15));
        }
    }
}