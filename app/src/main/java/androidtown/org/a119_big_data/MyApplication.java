package androidtown.org.a119_big_data;

import android.app.Application;
import com.kakao.vectormap.KakaoMapSdk;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        KakaoMapSdk.init(this, "cc76924995f59841ce76e2bb75d39a65");
    }
}
