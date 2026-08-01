package androidtown.org.a119_big_data;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
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

import java.util.ArrayList;
import java.util.List;

public class SafetyMapManager {

    private final Context context;
    private final KakaoMap kakaoMap;
    private final List<SafetyPlace> safetyList = new ArrayList<>();
    private final List<Label> activeMarkers = new ArrayList<>();

    // ★ 지도 매니저에 등록된 LabelStyles 객체를 보관할 변수
    private LabelStyles markerStyles;

    public interface OnDongSearchListner {
        void onDongSearchResult(String dongName, String guName, int fireStationCount, int safetyCenterCount, List<SafetyPlace> matchedList);
    }

    private OnDongSearchListner dongSearchListner;

    public void setOnDongSearchListener(OnDongSearchListner listener) {
        this.dongSearchListner = listener;
    }

    public SafetyMapManager(Context context, KakaoMap kakaoMap) {
        this.context = context;
        this.kakaoMap = kakaoMap;

        // 생성자 시점에 마커 스타일을 카카오지도에 1회 미리 등록
        initMarkerStyle();

        // 파이어베이스 데이터 수신
        loadFirebaseData();
    }

    // ★ 마커 스타일을 초기 1회만 LabelManager에 등록하는 메서드
    private void initMarkerStyle() {
        if (kakaoMap == null) return;

        Drawable vectorDrawable = ContextCompat.getDrawable(context, R.drawable.ic_red_arrow);
        if (vectorDrawable == null) return;

        int width = vectorDrawable.getIntrinsicWidth() > 0 ? vectorDrawable.getIntrinsicWidth() : 60;
        int height = vectorDrawable.getIntrinsicHeight() > 0 ? vectorDrawable.getIntrinsicHeight() : 60;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        vectorDrawable.draw(canvas);

        LabelStyle style = LabelStyle.from(bitmap).setAnchorPoint(0.5f, 1.0f);

        // ★ 카카오 지도 SDK v2 필수: addLabelStyles()로 매니저에 등록해야만 마커가 그려집니다!
        this.markerStyles = kakaoMap.getLabelManager().addLabelStyles(LabelStyles.from(style));
    }

