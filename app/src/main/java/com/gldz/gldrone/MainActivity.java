package com.gldz.gldrone;

import static java.lang.Math.abs;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.MAVLink.common.msg_attitude;
import com.MAVLink.common.msg_servo_output_raw;
import com.MAVLink.common.msg_sys_status;

import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private final int[] ch = {1500, 1500, 1000, 1500, 1500, 1500};
    // 加速度传感器数据
    float[] mAccValues = new float[3];
    // 地磁传感器数据
    float[] mMagValues = new float[3];
    // 旋转矩阵，用来保存磁场和加速度的数据
    float[] mRMatrix = new float[9];
    // 存储方向传感器的数据（原始数据为弧度）
    float[] mPhoneAngleValues = new float[3];
    float[] mRoationVector = new float[4];
    Button bt_setting = null;
    //    Button bt_disarm = null;
    Button bt_arm = null;
    TextView tv_ch1 = null;
    TextView tv_ch2 = null;
    TextView tv_ch3 = null;
    TextView tv_ch4 = null;
    TextView tv_ch5 = null;
    TextView tv_ch6 = null;
    TextView tv_mode = null;
    TextView tv_connect = null;
    TextView tv_arm = null;
    TextView tv_bat = null;
    TextView tv_load = null;
    TextView tv_pwm1 = null;
    TextView tv_pwm2 = null;
    TextView tv_pwm3 = null;
    TextView tv_rol, tv_pit, tv_yaw;
    RockerView rv_left = null;
    RockerView rv_right = null;
    RockerView CompThrStick = null;
    Mavlink mavlink;
    int flyMode = 0, flyModelast = -1;
    private SensorManager mSensorManager;
    private Sensor mGyroSensor;
    private Sensor mAccSensor;
    private Sensor mMagSensor;
    private Sensor mOrientationSensor;
    private boolean isSupportVectorOri;

    float left_x = -1;
    float left_y = -1;
    float right_x = -1;
    float right_y = -1;
    Switch switch_stick;

    double pitchOffset = -45;

    double pitchSensitive = 1.0;
    double rollSensitive = 1.0;
    Button setPitchZero;

    EditText pitchSensEdit;
    EditText rollSensEdit;
    SharedPreferences sharedPreferences;
    EditText pitchPlusCompInput;
    EditText pitchMinusCompEdit;
    EditText steeringCompEdit;

    int pitchPlusComp = 0;
    int pitchMinusComp = 0;
    int steeringComp = 0;

    /**
     * 隐藏虚拟按键，并且全屏
     */
    public void hideBottomUIMenu() {
        //隐藏虚拟按键，并且全屏
        if (Build.VERSION.SDK_INT > 11 && Build.VERSION.SDK_INT < 19) { // lower api
            View v = this.getWindow().getDecorView();
            v.setSystemUiVisibility(View.GONE);
        } else if (Build.VERSION.SDK_INT >= 19) {
            //for new api versions.
            View decorView = getWindow().getDecorView();
            int uiOptions =
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                            | View.SYSTEM_UI_FLAG_IMMERSIVE;
            decorView.setSystemUiVisibility(uiOptions);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        }
    }

    /**
     * 显示虚拟按键
     */
    public void showBottomUIMenu() {
        if (Build.VERSION.SDK_INT > 11 && Build.VERSION.SDK_INT < 19) {
            //低版本sdk
            View v1 = getWindow().getDecorView();
            v1.setSystemUiVisibility(View.VISIBLE);
        } else if (Build.VERSION.SDK_INT >= 19) {
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        Window window = getWindow();
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        int flag = WindowManager.LayoutParams.FLAG_FULLSCREEN;
        window.setFlags(flag, flag);
        hideBottomUIMenu();

        setContentView(R.layout.activity_main);
        // KEEP_SCREEN_ON
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        tv_ch1 = findViewById(R.id.tv_ch1);
        tv_ch2 = findViewById(R.id.tv_ch2);
        tv_ch3 = findViewById(R.id.tv_ch3);
        tv_ch4 = findViewById(R.id.tv_ch4);
        tv_ch5 = findViewById(R.id.tv_ch5);
//        tv_ch6 = (TextView) findViewById(R.id.tv_ch6);
        tv_mode = findViewById(R.id.tv_mode);
        tv_connect = findViewById(R.id.tv_connect);
        tv_arm = findViewById(R.id.tv_arm);
        tv_bat = findViewById(R.id.tv_bat);
        tv_load = findViewById(R.id.tv_load);

        tv_rol = findViewById(R.id.tv_rol);
        tv_pit = findViewById(R.id.tv_pit);
        tv_yaw = findViewById(R.id.tv_yaw);

        switch_stick = findViewById(R.id.stick_switch);
        rv_left = findViewById(R.id.rv_left);
        rv_right = findViewById(R.id.rv_right);
        CompThrStick = findViewById(R.id.CompThrStick);

        left_x = rv_left.getX();
        left_y = rv_left.getY();
        right_x = rv_right.getX();
        right_y = rv_right.getY();
        setPitchZero = findViewById(R.id.setPitchZero);

        pitchSensEdit = findViewById(R.id.edit_PS);
        rollSensEdit = findViewById(R.id.edit_RS);
        pitchPlusCompInput = findViewById(R.id.PitchPlusCompensate);
        pitchMinusCompEdit = findViewById(R.id.PitchMinusCompensateInput);

        steeringCompEdit = findViewById(R.id.STRInput);

        switch_stick.setOnCheckedChangeListener((buttonView, checked) -> {
            RelativeLayout.LayoutParams lpLeft =
                    (RelativeLayout.LayoutParams) rv_left.getLayoutParams();
            RelativeLayout.LayoutParams lpRight =
                    (RelativeLayout.LayoutParams) rv_right.getLayoutParams();

            // 必须 new，不能直接互换引用
            RelativeLayout.LayoutParams newLeftLp =
                    new RelativeLayout.LayoutParams(lpRight);
            RelativeLayout.LayoutParams newRightLp =
                    new RelativeLayout.LayoutParams(lpLeft);

            rv_left.setLayoutParams(newLeftLp);
            rv_right.setLayoutParams(newRightLp);
        });

        setPitchZero.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pitchOffset = -1000000;
            }
        });

        // 读取灵敏度与零点配置
        sharedPreferences = getSharedPreferences("config", MODE_PRIVATE);
