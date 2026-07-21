package androidtown.org.a119_big_data;

import com.google.firebase.database.PropertyName;

public class SafetyPlace {
    public long id;
    public String name;
    public String type; // "fire_station" 또는 "safety_center"
    public String address;
    public String gu;
    public String phone;
    public double latitude;

    // JSON의 "longitude" 키값을 자바의 longtitude 변수에 매핑
    @PropertyName("longitude")
    public double longtitude;

    // JSON의 "safety_index" 키값 매핑
    @PropertyName("safety_index")
    public int safetyIndex;

    // JSON의 "safety_score" 키값 매핑
    @PropertyName("safety_score")
    public int safetyScore;

    // 파이어베이스용 필수 빈 생성자
    public SafetyPlace() {
    }
}