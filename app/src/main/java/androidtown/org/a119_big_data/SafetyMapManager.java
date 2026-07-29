package androidtown.org.a119_big_data;

import android.content.Context;
import android.widget.Toast;
import androidx.annotation.NonNull;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
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

    public SafetyMapManager(Context context, KakaoMap kakaoMap) {
        this.context = context;
        this.kakaoMap = kakaoMap;

        // 인스턴스 생성 시 파이어베이스 데이터 즉시 수신
        loadFirebaseData();
    }

    // 파이어베이스 safety_places 노드 전체 읽기
    private void loadFirebaseData() {
        // ★ Realtime Database의 실제 데이터 수신 URL을 명시하고, database.getReference()로 연결해야 합니다.
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

    // 검색 기능 (구 이름, 소방서 이름, 주소 통합 검색)
    public void searchAndMoveToGu(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            Toast.makeText(context, "검색어를 입력해 주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        String searchKey = keyword.trim();
        // '종로구' 입력 시 '종로' 키워드도 같이 추출 (소방서 및 주소 매칭용)
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

            // ① 구(gu) 필드 매칭
            if (item.gu != null && (item.gu.contains(searchKey) || item.gu.contains(cleanGu))) {
                isMatch = true;
            }
            // ② 이름(name) 필드 매칭 (gu 필드가 없는 소방서도 검색 성공)
            else if (item.name != null && (item.name.contains(searchKey) || item.name.contains(cleanGu))) {
                isMatch = true;
            }
            // ③ 주소(address) 필드 매칭
            else if (item.address != null && item.address.contains(cleanGu)) {
                isMatch = true;
            }

            if (isMatch) {
                searchResults.add(item);
            }
        }

        // 검색 결과 처리
        if (searchResults.isEmpty()) {
            Toast.makeText(context, "'" + searchKey + "'에 대한 검색 결과를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
        } else {
            // 첫 번째 검색 결과 장소로 카카오 지도 카메라 이동
            SafetyPlace firstItem = searchResults.get(0);
            LatLng targetLocation = LatLng.from(firstItem.latitude, firstItem.longitude);

            if (kakaoMap != null) {
                kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(targetLocation, 14));
            }

            // 검색된 장소들만 지도에 마커 표시
            displayMarkers(searchResults);
            Toast.makeText(context, searchResults.size() + "개의 장소를 찾았습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    // 카테고리 선택 기능 ("fire_station" 또는 "safety_center")
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

    // 검색 필터 초기화 (전체 목록 다시 표시)
    public void clearGuFilter() {
        displayMarkers(safetyList);
    }

    // 안전시설 전용 마커만 지우기 (내 위치 마커에 영향을 주지 않도록 안전하게 처리)
    public void clearMarkers() {
        if (kakaoMap == null) return;

        LabelLayer layer = kakaoMap.getLabelManager().getLayer();
        for (Label label : activeMarkers) {
            if (label != null) {
                layer.remove(label);
            }
        }
        activeMarkers.clear();
    }

    // 장소 리스트를 받아서 지도에 마커(라벨)로 그리기
    public void displayMarkers(List<SafetyPlace> places) {
        if (kakaoMap == null || places == null) return;

        clearMarkers();
        LabelLayer layer = kakaoMap.getLabelManager().getLayer();

        // 1. XML Vector Drawable을 불러오기
        Drawable vectorDrawable = ContextCompat.getDrawable(context, R.drawable.ic_red_arrow);
        if (vectorDrawable == null) {
            Toast.makeText(context, "아이콘 이미지를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Vector를 Bitmap으로 렌더링하기
        Bitmap bitmap = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        vectorDrawable.draw(canvas);

        // ★ 핵심 수정: addLabelStyles로 매니저에 중복 등록하지 않고 바로 스타일 객체만 생성합니다.
        // setAnchorPoint를 통해 마커의 하단 뾰족한 부분이 실제 좌표를 가리키도록 설정합니다.
        LabelStyle style = LabelStyle.from(bitmap).setAnchorPoint(0.5f, 1.0f);
        LabelStyles styles = LabelStyles.from(style);

        int drawCount = 0;

        // 4. 지도에 마커 추가
        for (SafetyPlace place : places) {
            // ★ 중요: 파이어베이스에서 가져온 좌표가 0.0이 아닌 정상적인 값일 때만 그립니다.
            if (place.latitude != 0.0 && place.longitude != 0.0) {
                LatLng point = LatLng.from(place.latitude, place.longitude);
                LabelOptions options = LabelOptions.from(point).setStyles(styles);
                Label label = layer.addLabel(options);

                if (label != null) {
                    activeMarkers.add(label);
                    drawCount++;
                }
            }
        }

        // 만약 리스트에 장소는 있는데 그려진 마커가 하나도 없다면? -> 좌표 파싱 오류!
        if (drawCount == 0 && !places.isEmpty()) {
            Toast.makeText(context, "오류: 장소는 찾았으나 좌표(위도/경도) 값이 모두 0입니다.", Toast.LENGTH_LONG).show();
        }
    }
        // SafetyMapManager.java 맨 아래에 추가해 주세요!

        public List<SafetyPlace> getSafetyList () {
            return safetyList;
        }
}