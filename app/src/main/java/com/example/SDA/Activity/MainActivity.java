package com.example.SDA.Activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.SDA.Class.PreferenceManager;
import com.example.SDA.R;
import com.example.SDA.Service.CameraService;
import com.example.SDA.Service.FCMService;
import com.example.SDA.Service.FirebaseStorageService;
import com.example.SDA.Service.ForegroundService;
import com.example.SDA.View.CameraSurfaceView;

public class MainActivity extends AppCompatActivity {
    public static Context context;

    private Button serviceButton;
    private Button editInfoButton;
    private Button careCallButton;

    private ImageView serviceStateImageView;
    private TextView serviceStateTextView;

    private FCMService messageService;

    private FrameLayout frameLayout;
    private CameraSurfaceView cameraSurfaceView;

    Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] {
                            Manifest.permission.CAMERA,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE}, 200);
        } else {
            initLayout();
            context = getApplicationContext();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (!ForegroundService.isServiceRunning(getApplication())) {
            startService();
        } else {
            updateUI();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode==200 && grantResults.length > 0){
            boolean permissionGranted=true;
            for(int result : grantResults){
                if(result != PackageManager.PERMISSION_GRANTED){
                    // 사용자가 권한을 거절했다.
                    permissionGranted=false;
                    break;
                }
            }

            if (permissionGranted) {
                initLayout();
            } else {
                Toast.makeText(this,
                        "권한을 허용해야 SDA 앱을 이용하실 수 있습니다.",
                        Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void initLayout() {
        setContentView(R.layout.activity_main);

        serviceButton = (Button) findViewById(R.id.serviceButton);
        editInfoButton = (Button) findViewById(R.id.editInfoButton);
        careCallButton = (Button) findViewById(R.id.careCallButton);

        serviceStateImageView = (ImageView) findViewById(R.id.serviceStateImageView);
        serviceStateTextView = (TextView) findViewById(R.id.serviceStateTextView);
        messageService = new FCMService();

        frameLayout = (FrameLayout) findViewById(R.id.frameLayout);

        serviceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!ForegroundService.isServiceRunning(getApplication())) {
                    startService();
                } else {
                    stopService();
                }
            }
        });
        editInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ForegroundService.isServiceRunning(getApplication())) {
                    stopService();
                }
                Intent intent = new Intent(MainActivity.this, EditInformationActivity.class);
                startActivity(intent);
            }
        });
        careCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String careIdToken = PreferenceManager.getString(MainActivity.this, PreferenceManager.CARE_ID_TOKEN);
                String seniorName = PreferenceManager.getString(MainActivity.this, PreferenceManager.NAME);
                if (careIdToken == null) {
                    Toast.makeText(getApplicationContext(), "보호자 정보에 오류가 있습니다.", Toast.LENGTH_SHORT).show();
                    return;
                }
                String msg = seniorName + "님이 보호자님을 호출하였습니다!";
                messageService.sentPostToFCM(careIdToken, msg, "0");
                Toast.makeText(getApplicationContext(), "보호자에게 알림을 보냈습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startService() {
        Toast.makeText(getApplicationContext(), "서비스 시작", Toast.LENGTH_SHORT).show();
        startService(new Intent(getApplication(), ForegroundService.class));
        updateUI();
    }

    private void stopService() {
        Toast.makeText(getApplicationContext(), "서비스 중지", Toast.LENGTH_SHORT).show();
        stopService(new Intent(getApplication(), ForegroundService.class));
        serviceButton.setText(R.string.btn_text_service_on);
        serviceStateImageView.setImageResource(R.drawable.service_state_off);
        serviceStateTextView.setText("정지");
        frameLayout.setVisibility(View.INVISIBLE);
        frameLayout.removeView(cameraSurfaceView);
    }

    private void updateUI() {
        serviceButton.setText(R.string.btn_text_service_off);
        serviceStateImageView.setImageResource(R.drawable.service_state_on);
        serviceStateTextView.setText("동작");
        showPreview();
    }

    private void showPreview() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(CameraService.camera == null) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        frameLayout.removeView(cameraSurfaceView);
                        cameraSurfaceView = new CameraSurfaceView(MainActivity.this, CameraService.camera);
                        frameLayout.addView(cameraSurfaceView);
                        frameLayout.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
        thread.start();
    }
}