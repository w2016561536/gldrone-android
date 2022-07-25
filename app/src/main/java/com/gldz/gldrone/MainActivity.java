package com.gldz.gldrone;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;

import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.MAVLink.MAVLinkPacket;
import com.MAVLink.Parser;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private int[] ch = {1500,1500,1000,1500};

    private SensorManager mSensorManager;
    private Sensor mGyroSensor;
    private Sensor mAccSensor;
    private Sensor mMagSensor;

    // 加速度传感器数据
    float mAccValues[] = new float[3];
    // 地磁传感器数据
    float mMagValues[] = new float[3];
    // 旋转矩阵，用来保存磁场和加速度的数据
    float mRMatrix[] = new float[9];
    // 存储方向传感器的数据（原始数据为弧度）
    float mPhoneAngleValues[] = new float[3];

    Button bt_setting = null;
    TextView tv_ch1 = null;
    TextView tv_ch2 = null;
    TextView tv_ch3 = null;
    TextView tv_ch4 = null;
    TextView tv_mode = null;
    TextView tv_connect = null;
    TextView tv_arm = null;
    RockerView rv_left = null;
    RockerView rv_right = null;

    Mavlink mavlink;

    int flyMode = 0,flyModelast = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        Window window = getWindow();
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        int flag=WindowManager.LayoutParams.FLAG_FULLSCREEN;
        window.setFlags(flag, flag);

        setContentView(R.layout.activity_main);
        // KEEP_SCREEN_ON
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        tv_ch1 = (TextView)findViewById(R.id.tv_ch1);
        tv_ch2 = (TextView)findViewById(R.id.tv_ch2);
        tv_ch3 = (TextView)findViewById(R.id.tv_ch3);
        tv_ch4 = (TextView)findViewById(R.id.tv_ch4);
        tv_mode = (TextView)findViewById(R.id.tv_mode);
        tv_connect = (TextView)findViewById(R.id.tv_connect);
        tv_arm = (TextView)findViewById(R.id.tv_arm);

        bt_setting = (Button) findViewById(R.id.bt_setting);

        mavlink = new Mavlink();
        mavlink.setMavlinkListener(new Mavlink.MavlinkListener() {
            @Override
            public void mode(Mavlink.PX4_MODE mode) {

                runOnUiThread(new Runnable() {
                    public void run() {
                        tv_mode.setText(mavlink.px4ModeString(mode));
                    }
                });
            }

            @Override
            public void connect(boolean isConnect) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        if(isConnect)
                        {
                            tv_connect.setText("Connected");
                            tv_connect.setTextColor(Color.BLUE);
                        }
                        else
                        {
                            tv_connect.setText("Disconnected");
                            tv_connect.setTextColor(Color.RED);
                        }

                    }
                });
            }

            @Override
            public void armed(boolean isArm) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        if(isArm)
                        {
                            tv_arm.setText("Arm");
                            tv_arm.setTextColor(Color.BLUE);
                        }
                        else
                        {
                            tv_arm.setText("Disarm");
                            tv_arm.setTextColor(Color.RED);
                        }

                    }
                });
            }
        });


        rv_left = (RockerView)findViewById(R.id.rv_left);
        rv_left.setMode(true,false,true,false,true,true);
        rv_left.setRockerChangeListener(new RockerView.RockerChangeListener() {
            @Override
            public void report(int x, int y) {
                // TODO Auto-generated method stub
                ch[2] = y;
                ch[3] = x;
                //Log.i("LEFT", String.valueOf(x) + " " + String.valueOf(y));
                tv_ch3.setText("THR CH3:"+String.valueOf(y));
                tv_ch4.setText("YAW CH4:"+String.valueOf(x));
            }
        });
        rv_right = (RockerView)findViewById(R.id.rv_right);
        rv_right.setRockerChangeListener(new RockerView.RockerChangeListener() {

            @Override
            public void report(int x, int y) {
                // TODO Auto-generated method stub
                ch[0] = x;
                ch[1] = y;
                //Log.i("RIGHT", String.valueOf(x) + " " + String.valueOf(y));
                tv_ch1.setText("ROL CH1:"+String.valueOf(x));
                tv_ch2.setText("PIT CH2:"+String.valueOf(y));
            }
        });


        AlertDialog alertDialogModeSelect = new AlertDialog.Builder(this)
                .setTitle("Select Mode")
                .setIcon(R.mipmap.ic_launcher)
                .setItems(Mavlink.MODE, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mavlink.setMode(Mavlink.MODE[i]);
                    }
                })
                .create();

        tv_mode.setOnClickListener(new View.OnClickListener() {

             @Override
             public void onClick(View v) {
                 // TODO Auto-generated method stub

                 alertDialogModeSelect.show();

             }
        });

        final String[] FLYMODE = {"MODE1", "MODE2"};

        AlertDialog alertDialogFlyModeSelect = new AlertDialog.Builder(this)
                .setTitle("Select Fly Mode")
                .setIcon(R.mipmap.ic_launcher)
                .setItems(FLYMODE, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        setFlyMode(i);
                    }
                })
                .create();

        bt_setting.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub

                alertDialogFlyModeSelect.show();

            }
        });


        tv_arm.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub


                mavlink.sendMsgDisarm(1);

            }
        });



        Timer mTimer = new Timer();
        TimerTask mTimerTask = new TimerTask() {
            @Override
            public void run() {
                mavlink.sendMsgRC(ch[0],ch[1],ch[2],ch[3]);
            }
        };
        mTimer.schedule(mTimerTask, 4,4);