//        //获取Editor对象的引用
//        SharedPreferences.Editor editor = sharedPreferences.edit();
//        //将获取过来的值放入文件
//        editor.putString("name", "lucas");
//        editor.putInt("age", 30);
//        editor.putBoolean("islogin",true);
//        // 提交数据
//        editor.commit();
        pitchOffset = sharedPreferences.getFloat("pitchOffset", -45);
        pitchSensitive = sharedPreferences.getFloat("pitchSensitive", 1);
        rollSensitive = sharedPreferences.getFloat("rollSensitive", 1);

        pitchPlusComp = sharedPreferences.getInt("pitchPlusComp", 0);
        pitchMinusComp = sharedPreferences.getInt("pitchMinusComp", 0);
        steeringComp = sharedPreferences.getInt("steeringComp", 0);

        steeringCompEdit.setText(String.valueOf(steeringComp));
        pitchPlusCompInput.setText(String.valueOf(pitchPlusComp));
        pitchMinusCompEdit.setText(String.valueOf(pitchMinusComp));

        rollSensEdit.setText(String.format(Locale.US, "%.2f", rollSensitive));
        pitchSensEdit.setText(String.format(Locale.US, "%.2f", pitchSensitive));

        rollSensEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_DONE ||
                        i == EditorInfo.IME_ACTION_GO ||
                        i == EditorInfo.IME_ACTION_SEND ||
                        i == EditorInfo.IME_ACTION_NEXT
                ) {

                    rollSensitive = Float.valueOf(textView.getText().toString());
                    rollSensEdit.setText(String.format(Locale.US, "%.2f", rollSensitive));
                    rollSensitive = Float.valueOf(textView.getText().toString());
                    //获取Editor对象的引用
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    //将获取过来的值放入文件
                    editor.putFloat("rollSensitive", (float) rollSensitive);
                    // 提交数据
                    editor.commit();
                    textView.clearFocus();   // 主动失焦（可选但推荐）
//                    return true;      // 消费事件
                }
                return false;
            }
        });

        pitchSensEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_DONE ||
                        i == EditorInfo.IME_ACTION_GO ||
                        i == EditorInfo.IME_ACTION_SEND ||
                        i == EditorInfo.IME_ACTION_NEXT
                ) {
                    pitchSensitive = Float.valueOf(textView.getText().toString());
                    pitchSensEdit.setText(String.format(Locale.US, "%.2f", pitchSensitive));
                    pitchSensitive = Float.valueOf(textView.getText().toString());
                    //获取Editor对象的引用
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    //将获取过来的值放入文件
                    editor.putFloat("pitchSensitive", (float) pitchSensitive);
                    // 提交数据
                    editor.commit();
                    textView.clearFocus();   // 主动失焦（可选但推荐）
//                    return true;      // 消费事件
                }
                return false;
            }
        });

        steeringCompEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_DONE ||
                        i == EditorInfo.IME_ACTION_GO ||
                        i == EditorInfo.IME_ACTION_SEND ||
                        i == EditorInfo.IME_ACTION_NEXT
                ) {
                    steeringComp = Integer.valueOf(textView.getText().toString());
                    //获取Editor对象的引用
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    //将获取过来的值放入文件
                    editor.putInt("steeringComp", steeringComp);
                    // 提交数据
                    editor.commit();
                    textView.clearFocus();   // 主动失焦（可选但推荐）
//                    return true;      // 消费事件
                }
                return false;
            }
        });

        pitchPlusCompInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_DONE ||
                        i == EditorInfo.IME_ACTION_GO ||
                        i == EditorInfo.IME_ACTION_SEND ||
                        i == EditorInfo.IME_ACTION_NEXT
                ) {
                    pitchPlusComp = Integer.valueOf(textView.getText().toString());
                    //获取Editor对象的引用
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    //将获取过来的值放入文件
                    editor.putInt("pitchPlusComp", pitchPlusComp);
                    // 提交数据
                    editor.commit();
                    textView.clearFocus();   // 主动失焦（可选但推荐）
//                    return true;      // 消费事件
                }
                return false;
            }
        });

        pitchMinusCompEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_DONE ||
                        i == EditorInfo.IME_ACTION_GO ||
                        i == EditorInfo.IME_ACTION_SEND ||
                        i == EditorInfo.IME_ACTION_NEXT
                ) {
                    pitchMinusComp = Integer.valueOf(textView.getText().toString());
                    //获取Editor对象的引用
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    //将获取过来的值放入文件
                    editor.putInt("pitchMinusComp", pitchMinusComp);
                    // 提交数据
                    editor.commit();
                    textView.clearFocus();   // 主动失焦（可选但推荐）
//                    return true;      // 消费事件
                }
                return false;
            }
        });

        bt_setting = findViewById(R.id.bt_setting);