    private void loadFirebaseData() {
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://bigdata119-719b2-default-rtdb.asia-southeast1.firebasedatabase.app/");
        DatabaseReference ref = database.getReference("safety_places");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                safetyList.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    SafetyPlace item = child.getValue(SafetyPlace.class);
                    if (item != null) {
                        safetyList.add(item);
                    }
                }
                Toast.makeText(context, "불러온 데이터 개수: " + safetyList.size() + "개", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(context, "파이어베이스 오류: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    public void searchAndMoveToGu(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            Toast.makeText(context, "검색어를 입력해 주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        String searchKey = keyword.trim();
        String cleanGu = (searchKey.endsWith("구") && searchKey.length() > 1)
                ? searchKey.substring(0, searchKey.length() - 1)
                : searchKey;

        if (safetyList.isEmpty()) {
            Toast.makeText(context, "데이터를 불러오는 중입니다. 잠시 후 다시 시도해 주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<SafetyPlace> searchResults = new ArrayList<>();

        for (SafetyPlace item : safetyList) {
            boolean isMatch = false;

            if (item.gu != null && (item.gu.contains(searchKey) || item.gu.contains(cleanGu))) {
                isMatch = true;
            } else if (item.name != null && (item.name.contains(searchKey) || item.name.contains(cleanGu))) {
                isMatch = true;
            } else if (item.address != null && item.address.contains(cleanGu)) {
                isMatch = true;
            }

            if (isMatch) {
                searchResults.add(item);
            }
        }

        if (searchResults.isEmpty()) {
            Toast.makeText(context, "'" + searchKey + "'에 대한 검색 결과를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
        } else {
            SafetyPlace firstItem = searchResults.get(0);
            LatLng targetLocation = LatLng.from(firstItem.latitude, firstItem.longitude);

            if (kakaoMap != null) {
                kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(targetLocation, 14));
            }

            displayMarkers(searchResults);
            Toast.makeText(context, searchResults.size() + "개의 장소를 찾았습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    public void searchAndMoveToDong(String keyword){
        if(keyword == null || keyword.trim().isEmpty()){
            return;
        }

        String searchKey = keyword.trim();

        if(safetyList.isEmpty()){
            return;
        }

        List<SafetyPlace> searchResults = new ArrayList<>();
        String detectedGu = "알 수 없음";

        for(SafetyPlace item : safetyList){
            boolean isMatch = false;

            if(item.address != null && item.address.contains(searchKey)) {
                isMatch = true;
            } else if (item.name != null && item.name.contains(searchKey)){
                isMatch=true;
            }

            if(isMatch){
                searchResults.add(item);
                if (item.gu != null && !item.gu.isEmpty()) {
                    detectedGu = item.gu;
                }
            }
        }

        if(!searchResults.isEmpty()){
            int fireStationCount = 0;
            int safetyCenterCount = 0;

            for(SafetyPlace item : searchResults){
                if("소방서".equals(item.typeCategory) || (item.name != null && item.name.endsWith("소방서"))) {
                    fireStationCount++;
                } else {
                    safetyCenterCount++;
                }
            }

            SafetyPlace firstItem = searchResults.get(0);
            LatLng targetLocation = LatLng.from(firstItem.latitude, firstItem.longitude);
            if(kakaoMap != null){
                kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(targetLocation, 14));
            }
            displayMarkers(searchResults);

            if(dongSearchListner != null){
                dongSearchListner.onDongSearchResult(searchKey, detectedGu, fireStationCount, safetyCenterCount, searchResults);
            }
        }
    }

    public void setCategory(String category) {
        if (safetyList.isEmpty()) return;

        List<SafetyPlace> filteredList = new ArrayList<>();

        for (SafetyPlace item : safetyList) {
            if ("fire_station".equals(category)) {
                if ("소방서".equals(item.typeCategory) || (item.name != null && item.name.endsWith("소방서"))) {
                    filteredList.add(item);
                }
            } else if ("safety_center".equals(category)) {
                if ("안전센터/구조대".equals(item.typeCategory) || "safety_center".equals(item.type)
                        || (item.name != null && (item.name.contains("안전센터") || item.name.contains("구조대")))) {
                    filteredList.add(item);
                }
            }
        }
        displayMarkers(filteredList);

        Toast.makeText(context, filteredList.size() + "개의 장소를 표시합니다.", Toast.LENGTH_SHORT).show();
    }

    public void clearGuFilter() {
        displayMarkers(safetyList);
    }

    public void clearMarkers() {
        if (kakaoMap == null) return;

        LabelLayer layer = kakaoMap.getLabelManager().getLayer();
        if (layer == null) return;

        for (Label label : activeMarkers) {
            if (label != null) {
                layer.remove(label);
            }
        }
        activeMarkers.clear();
    }

    public void displayMarkers(List<SafetyPlace> places) {
        if (kakaoMap == null || places == null) return;

        clearMarkers();

        // 스타일이 아직 없다면 초기화
        if (markerStyles == null) {
            initMarkerStyle();
        }
        if (markerStyles == null) return;

        LabelLayer layer = kakaoMap.getLabelManager().getLayer();
        if (layer == null) return;

        int drawCount = 0;

        for (SafetyPlace place : places) {
            if (place.latitude != 0.0 && place.longitude != 0.0) {
                LatLng point = LatLng.from(place.latitude, place.longitude);
                // ★ 미리 등록된 markerStyles를 전달합니다.
                LabelOptions options = LabelOptions.from(point).setStyles(markerStyles);
                Label label = layer.addLabel(options);

                if (label != null) {
                    activeMarkers.add(label);
                    drawCount++;
                }
            }
        }

        if (drawCount == 0 && !places.isEmpty()) {
            Toast.makeText(context, "오류: 장소는 찾았으나 좌표(위도/경도) 값이 모두 0입니다.", Toast.LENGTH_LONG).show();
        }
    }

    public List<SafetyPlace> getSafetyList() {
        return safetyList;
    }
}