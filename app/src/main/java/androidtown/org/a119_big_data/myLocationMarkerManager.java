package androidtown.org.a119_big_data;

import android.content.Context;

import com.kakao.vectormap.KakaoMap;
import com.kakao.vectormap.LatLng;
import com.kakao.vectormap.camera.CameraUpdateFactory;
import com.kakao.vectormap.label.Label;
import com.kakao.vectormap.label.LabelLayer;
import com.kakao.vectormap.label.LabelOptions;
import com.kakao.vectormap.label.LabelStyle;
import com.kakao.vectormap.label.LabelStyles;

public class myLocationMarkerManager {

    private final Context context;
    private final KakaoMap kakaoMap;

    private Label myLocationLabel; // 내 위치 파란 점 마커
    private LatLng currentPosition; // 현재 내 위치 좌표

    public myLocationMarkerManager(Context context, KakaoMap kakaoMap) {
        this.context = context;
        this.kakaoMap = kakaoMap;
    }

    /*
     * GPS 위치가 갱신될 때마다 파란 점 마커의 위치를 업데이트합니다.
     */
    public void updateLocation(double lat, double lng, boolean moveCamera) {
        if (kakaoMap == null) return;

        currentPosition = LatLng.from(lat, lng);
        LabelLayer layer = kakaoMap.getLabelManager().getLayer();

        // 1. 파란 점 마커가 아직 지도에 없으면 새로 생성
        if (myLocationLabel == null) {
            // ★ 주의: R.drawable.ic_blue_dot 파일은 반드시 .png 파일이어야 합니다!
            LabelStyle style = LabelStyle.from(android.R.drawable.btn_star_big_on)
                    .setAnchorPoint(0.5f, 0.5f); // 중심점을 이미지의 정중앙으로 설정

            LabelStyles styles = kakaoMap.getLabelManager().addLabelStyles(LabelStyles.from(style));

            // 내 위치 마커는 다른 마커들보다 항상 위에 보이도록 ZOrder를 높게 설정(10000)
            LabelOptions options = LabelOptions.from("my_location_marker", currentPosition)
                    .setStyles(styles)
                    .setRank(10000);

            myLocationLabel = layer.addLabel(options);
        }
        // 2. 이미 파란 점이 있다면 위치만 스르륵 이동시킴
        else {
            myLocationLabel.moveTo(currentPosition);
        }

        // 카메라 이동이 필요할 때만 이동
        if (moveCamera) {
            moveToCurrentLocation();
        }
    }

    /*
     * 내 위치로 카메라(지도 화면)를 줌 인(Zoom In)하며 이동합니다.
     */
    public void moveToCurrentLocation() {
        if (kakaoMap != null && currentPosition != null) {
            // 줌 레벨 15 정도로 내 위치를 확대해서 보여줌
            kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(currentPosition, 15));
        }
    }
}