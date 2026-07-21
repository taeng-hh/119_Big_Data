package androidtown.org.a119_big_data;

import androidx.annotation.NonNull;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class SafetyDataParser {

    public interface SafetyDataCallback {
        void onDataLoaded(ArrayList<SafetyPlace> safetyPlacesList);
        void onError(String errorMessage);
    }

    public static void loadSafetyDataFromFirebase(SafetyDataCallback callback) {
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference("safety_places");

        mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ArrayList<SafetyPlace> safetyPlacesList = new ArrayList<>();

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    SafetyPlace place = dataSnapshot.getValue(SafetyPlace.class);

                    if (place != null) {
                        // "구" 키값이 비어있을 경우 예외 처리 오타 수정 ("gu")
                        if (place.gu == null || place.gu.isEmpty()) {
                            if (dataSnapshot.hasChild("구")) {
                                place.gu = dataSnapshot.child("구").getValue(String.class);
                            } else if (dataSnapshot.hasChild("gu")) {
                                place.gu = dataSnapshot.child("gu").getValue(String.class);
                            }
                        }
                        safetyPlacesList.add(place);
                    }
                }
                callback.onDataLoaded(safetyPlacesList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        });
    }
}