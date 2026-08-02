package androidtown.org.a119_big_data;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class SafetyScoreHelper {

    public static class SafetyResult {
        public String name;
        public String district;
        public String type;
        public double score;
        public int fireCount;
        public int waterCount;
        public int totalCasualties;
        public int rescueCount;
        public int elevatorTrafficCount;
        public double avgDispatchTime;
        public String evaluation;

        public SafetyResult(String name , String district, String type, double score,
                            int fireCount, int waterCount, int totalCasualties,
                            int rescueCount, int elevatorTrafficCount, double avgDispatchTime) {
            this.name = name;
            this.district = district;
            this.type = type;
            this.score = score;
            this.fireCount = fireCount;
            this.waterCount = waterCount;
            this.totalCasualties = totalCasualties;
            this.rescueCount = rescueCount;
            this.elevatorTrafficCount = elevatorTrafficCount;
            this.avgDispatchTime = avgDispatchTime;

            if(score >= 80.0) {
                this.evaluation = "안전한 지역입니다. 자취 구역으로 추천합니다";
            } else if (score >= 60.0){
                this.evaluation = "보통 수준의 안전 지역입니다";
            } else {
                this.evaluation = "화재 및 사고 취약 요인이 있어 주의가 필요합니다";
            }
        }
    }

    public static SafetyResult getScoreData(Context context, String keyword) {
        try{
            InputStream is = context.getAssets().open("all_safety_scores.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String jsonStr = new String(buffer, StandardCharsets.UTF_8);

            JSONArray jsonArray = new JSONArray(jsonStr);
            for (int i = 0; i < jsonArray.length(); i++){
                JSONObject obj = jsonArray.getJSONObject(i);
                String name = obj.getString("name");

                if(name.contains(keyword)){
                    return new SafetyResult(
                            name,
                            obj.getString("district"),
                            obj.getString("Type"),
                            obj.getDouble("score"),
                            obj.getInt("fire_count"),
                            obj.getInt("water_count"),
                            obj.getInt("total_casualties"),
                            obj.getInt("rescue_count"),
                            obj.getInt("elevator_traffic_count"),
                            obj.getDouble("avg_dispatch_time")
                    );
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
}
