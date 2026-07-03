package androidtown.org.a119_big_data;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.kakao.vectormap.KakaoMap;
import com.kakao.vectormap.KakaoMapReadyCallback;
import com.kakao.vectormap.MapLifeCycleCallback;
import com.kakao.vectormap.MapView;

public class MainActivity extends AppCompatActivity {

    private MapView mapView;
    private KakaoMap kakaoMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            android.content.pm.PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), android.content.pm.PackageManager.GET_SIGNATURES);
            for (android.content.pm.Signature signature : info.signatures) {
                java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                android.util.Log.d("🚨진짜키해시🚨", android.util.Base64.encodeToString(md.digest(), android.util.Base64.DEFAULT).trim());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        mapView = findViewById(R.id.map_view);

        // 두 개의 콜백(라이프사이클, 준비완료)을 쉼표(,)로 연결하여 지도 시작
        mapView.start(new MapLifeCycleCallback() {
            @Override
            public void onMapDestroy() {
                // 지도 소멸 시 처리 (비워두셔도 됩니다)
            }
            @Override
            public void onMapError(Exception e) {
                // 지도 초기화 실패 시 에러 로그 출력
                e.printStackTrace();
            }
        }, new KakaoMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull KakaoMap map) {
                // 지도가 성공적으로 켜지면 전역 변수에 저장
                kakaoMap = map;
            }
        });
    }
}