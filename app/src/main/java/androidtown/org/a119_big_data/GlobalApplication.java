package androidtown.org.a119_big_data;

import android.app.Application;
import com.kakao.sdk.common.KakaoSdk;

public class GlobalApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // ★ KaKaoSDK -> KakaoSdk 로 대소문자 수정
        KakaoSdk.init(this, "5d3f0a47bed40fc0f67ef9c052865514");
    }
}