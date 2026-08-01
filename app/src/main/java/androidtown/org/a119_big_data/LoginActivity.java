package androidtown.org.a119_big_data;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import com.kakao.sdk.user.UserApiClient;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private Button btnKakaoLogin, btnGoogleLogin;

    // 구글 로그인을 위한 변수들
    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        btnKakaoLogin = findViewById(R.id.btn_kakao_login);
        btnGoogleLogin = findViewById(R.id.btn_google_login);

        db = FirebaseFirestore.getInstance();

        // 1. 카카오 로그인 로직 구현
        btnKakaoLogin.setOnClickListener(v -> {
            // 카카오톡 앱이 설치되어 있는지 확인
            if (UserApiClient.getInstance().isKakaoTalkLoginAvailable(LoginActivity.this)) {
                UserApiClient.getInstance().loginWithKakaoTalk(LoginActivity.this, (token, error) -> {
                    if (error != null) {
                        // 카카오톡 로그인 취소 또는 오류 시 카카오 계정(웹) 로그인으로 시도
                        loginWithKakaoAccount();
                    } else if (token != null) {
                        Toast.makeText(this, "카카오 로그인 성공!", Toast.LENGTH_SHORT).show();
                        navigateToMain();
                    }
                    return null;
                });
            } else {
                // 카카오톡이 없으면 카카오 계정으로 로그인
                loginWithKakaoAccount();
            }
        });

        // 2. 구글 로그인 로직 구현
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail() // 이메일 정보 요청
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

        // 구글 로그인 화면 결과 처리 런처
        // 구글 로그인 화면 결과 처리 런처 수정
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // 어떤 결과 코드가 반환되었는지 확인하기 위한 로그
                    if (result.getResultCode() == RESULT_OK) {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        handleGoogleSignInResult(task);
                    } else {
                        Toast.makeText(this, "구글 로그인 취소 또는 실패 (Result Code: " + result.getResultCode(), Toast.LENGTH_LONG).show();
                    }
                }
        );

        btnGoogleLogin.setOnClickListener(v -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
    }

    // 카카오 계정 로그인 메서드
    private void loginWithKakaoAccount() {
        UserApiClient.getInstance().loginWithKakaoAccount(LoginActivity.this, (token, error) -> {
            if (error != null) {
                Toast.makeText(this, "카카오 로그인 실패: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            } else if (token != null) {
                fetchKakaoUserInfo();
            }
            return null;
        });
    }

    private void fetchKakaoUserInfo() {
        UserApiClient.getInstance().me((user, error) -> {
            if(error != null) {
                Toast.makeText(this, "사용자 정보 요청 실패 : " + error.getMessage(), Toast.LENGTH_SHORT).show();
            } else if (user != null) {
                String userId = String.valueOf(user.getId());
                String email = (user.getKakaoAccount() != null && user.getKakaoAccount().getEmail() != null)
                        ? user.getKakaoAccount().getEmail()
                        : "Kakao_" + userId + "@social.com";
                saveUserToFirestore(userId, email, "Kakao");
            }
            return null ;
        });
    }

    // 구글 로그인 결과 처리 메서드
    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account != null) {
                String email = account.getEmail();
                Toast.makeText(this, "구글 로그인 성공 (" + email + ")", Toast.LENGTH_SHORT).show();
                navigateToMain();
            }
        } catch (ApiException e) {
            Toast.makeText(this, "구글 로그인 실패: " + e.getStatusCode(), Toast.LENGTH_SHORT).show();
        }
    }

    private void saveUserToFirestore(String userId, String email, String provider) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("userId", userId);
        userData.put("email", email);
        userData.put("provider", provider);
        userData.put("lastLogin", FieldValue.serverTimestamp());


        db.collection("Users").document(email)
                .set(userData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, provider + "로그인 및 회원가입 완료", Toast.LENGTH_SHORT).show();
                    navigateToMain();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "데이터베이스 저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // 메인 화면으로 이동하는 공통 메서드
    private void navigateToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}