//        bt_disarm = findViewById(R.id.bt_disarm);
        bt_arm = findViewById(R.id.bt_arm);

        mavlink = new Mavlink();
        if (!mavlink.socketInitSuccess) {
            Toast.makeText(getApplicationContext(), "Socket Creat Fail. Check WIFI and Try.",
                    Toast.LENGTH_LONG).show();
        }

        mavlink.setMavlinkListener(new Mavlink.MavlinkListener() {
            @Override
            public void mode(Mavlink.PX4_MODE mode) {

//                runOnUiThread(new Runnable() {
//                    public void run() {
//                        tv_mode.setText(mavlink.px4ModeString(mode));
//                    }
//                });
            }

            @Override
            public void customMode(long mode) {

                runOnUiThread(new Runnable() {
                    public void run() {
                        if (mode < 0) {
                            Log.e("modeHeartbeat", "mode id is < 0");
                            return;
                        }
                        if (mavlink.modeList.size() != mavlink.modeId.size()) {
                            Log.e("modeHeartbeat", "mode id is not equal to mode name");
                            return;
                        }
                        for (int i = 0; i < mavlink.modeId.size(); i++) {
                            if (mode == mavlink.modeId.get(i)) {
                                tv_mode.setText(mavlink.modeList.get(i));
                                return;
                            }
                        }
                        tv_mode.setText("UNKNOW");
                    }
                });
            }

            @Override
            public void connect(boolean isConnect) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        if (isConnect) {
                            // request All Modes
//                            mavlink.fullModeCount = -1;
//                            mavlink.modeList.clear();
//                            mavlink.modeId.clear();
//                            mavlink.requestCustomModeList();

                            tv_connect.setText("Connected");
                            tv_connect.setTextColor(Color.BLUE);
                        } else {
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
                        if (isArm) {
                            tv_arm.setText("Arm");
                            tv_arm.setTextColor(Color.BLUE);
                            bt_arm.setText("DISARM");
                            setPitchZero.setEnabled(false);
                            pitchSensEdit.setEnabled(false);
                            rollSensEdit.setEnabled(false);
                            pitchPlusCompInput.setEnabled(false);
                            pitchMinusCompEdit.setEnabled(false);
                            steeringCompEdit.setEnabled(false);
                        } else {
                            tv_arm.setText("Disarm");
                            tv_arm.setTextColor(Color.RED);
                            bt_arm.setText("ARM");
                            if (flyMode == 1) {
                                setPitchZero.setEnabled(true);
                                pitchSensEdit.setEnabled(true);
                                rollSensEdit.setEnabled(true);
                            }
                            pitchPlusCompInput.setEnabled(true);
                            pitchMinusCompEdit.setEnabled(true);
                            steeringCompEdit.setEnabled(true);
                        }

                    }
                });
            }

            @Override
            public void sys_status(msg_sys_status sys_status) {
                runOnUiThread(new Runnable() {
                    public void run() {

                        @SuppressLint("DefaultLocale") String bat =
                                String.format("%.2f", (float) sys_status.voltage_battery / 1000) +
                                        "v   ";
                        @SuppressLint("DefaultLocale") String load =
                                String.format("%.0f", (float) sys_status.load / 10) + "%";
                        tv_bat.setText(bat);
                        tv_load.setText(load);
                        if (sys_status.voltage_battery >= 3700) {
                            tv_bat.setTextColor(0xff00ff00);
                        } else if (sys_status.voltage_battery >= 3500) {
                            tv_bat.setTextColor(0xffFFFF00);
                        } else {
                            tv_bat.setTextColor(0xffFF0000);
                        }

                    }
                });

            }

            @Override
            public void servo_output_raw(msg_servo_output_raw servo_output_raw) {
//                runOnUiThread(new Runnable() {
//                    public void run() {
//
//                        tv_pwm1.setText("PWM1:" + String.valueOf(servo_output_raw.servo1_raw));
//                        tv_pwm2.setText("PWM2:" + String.valueOf(servo_output_raw.servo2_raw));
//                        tv_pwm3.setText("PWM3:" + String.valueOf(servo_output_raw.servo3_raw));
//                        tv_pwm4.setText("PWM4:" + String.valueOf(servo_output_raw.servo4_raw));
//                    }
//                });
            }

            @Override
            public void attitude(msg_attitude attitude) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        String roll = String.format("%.2f", attitude.roll * 180 / 3.1415926);
                        String pitch = String.format("%.2f", attitude.pitch * 180 / 3.1415926);
                        String yaw = String.format("%.2f", attitude.yaw * 180 / 3.1415926);

                        tv_rol.setText("ROL:" + roll);
                        tv_pit.setText("PIT:" + pitch);
                        tv_yaw.setText("YAW:" + yaw);

                    }
                });
            }
        });


        rv_left.setMode(false, false, false, false, true, true);
        rv_left.setRockerChangeListener(new RockerView.RockerChangeListener() {
            @Override
            public void report(int x, int y) {
                // TODO Auto-generated method stub
//                ch[2] = y;
                ch[3] = x;
                //Log.i("LEFT", String.valueOf(x) + " " + String.valueOf(y));
                tv_ch4.setText("YAW CH4:" + x);
                // 计算转向比例
                int new_Thr = 0;
                if (rv_right.getXY()[1] < 1500) {
                    new_Thr =
                            rv_left.getXY()[1] + (1500 - rv_right.getXY()[1]) * pitchPlusComp / 100
                                    + (abs(rv_right.getXY()[0] - 1500) * steeringComp / 100);
                }else{
                    new_Thr =
                            rv_left.getXY()[1] + (1500 - rv_right.getXY()[1]) * pitchMinusComp / 100
                                    + (abs(rv_right.getXY()[0] - 1500) * steeringComp / 100);
                }
                new_Thr = RockerView.limit_rc(new_Thr);
                if (rv_left.getXY()[1] < 1200){
                    new_Thr = 1000;
                }
                CompThrStick.setXY(1500,new_Thr);
                ch[2] = new_Thr;
                tv_ch3.setText("THR CH3:" + new_Thr);
            }
        });

        CompThrStick.setMode(true, false, false, true, true, true);

        rv_right.setRockerChangeListener(new RockerView.RockerChangeListener() {

            @Override
            public void report(int x, int y) {
                // TODO Auto-generated method stub
                ch[0] = x;
                ch[1] = y;
                tv_ch1.setText("ROL CH1:" + x);
                tv_ch2.setText("PIT CH2:" + y);
                // 计算转向比例
                int new_Thr = 0;
                if (rv_right.getXY()[1] < 1500) {
                    new_Thr =
                            rv_left.getXY()[1] + (1500 - rv_right.getXY()[1]) * pitchPlusComp / 100
                                    + (abs(rv_right.getXY()[0] - 1500) * steeringComp / 100);
                }else{
                    new_Thr =
                            rv_left.getXY()[1] + (1500 - rv_right.getXY()[1]) * pitchMinusComp / 100
                                    + (abs(rv_right.getXY()[0] - 1500) * steeringComp / 100);
                }
                new_Thr = RockerView.limit_rc(new_Thr);
                if (rv_left.getXY()[1] < 1200){
                    new_Thr = 1000;
                }
                CompThrStick.setXY(1500,new_Thr);
                ch[2] = new_Thr;
                tv_ch3.setText("THR CH3:" + new_Thr);
            }
        });


        final AlertDialog[] alertDialogModeSelect = {new AlertDialog.Builder(this)
                .setTitle("Select Mode").setIcon(R.mipmap.ic_launcher)
                .setItems(mavlink.modeList.toArray(new CharSequence[0]),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                mavlink.sendMsgSetCustomMode(mavlink.modeId.get(i));
                            }
                        }).create()};

        tv_mode.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                if (mavlink.modeId.isEmpty()) {
                    mavlink.requestCustomModeList();
                    Toast.makeText(getApplicationContext(), "request modes", Toast.LENGTH_SHORT)
                            .show();
                }
                alertDialogModeSelect[0] =
                        new AlertDialog.Builder(alertDialogModeSelect[0].getContext()).setTitle(
                                        "Select Mode").setIcon(R.mipmap.ic_launcher)
                                .setItems(mavlink.modeList.toArray(new CharSequence[0]),
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface,
                                                                int i) {
                                                mavlink.sendMsgSetCustomMode(mavlink.modeId.get(i));
                                            }
                                        }).create();
                alertDialogModeSelect[0].show();

            }
        });

        final String[] FLYMODE = {"触摸", "体感"};

        AlertDialog alertDialogFlyModeSelect =
                new AlertDialog.Builder(this).setTitle("Select Fly Mode")
                        .setIcon(R.mipmap.ic_launcher)
                        .setItems(FLYMODE, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                setFlyMode(i);
                            }
                        }).create();

        bt_setting.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub

                alertDialogFlyModeSelect.show();

            }
        });


        bt_arm.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub

                if (mavlink.is_armed) {
                    mavlink.sendMsgDisarm(0, 21196);
                } else {
                    mavlink.sendMsgDisarm(1, 0);
                }

            }
        });

        bt_arm.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (mavlink.is_armed) {
                    mavlink.sendMsgDisarm(0, 21196);
                } else {
                    mavlink.sendMsgDisarm(1, 21196);
                }
                return true;
            }
        });

