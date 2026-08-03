package androidtown.org.a119_big_data;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class SafetyScoreHelper {

    public static class SafetyResult {
        public String type;       // "행정구" 또는 "행정동"
        public String name;       // 이름 (예: 양천구 또는 화곡3동)
        public double score;      // predicted_safety_score
        public int fireCount;     // fire_count
        public int waterCount;    // water_count (소방용수)
        public int elevatorTrafficCount; // elevator_traffic_count (승강기/교통)
        public String evaluation; // 총평

        public SafetyResult(String type, String name, double score, int fireCount, int waterCount, int elevatorTrafficCount, String evaluation) {
            this.type = type;
            this.name = name;
            this.score = score;
            this.fireCount = fireCount;
            this.waterCount = waterCount;
            this.elevatorTrafficCount = elevatorTrafficCount;
            this.evaluation = evaluation;
        }
    }

    /* 키워드(구 또는 동 이름)를 받아 all_safety_scores.json에서 일치하는 안전 데이터를 찾아 반환*/
    public static SafetyResult getScoreData(Context context, String keyword) {
        try {
            InputStream is = context.getAssets().open("all_safety_scores.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String jsonStr = new String(buffer, StandardCharsets.UTF_8);

            JSONObject jsonObject = new JSONObject(jsonStr);

            // 1. 행정동(all_dongs) 목록에서 먼저 검색 (동 이름이 더 구체적이므로 우선 검색)
            JSONArray dongs = jsonObject.getJSONArray("all_dongs");
            for (int i = 0; i < dongs.length(); i++) {
                JSONObject dong = dongs.getJSONObject(i);
                String neighborhood = dong.getString("neighborhood"); // 예: "화곡3동"

                if (keyword.contains(neighborhood) || neighborhood.contains(keyword)) {
                    String district = dong.getString("district");
                    double score = dong.getDouble("predicted_safety_score");
                    int fireCount = dong.getInt("fire_count");
                    int waterCount = (int) dong.getDouble("water_count");
                    int elevatorCount = dong.getInt("elevator_traffic_count");

                    return new SafetyResult(
                            "행정동",
                            district + " " + neighborhood,
                            score,
                            fireCount,
                            waterCount,
                            elevatorCount,
                            getEvaluation(score)
                    );
                }
            }

            // 행정동에 없으면 행정구(all_districts) 목록에서 검색
            JSONArray districts = jsonObject.getJSONArray("all_districts");
            for (int i = 0; i < districts.length(); i++) {
                JSONObject dist = districts.getJSONObject(i);
                String districtName = dist.getString("district"); // 예: "양천구"

                if (keyword.contains(districtName) || districtName.contains(keyword)) {
                    double score = dist.getDouble("predicted_safety_score");
                    int fireCount = dist.getInt("fire_count");
                    int waterCount = (int) dist.getDouble("water_count");
                    int elevatorCount = dist.getInt("elevator_traffic_count");

                    return new SafetyResult(
                            "행정구",
                            districtName,
                            score,
                            fireCount,
                            waterCount,
                            elevatorCount,
                            getEvaluation(score)
                    );
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            Log.e("SafetyScoreHelper", "JSON 파싱 오류: " + e.getMessage());
        }

        return null;
    }

    // 점수에 따른 간단한 총평 생성 헬퍼 메서드
    private static String getEvaluation(double score) {
        if (score >= 80.0) return "매우 안전함 (우수 지역)";
        else if (score >= 60.0) return "양호함 (보통 지역)";
        else if (score >= 40.0) return "보통 (주의 필요)";
        else return "개선 필요 (위험 지역)";
    }
}