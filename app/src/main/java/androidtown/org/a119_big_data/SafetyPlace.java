package androidtown.org.a119_big_data;

import com.google.firebase.database.PropertyName;

public class SafetyPlace {
    public long id;
    public String name;
    public String type;

    @PropertyName("type_category")
    public String typeCategory;

    public String address;
    public String gu;

    @PropertyName("Phone") // 파이어베이스의 대문자 "Phone" 매핑
    public String phone;

    public double latitude;
    public double longitude;

    @PropertyName("safety_index")
    public Integer safetyIndex; // 값이 없는 항목을 위해 Integer(null 허용)로 설정

    @PropertyName("safety_score")
    public Integer safetyScore; // 값이 없는 항목을 위해 Integer(null 허용)로 설정

    // 파이어베이스용 필수 빈 생성자
    public SafetyPlace() {}
}