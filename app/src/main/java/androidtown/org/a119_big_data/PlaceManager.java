package androidtown.org.a119_big_data;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class PlaceManager {
    private static final String PREF_NAME = "saved_places_pref";
    private static final String KEY_PLACES = "key_places";

    // 장소 추가하기
    public static void savePlace(Context context, SavedPlace place) {
        List<SavedPlace> places = getSavedPlaces(context);

        // 중복 저장 방지 (이름이 같으면 최신 정보로 갱신)
        for (int i = 0; i < places.size(); i++) {
            if (places.get(i).name.equals(place.name)) {
                places.remove(i);
                break;
            }
        }

        places.add(place);
        saveListToPrefs(context, places);
    }

    // 장소 삭제하기
    public static void removePlace(Context context, String placeName) {
        List<SavedPlace> places = getSavedPlaces(context);
        places.removeIf(p -> p.name.equals(placeName));
        saveListToPrefs(context, places);
    }

    // 저장된 모든 장소 불러오기
    public static List<SavedPlace> getSavedPlaces(Context context) {
        List<SavedPlace> places = new ArrayList<>();
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String jsonStr = prefs.getString(KEY_PLACES, "[]");

        try {
            JSONArray jsonArray = new JSONArray(jsonStr);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                places.add(new SavedPlace(
                        obj.getString("name"),
                        obj.getString("category"),
                        obj.getString("description")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return places;
    }

    // 내부 저장소에 리스트 동기화
    private static void saveListToPrefs(Context context, List<SavedPlace> places) {
        try {
            JSONArray jsonArray = new JSONArray();
            for (SavedPlace p : places) {
                JSONObject obj = new JSONObject();
                obj.put("name", p.name);
                obj.put("category", p.category);
                obj.put("description", p.description);
                jsonArray.put(obj);
            }
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString(KEY_PLACES, jsonArray.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}