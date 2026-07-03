package androidtown.org.a119_big_data;

import android.app.Application;
import com.kakao.vectormap.KakaoMapSdk;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        KakaoMapSdk.init(this, "5d3f0a47bed40fc0f67ef9c052865514");
    }
}
