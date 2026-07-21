package androidtown.org.a119_big_data;

import android.content.Context;
import android.graphics.Color;
import android.widget.Toast;

import com.kakao.vectormap.KakaoMap;
import com.kakao.vectormap.LatLng;
import com.kakao.vectormap.camera.CameraUpdateFactory;
import com.kakao.vectormap.label.LabelOptions;
import com.kakao.vectormap.label.LabelStyle;
import com.kakao.vectormap.label.LabelStyles;
import com.kakao.vectormap.label.LabelTextBuilder;

import java.util.ArrayList;

public class SafetyMapManager {

    private Context context;
    private KakaoMap kakaoMap;
    private ArrayList<SafetyPlace> safetyPlacesList = new ArrayList<>();

    private String currentSelectedGu = "";
    private String currentSelectedType = "fire_station";

    public interface OnMapClearedListener {
        void onMapCleared();
    }
    private OnMapClearedListener clearedListener;

    public SafetyMapManager(Context context, KakaoMap kakaoMap) {
        this.context = context;
        this.kakaoMap = kakaoMap;

        // 파이어베이스에서 병합된 데이터를 불러옴
        SafetyDataParser.loadSafetyDataFromFirebase(new SafetyDataParser.SafetyDataCallback() {
            @Override
            public void onDataLoaded(ArrayList<SafetyPlace> downloadedList) {
                safetyPlacesList.clear();
                safetyPlacesList.addAll(downloadedList);

                // 지도에 필터링된 마커 표시
                showFilteredMarkers();

                Toast.makeText(context, "파이어베이스 데이터 로드 완료! (" + safetyPlacesList.size() + "개)", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(context, "데이터 불러오기 실패: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void setOnMapClearedListener(OnMapClearedListener listener) {
        this.clearedListener = listener;
    }

    // 카테고리 변경 (fire_station / safety_center)
    public void setCategory(String type) {
        this.currentSelectedType = type;
        showFilteredMarkers();
    }

    // 구 필터 초기화
    public void clearGuFilter() {
        this.currentSelectedGu = "";
        showFilteredMarkers();
    }

    // 특정 구 검색 및 이동
    public void searchAndMoveToGu(String guName) {
        if (kakaoMap == null) return;

        double totalLat = 0;
        double totalLng = 0;
        int count = 0;
        String matchedGuRealName = "";

        for (SafetyPlace place : safetyPlacesList) {
            if ((place.gu != null && place.gu.contains(guName)) || (place.address != null && place.address.contains(guName))) {
                totalLat += place.latitude;
                totalLng += place.longtitude;
                count++;
                if (matchedGuRealName.isEmpty() && place.gu != null) {
                    matchedGuRealName = place.gu;
                }
            }
        }

        if (count > 0) {
            currentSelectedGu = matchedGuRealName;

            double avgLat = totalLat / count;
            double avgLng = totalLng / count;
            LatLng guCenter = LatLng.from(avgLat, avgLng);

            kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(guCenter));
            showFilteredMarkers();

            Toast.makeText(context, currentSelectedGu + "로 이동합니다.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "입력하신 '구'를 찾을 수 없습니다. (예: 종로구)", Toast.LENGTH_SHORT).show();
        }
    }

    // 필터링된 마커 표시 로직
    public void showFilteredMarkers() {
        if (kakaoMap == null || safetyPlacesList.isEmpty()) return;

        kakaoMap.getLabelManager().getLayer().removeAll();
        if (clearedListener != null) {
            clearedListener.onMapCleared();
        }

        for (SafetyPlace place : safetyPlacesList) {
            boolean matchesGu = currentSelectedGu.isEmpty() || (place.gu != null && place.gu.equals(currentSelectedGu));
            boolean matchesType = place.type != null && place.type.equalsIgnoreCase(currentSelectedType);

            if (matchesGu && matchesType) {
                LatLng position = LatLng.from(place.latitude, place.longtitude);

                LabelStyles styles = kakaoMap.getLabelManager()
                        .addLabelStyles(LabelStyles.from(LabelStyle.from(android.R.drawable.ic_dialog_map)
                                .setTextStyles(35, Color.BLACK)));

                LabelOptions options = LabelOptions.from(position)
                        .setStyles(styles)
                        .setTexts(new LabelTextBuilder().setTexts(place.name + " (안전지수: " + place.safetyIndex + ")"));

                kakaoMap.getLabelManager().getLayer().addLabel(options);
            }
        }
    }
}