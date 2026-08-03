package androidtown.org.a119_big_data;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import android.content.Intent;

public class CategoryMenuDialog extends BottomSheetDialogFragment {

    private GridLayout layoutHospitalSub;
    private boolean isHopitalExpanded = false;
    private OnCategorySelectedListener listener;

    public interface OnCategorySelectedListener{
        void onCategorySelected(String mainCategory, String subCategory);
    }

    public void setOnCategorySelectedListener(OnCategorySelectedListener listener){
        this.listener = listener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.bottom_sheet_menu, container, false);

        Button btnFireStation = view.findViewById(R.id.btn_fire_station);
        Button btnSafetyCenter = view.findViewById(R.id.btn_safety_center);
        Button btnHospitalMain = view.findViewById(R.id.btn_hospital_main);
        Button btnDistrictRisk = view.findViewById(R.id.btn_district_risk);
        layoutHospitalSub = view.findViewById(R.id.layout_hospital_sub);

        Button btnSubGeneral = view.findViewById(R.id.btn_sub_general);
        Button btnSubDental = view.findViewById(R.id.btn_sub_dental);
        Button btnSubPediatrics = view.findViewById(R.id.btn_sub_pediatrics);
        Button btnSubInternal = view.findViewById(R.id.btn_sub_internal);
        Button btnSubOrtho = view.findViewById(R.id.btn_sub_ortho);
        Button btnSubOphthal = view.findViewById(R.id.btn_sub_ophthal);
        Button btnSubAnpa = view.findViewById(R.id.btn_sub_anpa);
        Button btnSubDerma = view.findViewById(R.id.btn_sub_derma);
        Button btnSubFamily = view.findViewById(R.id.btn_sub_family);
        Button btnSubGs = view.findViewById(R.id.btn_sub_gs);
        Button btnSubMC = view.findViewById(R.id.btn_sub_mentalClinic);
        Button btnSubNs = view.findViewById(R.id.btn_sub_ns);
        Button btnSubPs = view.findViewById(R.id.btn_sub_ps);
        Button btnSubRadio = view.findViewById(R.id.btn_sub_radio);
        Button btnSubEtc = view.findViewById(R.id.btn_sub_etc);
        Button btnSubNeuron = view.findViewById(R.id.btn_sub_neuron);
        Button btnSubNursing = view.findViewById(R.id.btn_sub_nursing);
        Button btnSubRehabilitation = view.findViewById(R.id.btn_sub_rehabilitation);

        btnFireStation.setOnClickListener(v -> sendSelection("소방서", null));
        btnSafetyCenter.setOnClickListener(v -> sendSelection("안전센터", null));
        btnDistrictRisk.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), DistrictRiskActivity.class);
            startActivity(intent);
            dismiss();
        });
        btnHospitalMain.setOnClickListener(v -> {
            isHopitalExpanded = !isHopitalExpanded;
            layoutHospitalSub.setVisibility(isHopitalExpanded ? View.VISIBLE : View.GONE);
            btnHospitalMain.setText(isHopitalExpanded ? "병원 (접기 ▲)" : "병원 (진료과 선택)");
        });

        btnSubGeneral.setOnClickListener(v -> sendSelection("병원", "일반의원"));
        btnSubDental.setOnClickListener(v -> sendSelection("병원", "치과"));
        btnSubPediatrics.setOnClickListener(v -> sendSelection("병원", "소아청소년과"));
        btnSubInternal.setOnClickListener(v -> sendSelection("병원", "내과"));
        btnSubOrtho.setOnClickListener(v -> sendSelection("병원", "정형외과"));
        btnSubOphthal.setOnClickListener(v -> sendSelection("병원", "안과"));
        btnSubAnpa.setOnClickListener(v -> sendSelection("병원", "마취통증의학과"));
        btnSubDerma.setOnClickListener(v -> sendSelection("병원", "피부과"));
        btnSubEtc.setOnClickListener(v-> sendSelection("병원", "기타(흉부외과, 방사선과)"));
        btnSubFamily.setOnClickListener(v-> sendSelection("병원", "가정의학과"));
        btnSubGs.setOnClickListener(v -> sendSelection("병원", "외과"));
        btnSubMC.setOnClickListener(v -> sendSelection("병원", "정신건강의학과"));
        btnSubNeuron.setOnClickListener( v -> sendSelection("병원", "신경과"));
        btnSubNs.setOnClickListener(v -> sendSelection("병원", "신경외과"));
        btnSubNursing.setOnClickListener(v -> sendSelection("병원", "요양병원"));
        btnSubPs.setOnClickListener(v -> sendSelection("병원", "성형외과"));
        btnSubRadio.setOnClickListener(v -> sendSelection("병원", "영상의학과"));
        btnSubRehabilitation.setOnClickListener(v -> sendSelection("병원", "재활의학과"));

        return view;
    }

    private void sendSelection(String main, String sub){
        if(listener != null){
            listener.onCategorySelected(main, sub);
        }
        dismiss();
    }
}