//        bt_disarm.setOnClickListener(new View.OnClickListener() {
//
//            @Override
//            public void onClick(View v) {
//                // TODO Auto-generated method stub
//
//                Toast.makeText(getApplicationContext(), "FORCE DISARM", Toast.LENGTH_SHORT).show();
//                mavlink.sendMsgDisarm(0, 21196);
//
//            }
//        });

        Timer mTimer = new Timer();
        TimerTask mTimerTask = new TimerTask() {
            @Override
            public void run() {
//                mavlink.sendMsgRC(ch[0], ch[1], ch[2], ch[3], ch[4], ch[5]);
                mavlink.sendMsgManualControl(ch[2] - 1000, ch[3] * 2 - 3000,
                        ch[1] * 2 - 3000, ch[0] * 2 - 3000);
            }
        };
        mTimer.schedule(mTimerTask, 20, 20);

        initPhoneSensors();
        setFlyMode(flyMode);
    }

    public void setFlyMode(int mode) {

        flyMode = mode;
        if (flyMode != flyModelast) {
            flyModelast = flyMode;

            try {
                mSensorManager.unregisterListener(this, mGyroSensor);
                mSensorManager.unregisterListener(this, mAccSensor);
                mSensorManager.unregisterListener(this, mMagSensor);
            } catch (Throwable e) {

            }

            try {
                mSensorManager.unregisterListener(this, mOrientationSensor);
            } catch (Throwable e) {

            }

            if (flyMode == 0) {  // 触摸
                setPitchZero.setEnabled(false);
                pitchSensEdit.setEnabled(false);
                rollSensEdit.setEnabled(false);
                rv_right.setXY(1500, 1500);
                rv_right.xyReport();
                rv_right.touchEnable = true;
            } else if (flyMode == 1) {
                if (!mavlink.is_armed) {
                    setPitchZero.setEnabled(true);
                    pitchSensEdit.setEnabled(true);
                    rollSensEdit.setEnabled(true);
                }
                if (isSupportVectorOri) {
                    mSensorManager.registerListener(this, mOrientationSensor,
                            SensorManager.SENSOR_DELAY_FASTEST);
                } else {
                    mSensorManager.registerListener(this, mGyroSensor,
                            SensorManager.SENSOR_DELAY_FASTEST);
                    mSensorManager.registerListener(this, mAccSensor,
                            SensorManager.SENSOR_DELAY_FASTEST);
                    mSensorManager.registerListener(this, mMagSensor,
                            SensorManager.SENSOR_DELAY_FASTEST);
                }
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
            case Sensor.TYPE_ROTATION_VECTOR:
                System.arraycopy(event.values, 0, mRoationVector, 0, mRoationVector.length);
                break;
        }
        if (isSupportVectorOri) {
            SensorManager.getRotationMatrixFromVector(mRMatrix, mRoationVector);
        } else {
            SensorManager.getRotationMatrix(mRMatrix, null, mAccValues, mMagValues);
        }
        SensorManager.getOrientation(mRMatrix, mPhoneAngleValues);// 此时获取到了手机的角度信息
//        mPhoneAzTv.setText(String.format(Locale.CHINA, "Azimuth(地平经度): %f", Math.toDegrees(mPhoneAngleValues[0])));
//        mPhonePitchTv.setText(String.format(Locale.CHINA, "Pitch: %f", Math.toDegrees(mPhoneAngleValues[1])));
//        mPhoneRollTv.setText(String.format(Locale.CHINA, "Roll: %f", Math.toDegrees(mPhoneAngleValues[2])));

        if (abs(pitchOffset - (-1000000)) < 0.001f) {
            pitchOffset = Math.toDegrees(mPhoneAngleValues[2]);
            //获取Editor对象的引用
            SharedPreferences.Editor editor = sharedPreferences.edit();
            //将获取过来的值放入文件
            editor.putFloat("pitchOffset", (float) pitchOffset);
            // 提交数据
            editor.commit();
        }
        double rol = Math.toDegrees(-mPhoneAngleValues[1]) * rollSensitive;
        double pit = (Math.toDegrees(mPhoneAngleValues[2]) - pitchOffset) * pitchSensitive;


        //Log.i("SENSOR", String.valueOf(Math.toDegrees(mPhoneAngleValues[1])) + " " + String.valueOf(Math.toDegrees(mPhoneAngleValues[2])));

        ch[0] = RockerView.limit_rc((int) (1500 + 500 * rol / 30));
        ch[1] = RockerView.limit_rc((int) (1500 + 500 * pit / 30));
//        Log.i("RIGHT", String.valueOf(ch[0]) + " " + String.valueOf(ch[1]));
        tv_ch1.setText("ROL CH1:" + ch[0]);
        tv_ch2.setText("PIT CH2:" + ch[1]);
        rv_right.setXY(ch[0], ch[1]);
        // 计算转向比例
        int new_Thr = 0;
        if (rv_right.getXY()[1] < 1500) {
            new_Thr =
                    rv_left.getXY()[1] + (1500 - rv_right.getXY()[1]) * pitchPlusComp / 100
                            + (abs(rv_right.getXY()[0] - 1500) * steeringComp / 100);
        }else{
            new_Thr =
                    rv_left.getXY()[1] + (1500 - rv_right.getXY()[1]) * pitchMinusComp / 100
                            + (abs(rv_right.getXY()[0] - 1500) * steeringComp / 100);
        }
        new_Thr = RockerView.limit_rc(new_Thr);
        if (rv_left.getXY()[1] < 1200){
            new_Thr = 1000;
        }
        CompThrStick.setXY(1500,new_Thr);
        ch[2] = new_Thr;
        tv_ch3.setText("THR CH3:" + new_Thr);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void initPhoneSensors() {
        isSupportVectorOri = false;
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        List<Sensor> sensorList = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        for (Sensor sensor : sensorList) {
//            Log.d(TAG, String.format(Locale.CHINA, "[Sensor] name: %s \tvendor:%s",
//                    sensor.getName(), sensor.getVendor()));
            if (sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
                Toast.makeText(this, "手机支持融合角度", Toast.LENGTH_SHORT).show();
                isSupportVectorOri = true;
            }
        }
        if (!isSupportVectorOri) {
            Toast.makeText(this, "手机不支持融合角度，退化为加速度计", Toast.LENGTH_SHORT).show();
        }
        // 获取传感器
        mGyroSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mAccSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (isSupportVectorOri) {
            mOrientationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        }

    }
}