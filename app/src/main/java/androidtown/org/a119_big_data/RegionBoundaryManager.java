package androidtown.org.a119_big_data;

import android.content.Context;
import android.graphics.Color;

import com.kakao.vectormap.KakaoMap;
import com.kakao.vectormap.LatLng;
import com.kakao.vectormap.shape.MapPoints;
import com.kakao.vectormap.shape.Polygon;
import com.kakao.vectormap.shape.PolygonOptions;
import com.kakao.vectormap.shape.PolygonStyle;
import com.kakao.vectormap.shape.ShapeLayer;
import com.kakao.vectormap.shape.ShapeLayerOptions;
import com.kakao.vectormap.shape.ShapeManager;

import java.util.ArrayList;
import java.util.List;

public class RegionBoundaryManager {
    private final Context context;
    private final KakaoMap kakaoMap;
    private final List<Polygon> currentPolygons = new ArrayList<>();

    public RegionBoundaryManager(Context context, KakaoMap kakaoMap){
        this.context = context;
        this.kakaoMap = kakaoMap;
    }

    /**
     * 여러 개의 좌표 그룹(구/동 단위)을 받아 연한 분홍색 면과 빨간 테두리 폴리곤을 그림
     */
    public void drawRegionBoundary(List<List<LatLng>> allBoundaryCoords){
        if(kakaoMap == null || allBoundaryCoords == null || allBoundaryCoords.isEmpty()) {
            return;
        }
        clearBoundary();

        ShapeManager shapeManager = kakaoMap.getShapeManager();
        if(shapeManager != null) {
            ShapeLayer layer = shapeManager.getLayer();
            if (layer == null) {
                layer = shapeManager.addLayer(ShapeLayerOptions.from("region_boundary_layer"));
            }

            if (layer != null) {
                // 1. 색상 설정 (연한 분홍색 면, 빨간색 테두리)
                int fillColor = Color.argb(90, 255, 182, 193);
                int strokeColor = Color.RED;
                int strokeWidth = 3;

                // 2. PolygonStyle 생성 (SDK v2 규격에 맞게 스타일 객체 활용)
                PolygonStyle style = PolygonStyle.from(fillColor, strokeWidth, strokeColor);

                for (List<LatLng> coords : allBoundaryCoords) {
                    if (coords != null && !coords.isEmpty()) {
                        MapPoints mapPoints = MapPoints.fromLatLng(coords);

                        // 3. MapPoints와 PolygonStyle을 조합하여 옵션 생성
                        PolygonOptions options = PolygonOptions.from(mapPoints, style);

                        Polygon polygon = layer.addPolygon(options);
                        currentPolygons.add(polygon);
                    }
                }
            }
        }
    }

    /**
     * 그려진 모든 경계선 초기화
     */
    public void clearBoundary() {
        if(kakaoMap != null && !currentPolygons.isEmpty()){
            ShapeManager shapeManager = kakaoMap.getShapeManager();
            if(shapeManager != null) {
                ShapeLayer layer = shapeManager.getLayer();
                if(layer != null){
                    for (Polygon polygon : currentPolygons) {
                        layer.remove(polygon);
                    }
                    currentPolygons.clear();
                }
            }
        }
    }
}