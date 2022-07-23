package com.gldz.gldrone;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
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


public class MainActivity extends AppCompatActivity {


    Mavlink mavlink;
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

        TextView tv_ch1 = (TextView)findViewById(R.id.tv_ch1);
        TextView tv_ch2 = (TextView)findViewById(R.id.tv_ch2);
        TextView tv_ch3 = (TextView)findViewById(R.id.tv_ch3);
        TextView tv_ch4 = (TextView)findViewById(R.id.tv_ch4);
        TextView tv_mode = (TextView)findViewById(R.id.tv_mode);
        TextView tv_connect = (TextView)findViewById(R.id.tv_connect);
        TextView tv_arm = (TextView)findViewById(R.id.tv_arm);

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
                            tv_connect.setTextColor(Color.GREEN);
                        }
                        else
                        {
                            tv_connect.setText("Disconnected");
                            tv_connect.setTextColor(Color.RED);
                        }

                    }
                });
            }
        });


        RockerView rv_left = (RockerView)findViewById(R.id.rv_left);
        rv_left.setMode(true,false,true,false,true,true);
        rv_left.setRockerChangeListener(new RockerView.RockerChangeListener() {
            @Override
            public void report(int x, int y) {
                // TODO Auto-generated method stub
                //Log.i("LEFT", String.valueOf(x) + " " + String.valueOf(y));
                tv_ch3.setText("CH3:"+String.valueOf(y));
                tv_ch4.setText("CH4:"+String.valueOf(x));
//                transsion.setCh3((int)(1540+ y*500.0));
//                transsion.setCh4((int)(1500+ x*200.0));
            }
        });
        RockerView rv_right = (RockerView)findViewById(R.id.rv_right);
        rv_right.setRockerChangeListener(new RockerView.RockerChangeListener() {

            @Override
            public void report(int x, int y) {
                // TODO Auto-generated method stub
                Log.i("RIGHT", String.valueOf(x) + " " + String.valueOf(y));
                tv_ch1.setText("CH1:"+String.valueOf(x));
                tv_ch2.setText("CH2:"+String.valueOf(y));
//                transsion.setCh1((int)(1500+x*200.0));
//                transsion.setCh2((int)(1500-y*200.0));
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
    }


}