//        Thread t1 = new Thread(() -> {
//            try {
//                while (true) {
//                    Thread.sleep(1000);
//                    rv_left.xyReport();
//                    rv_right.xyReport();
//                }
//            }catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        });
//        t1.start();

        initPhoneSensors();
        setFlyMode(flyMode);
    }

    public void setFlyMode(int mode)
    {

        flyMode = mode;
        if(flyMode != flyModelast)
        {
            flyModelast = flyMode;

            if(flyMode == 0)
            {
                mSensorManager.unregisterListener(this, mGyroSensor);
                mSensorManager.unregisterListener(this, mAccSensor);
                mSensorManager.unregisterListener(this, mMagSensor);
                rv_right.setXY(1500,1500);
                rv_right.xyReport();
                rv_right.touchEnable = true;
            }else{
                mSensorManager.registerListener(this, mGyroSensor, SensorManager.SENSOR_DELAY_UI);
                mSensorManager.registerListener(this, mAccSensor, SensorManager.SENSOR_DELAY_UI);
                mSensorManager.registerListener(this, mMagSensor, SensorManager.SENSOR_DELAY_UI);
                rv_right.touchEnable = false;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mSensorManager.unregisterListener(this, mGyroSensor);
        mSensorManager.unregisterListener(this, mAccSensor);
        mSensorManager.unregisterListener(this, mMagSensor);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
//                mAccXTv.setText(String.format(Locale.CHINA, "acc x : %f", event.values[0]));
//                mAccYTv.setText(String.format(Locale.CHINA, "acc y : %f", event.values[1]));
//                mAccZTv.setText(String.format(Locale.CHINA, "acc z : %f", event.values[2]));
                System.arraycopy(event.values, 0, mAccValues, 0, mAccValues.length);// 获取数据
                break;
            case Sensor.TYPE_GYROSCOPE:
//                mPhoneGyroXTv.setText(String.format(Locale.CHINA, "PhoneGyro x : %f", event.values[0]));
//                mPhoneGyroYTv.setText(String.format(Locale.CHINA, "PhoneGyro y : %f", event.values[1]));
//                mPhoneGyroZTv.setText(String.format(Locale.CHINA, "PhoneGyro z : %f", event.values[2]));
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                System.arraycopy(event.values, 0, mMagValues, 0, mMagValues.length);// 获取数据
                break;
        }
        SensorManager.getRotationMatrix(mRMatrix, null, mAccValues, mMagValues);
        SensorManager.getOrientation(mRMatrix, mPhoneAngleValues);// 此时获取到了手机的角度信息
//        mPhoneAzTv.setText(String.format(Locale.CHINA, "Azimuth(地平经度): %f", Math.toDegrees(mPhoneAngleValues[0])));
//        mPhonePitchTv.setText(String.format(Locale.CHINA, "Pitch: %f", Math.toDegrees(mPhoneAngleValues[1])));
//        mPhoneRollTv.setText(String.format(Locale.CHINA, "Roll: %f", Math.toDegrees(mPhoneAngleValues[2])));

        double rol = Math.toDegrees(-mPhoneAngleValues[1]);
        double pit = Math.toDegrees(mPhoneAngleValues[2]);



        //Log.i("SENSOR", String.valueOf(Math.toDegrees(mPhoneAngleValues[1])) + " " + String.valueOf(Math.toDegrees(mPhoneAngleValues[2])));

        ch[0] = RockerView.limit_rc((int)(1500 + 500 * rol / 30));
        ch[1] = RockerView.limit_rc((int)(1500 + 500 * pit / 30));
//        Log.i("RIGHT", String.valueOf(ch[0]) + " " + String.valueOf(ch[1]));
        tv_ch1.setText("ROL CH1:"+String.valueOf(ch[0]));
        tv_ch2.setText("PIT CH2:"+String.valueOf(ch[1]));
        rv_right.setXY(ch[0],ch[1]);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void initPhoneSensors() {
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        List<Sensor> sensorList = mSensorManager.getSensorList(Sensor.TYPE_ALL);
//        for (Sensor sensor : sensorList) {
//            Log.d(TAG, String.format(Locale.CHINA, "[Sensor] name: %s \tvendor:%s",
//                    sensor.getName(), sensor.getVendor()));
//        }
        // 获取传感器
        mGyroSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mAccSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

    }
}