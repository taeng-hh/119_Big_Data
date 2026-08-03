package androidtown.org.a119_big_data;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class DistrictRiskActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_district_risk);

        ImageButton btnBack = findViewById(R.id.btn_back);
        ListView listView = findViewById(R.id.list_district_risk);
        TextView emptyView = findViewById(R.id.tv_empty_ranking);

        btnBack.setOnClickListener(v -> finish());
        listView.setEmptyView(emptyView);

        try {
            List<JSONObject> districts = loadDistricts();

            // 위험도 = 100 - 안전 점수이므로, 안전 점수가 낮은 지역이 먼저입니다.
            districts.sort(Comparator.comparingDouble(
                    (JSONObject item) ->
                            item.optDouble("predicted_safety_score", 0.0)
            ));

            List<String> ranking = new ArrayList<>();

            for (int i = 0; i < districts.size(); i++) {
                JSONObject item = districts.get(i);

                double safetyScore = item.getDouble("predicted_safety_score");
                double riskScore = Math.max(0.0, Math.min(100.0, 100.0 - safetyScore));

                ranking.add(String.format(
                        Locale.KOREA,
                        "%d위  %s   위험도 %.2f점",
                        i + 1,
                        item.getString("district"),
                        riskScore
                ));
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_list_item_1,
                    ranking
            );

            listView.setAdapter(adapter);

        } catch (Exception e) {
            emptyView.setVisibility(View.VISIBLE);
            emptyView.setText("안전 점수 데이터를 불러오지 못했습니다.");

            Toast.makeText(
                    this,
                    "데이터를 불러오는 중 오류가 발생했습니다.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private List<JSONObject> loadDistricts() throws Exception {
        StringBuilder json = new StringBuilder();

        try (InputStream inputStream =
                     getAssets().open("all_safety_scores.json");
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(
                             inputStream,
                             StandardCharsets.UTF_8
                     ))) {

            String line;

            while ((line = reader.readLine()) != null) {
                json.append(line);
            }
        }

        JSONArray array = new JSONObject(json.toString())
                .getJSONArray("all_districts");

        List<JSONObject> districts = new ArrayList<>();

        for (int i = 0; i < array.length(); i++) {
            districts.add(array.getJSONObject(i));
        }

        return districts;
    }
}
