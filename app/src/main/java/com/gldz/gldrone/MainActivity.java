package com.gldz.gldrone;


import static com.MAVLink.minimal.msg_heartbeat.MAVLINK_MSG_ID_HEARTBEAT;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.concurrent.TimeUnit;

import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import com.MAVLink.MAVLinkPacket;
import com.MAVLink.Parser;
import com.MAVLink.common.msg_command_long;
import com.MAVLink.enums.MAV_AUTOPILOT;
import com.MAVLink.enums.MAV_CMD;
import com.MAVLink.enums.MAV_MODE_FLAG;
import com.MAVLink.enums.MAV_TYPE;
import com.MAVLink.minimal.msg_heartbeat;

import io.dronefleet.mavlink.common.CommandLong;

import io.dronefleet.mavlink.common.MavCmd;

import io.mavsdk.mavsdkserver.MavsdkServer;


public class MainActivity extends AppCompatActivity {
    DatagramSocket sendSocket;
    MavsdkServer server = new MavsdkServer();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // KEEP_SCREEN_ON
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        Thread t1 = new Thread(() -> {
            try
            {
                Parser parser = new Parser();
                byte[] receiveData = new byte[512];
                DatagramSocket recvSocket = new DatagramSocket(14550);
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                while(true)
                {
                    recvSocket.receive(receivePacket);// 在接收到信息之前，一直保持阻塞状态
                    for(int i=0;i<receivePacket.getLength();i++)
                    {

                        MAVLinkPacket msg = parser.mavlink_parse_char(receiveData[i]);
                        if(msg != null)
                        {
                            //Log.i("MAVLINK", String.valueOf(msg.msgid));
                            if(msg.msgid == MAVLINK_MSG_ID_HEARTBEAT)
                            {

                                msg_heartbeat heart = new msg_heartbeat();
                                heart.unpack(msg.payload);

                                Log.i("MAVLINK", heart.toString());
                            }
                        }
                    }
                }
            }catch(IOException e) {
                e.printStackTrace();
            }
        });
        t1.start(); // 启动新线程

        try {
            sendSocket = new DatagramSocket(14556);
        } catch (SocketException e) {
            e.printStackTrace();
        }

        Thread t2 = new Thread(() -> {
            try
            {


                while(true)
                {

                    msg_heartbeat heart = new msg_heartbeat();
                    heart.type = MAV_TYPE.MAV_TYPE_GCS;
                    heart.autopilot = MAV_AUTOPILOT.MAV_AUTOPILOT_GENERIC;


                    MAVLinkPacket msg = heart.pack();
                    msg.isMavlink2 = false;
                    msg.sysid = 0;
                    msg.compid = 0;
                    byte[] byteMsg = msg.encodePacket();
                    DatagramPacket sendPacket = new DatagramPacket(byteMsg, byteMsg.length, InetAddress.getByName("10.0.0.1"),14556);
                    sendSocket.send(sendPacket);
                    Log.i("MAVLINK", "send "+ String.valueOf(sendPacket.getLength()));
                    Thread.sleep(1000);
                }
            }catch(IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        //t2.start(); // 启动新线程


        Button btTest1 = (Button)findViewById(R.id.bt_send1);
        btTest1.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub

                Thread t = new Thread(() -> {
                    try
                    {

                            msg_command_long cmd = new msg_command_long();
                            cmd.command = MAV_CMD.MAV_CMD_DO_SET_MODE;
                            cmd.param1 = 0;
                            cmd.param2 = 209;
                            cmd.param3 = 7;


                            MAVLinkPacket msg = cmd.pack();
                            msg.isMavlink2 = false;
                            msg.sysid = 0;
                            msg.compid = 0;
                            byte[] byteMsg = msg.encodePacket();


                            DatagramPacket sendPacket = new DatagramPacket(byteMsg, byteMsg.length, InetAddress.getByName("10.0.0.1"),14556);
                            sendSocket.send(sendPacket);
                            Log.i("MAVLINK", "send "+ String.valueOf(sendPacket.getLength()));

                    }catch(IOException e) {
                        e.printStackTrace();
                    }
                });
                t.start(); // 启动新线程
            }
        });

        Button btTest2 = (Button)findViewById(R.id.bt_send2);
        btTest2.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                Thread t = new Thread(() -> {
                    try
                    {

                        msg_command_long cmd = new msg_command_long();
                        cmd.command = MAV_CMD.MAV_CMD_NAV_TAKEOFF;
                        cmd.param7 = 1;


                        MAVLinkPacket msg = cmd.pack();
                        msg.isMavlink2 = false;
                        msg.sysid = 0;
                        msg.compid = 0;
                        byte[] byteMsg = msg.encodePacket();


                        DatagramPacket sendPacket = new DatagramPacket(byteMsg, byteMsg.length, InetAddress.getByName("10.0.0.1"),14556);
                        sendSocket.send(sendPacket);
                        Log.i("MAVLINK", "send "+ String.valueOf(sendPacket.getLength()));

                    }catch(IOException e) {
                        e.printStackTrace();
                    }
                });
                t.start(); // 启动新线程
            }
        });
    }

}