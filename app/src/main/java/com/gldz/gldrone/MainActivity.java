package com.gldz.gldrone;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import java.lang.ref.WeakReference;

import android.view.WindowManager;

public class MainActivity extends AppCompatActivity {

    // Handler
    static MyHandler mHandler;

    // MAVLinkConnection
    MAVLinkConnection MAVLinkConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // KEEP_SCREEN_ON
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mHandler = new MyHandler(this);
        // MAVLinkConnection
        MAVLinkConnection = new MAVLinkConnection(mHandler);


        Thread t = new Thread(() -> {
            MAVLinkConnection.Create_Connection();
        });
        t.start(); // 启动新线程
    }

    // This handler will handle TextView UI
    private static class MyHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;
        public MyHandler(MainActivity activity) {
            mActivity = new WeakReference<>(activity);
        }
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {

                // USB or TCP connection status (refer to MAVLinkConnection.java)
                case 100:
                    String connection_PIXHAWK = (String) msg.obj;
                    //mActivity.get().display_PIXHAWK.setText(connection_PIXHAWK);
                    break;
                // FlightMode
                case 200:
                    String FlightMode = (String) msg.obj;
                    //mActivity.get().display_FlightMode.setText(FlightMode);
                    break;
                // isArmed
                case 201:
                    String isArmed = (String) msg.obj;
                    //mActivity.get().display_isArmed.setText(isArmed);
                    break;
                // Location
                case 202:
                    String Location = (String) msg.obj;
                    //mActivity.get().display_Location.setText(Location);
                    break;
                // Speed
                case 203:
                    String Speed = (String) msg.obj;
                    //mActivity.get().display_Speed.setText(Speed);
                    break;
                // Battery
                case 204:
                    String Battery = (String) msg.obj;
                    //mActivity.get().display_Battery.setText(Battery);
                    break;
                // CMD
                case 300:
                    String web_cmd = (String) msg.obj;
                    //mActivity.get().display_drone_command.append("\n" + web_cmd);
                    break;
                // CMD_ACK
                case 301:
                    String cmd_ack = (String) msg.obj;
                    //mActivity.get().display_drone_command_ack.append("\n" + cmd_ack);
                    break;
//                // Sensor Data (refer to UsbService.java)
//                case UsbService.SYNC_READ:
//                    String sensor_data = (String) msg.obj;
//                    break;
            }
        }
    }
}