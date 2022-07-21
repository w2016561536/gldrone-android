package com.gldz.gldrone;


import static com.MAVLink.common.msg_altitude.MAVLINK_MSG_ID_ALTITUDE;
import static com.MAVLink.common.msg_attitude.MAVLINK_MSG_ID_ATTITUDE;
import static com.MAVLink.common.msg_battery_status.MAVLINK_MSG_ID_BATTERY_STATUS;
import static com.MAVLink.common.msg_command_ack.MAVLINK_MSG_ID_COMMAND_ACK;
import static com.MAVLink.common.msg_extended_sys_state.MAVLINK_MSG_ID_EXTENDED_SYS_STATE;
import static com.MAVLink.common.msg_mission_request.MAVLINK_MSG_ID_MISSION_REQUEST;
import static com.MAVLink.common.msg_serial_control.MAVLINK_MSG_ID_SERIAL_CONTROL;
import static com.MAVLink.common.msg_servo_output_raw.MAVLINK_MSG_ID_SERVO_OUTPUT_RAW;
import static com.MAVLink.common.msg_sys_status.MAVLINK_MSG_ID_SYS_STATUS;
import static com.MAVLink.common.msg_vfr_hud.MAVLINK_MSG_ID_VFR_HUD;
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
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.concurrent.TimeUnit;

import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import com.MAVLink.MAVLinkPacket;
import com.MAVLink.Parser;
import com.MAVLink.common.msg_command_ack;
import com.MAVLink.common.msg_command_long;
import com.MAVLink.enums.MAV_AUTOPILOT;
import com.MAVLink.enums.MAV_CMD;
import com.MAVLink.enums.MAV_COMPONENT;
import com.MAVLink.enums.MAV_MODE;
import com.MAVLink.enums.MAV_MODE_FLAG;
import com.MAVLink.enums.MAV_STATE;
import com.MAVLink.enums.MAV_TYPE;
import com.MAVLink.minimal.msg_heartbeat;

import io.dronefleet.mavlink.common.CommandLong;

import io.dronefleet.mavlink.common.MavCmd;

import io.mavsdk.mavsdkserver.MavsdkServer;


public class MainActivity extends AppCompatActivity {

    final static int M_SYS_ID = 245;
    final static int M_COMP_ID = MAV_COMPONENT.MAV_COMP_ID_MISSIONPLANNER;

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
                byte[] receiveData = new byte[4096];
                DatagramSocket recvSocket = new DatagramSocket(new InetSocketAddress(14550));
//                recvSocket.setReuseAddress(true);
//                recvSocket.setSoTimeout(10000);
//                recvSocket.bind(new InetSocketAddress(14550));
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                while(true)
                {

                    recvSocket.receive(receivePacket);// 在接收到信息之前，一直保持阻塞状态
                    Log.i("MAVLINK", String.valueOf(receivePacket.getLength()));
//                    for(int i=0;i<receivePacket.getLength();i++)
//                    {
//
//                        MAVLinkPacket msg = parser.mavlink_parse_char(receiveData[i]);
//                        if(msg != null)
//                        {
//                            switch(msg.msgid){
//                                case MAVLINK_MSG_ID_HEARTBEAT:
//                                    msg_heartbeat heart = new msg_heartbeat();
//                                    heart.unpack(msg.payload);
//
//                                    Log.i("MAVLINK", heart.toString());
//                                    break;
//                                case MAVLINK_MSG_ID_SERIAL_CONTROL:
//                                    //Log.i("MAVLINK", "MAVLINK_MSG_ID_SERIAL_CONTROL");
//                                    break;
//
//                                case MAVLINK_MSG_ID_ALTITUDE:
//                                    //Log.i("MAVLINK", "MAVLINK_MSG_ID_ALTITUDE");
//                                    break;
//                                case MAVLINK_MSG_ID_ATTITUDE:
//                                    //Log.i("MAVLINK", "MAVLINK_MSG_ID_ATTITUDE");
//                                    break;
//                                case MAVLINK_MSG_ID_SERVO_OUTPUT_RAW:
//                                    //Log.i("MAVLINK", "MAVLINK_MSG_ID_SERVO_OUTPUT_RAW");
//                                    break;
//                                case MAVLINK_MSG_ID_SYS_STATUS:
//                                    //Log.i("MAVLINK", "MAVLINK_MSG_ID_SYS_STATUS");
//                                    break;
//                                case MAVLINK_MSG_ID_VFR_HUD:
//                                    //Log.i("MAVLINK", "MAVLINK_MSG_ID_VFR_HUD");
//                                    break;
//                                case MAVLINK_MSG_ID_BATTERY_STATUS:
//                                    //Log.i("MAVLINK", "MAVLINK_MSG_ID_BATTERY_STATUS");
//                                    break;
//                                case MAVLINK_MSG_ID_MISSION_REQUEST:
//                                    //Log.i("MAVLINK", "MAVLINK_MSG_ID_MISSION_REQUEST");
//                                    break;
//                                case MAVLINK_MSG_ID_COMMAND_ACK:
//                                    msg_command_ack pmsg = new msg_command_ack();
//                                    pmsg.unpack(msg.payload);
////                                    if(pmsg.result == MAV_RESULT.
//
//                                    Log.i("MAVLINK", pmsg.toString());
//                                    break;
//
//                                default:
//                                    //Log.e("MAVLINK", String.valueOf(msg.msgid));
//                                    break;
//                            }
//                        }
//                    }
                }
            }catch(IOException e) {
                e.printStackTrace();
            }
        });


        try {
            sendSocket = new DatagramSocket(14556);
        } catch (SocketException e) {
            e.printStackTrace();
        }

        Thread t2 = new Thread(() -> {
            try
            {

                int seq = 0;
                while(true)
                {

                    msg_heartbeat heart = new msg_heartbeat();
                    heart.type = MAV_TYPE.MAV_TYPE_GCS;
                    heart.autopilot = MAV_AUTOPILOT.MAV_AUTOPILOT_INVALID;
                    heart.system_status = MAV_STATE.MAV_STATE_ACTIVE;
                    heart.base_mode = MAV_MODE.MAV_MODE_GUIDED_ARMED;
                    heart.custom_mode = 0;
                    heart.sysid = M_SYS_ID;
                    heart.compid = M_COMP_ID;
                    heart.isMavlink2 = false;
                    heart.mavlink_version = 3;

                    Log.i("MAVLINK", "send "+ heart.toString());

                    MAVLinkPacket msg = heart.pack();
//                    msg.seq = seq;
//                    seq++;
//                    if(seq>255)
//                        seq = 0;
//                    msg.isMavlink2 = false;
//                    msg.sysid = M_SYS_ID;
//                    msg.compid = M_COMP_ID;



                    byte[] byteMsg = msg.encodePacket();
                    Log.i("MAVLINK", "send "+ printHexString(byteMsg));
                    DatagramPacket sendPacket = new DatagramPacket(byteMsg, byteMsg.length, InetAddress.getByName("10.0.0.1"),14556);
                    sendSocket.send(sendPacket);

                    Thread.sleep(2000);
                }
            }catch(IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        //t2.start(); // 启动新线程
        t1.start(); // 启动新线程

        Button btTest1 = (Button)findViewById(R.id.bt_send1);
        btTest1.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub

                Thread t = new Thread(() -> {
                    try
                    {

//                            msg_command_long cmd = new msg_command_long();
//                            cmd.msgid = 0;
//                            cmd.compid = 0;
//                            cmd.command = MAV_CMD.MAV_CMD_DO_SET_MODE;
//                            cmd.param1 = 0;
//                            cmd.param2 = 209;
//                            cmd.param3 = 7;

                        msg_command_long cmd = new msg_command_long();
                        cmd.sysid = M_SYS_ID;
                        cmd.compid = M_COMP_ID;
                        cmd.target_system = 0;
                        cmd.target_component = 0;

                        cmd.command = MAV_CMD.MAV_CMD_NAV_TAKEOFF;
                        cmd.param1 = 0;
                        cmd.param2 = 0;
                        cmd.param3 = 0;
                        cmd.param4 = 0;
                        cmd.param5 = 0;
                        cmd.param6 = 0;
                        cmd.param7 = 0;


                        Log.i("MAVLINK", "send "+ cmd.toString());

                            MAVLinkPacket msg = cmd.pack();
                            msg.isMavlink2 = false;
                            msg.sysid = M_SYS_ID;
                            msg.compid = M_COMP_ID;
                            byte[] byteMsg = msg.encodePacket();


                            DatagramPacket sendPacket = new DatagramPacket(byteMsg, byteMsg.length, InetAddress.getByName("10.0.0.1"),14556);
                            sendSocket.send(sendPacket);


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

                        msg_heartbeat heart = new msg_heartbeat();
                        heart.type = MAV_TYPE.MAV_TYPE_GCS;
                        heart.autopilot = MAV_AUTOPILOT.MAV_AUTOPILOT_INVALID;
                        heart.system_status = MAV_STATE.MAV_STATE_ACTIVE;
                        heart.base_mode = MAV_MODE.MAV_MODE_GUIDED_ARMED;
                        heart.custom_mode = 0;
                        heart.sysid = M_SYS_ID;
                        heart.compid = M_COMP_ID;
                        heart.isMavlink2 = false;
                        heart.mavlink_version = 3;

                        Log.i("MAVLINK", "send "+ heart.toString());

                        MAVLinkPacket msg = heart.pack();
//                    msg.isMavlink2 = false;
//                    msg.sysid = M_SYS_ID;
//                    msg.compid = M_COMP_ID;

                        byte[] byteMsg = msg.encodePacket();
                        DatagramPacket sendPacket = new DatagramPacket(byteMsg, byteMsg.length, InetAddress.getByName("10.0.0.1"),14556);
                        sendSocket.send(sendPacket);

                    }catch(IOException e) {
                        e.printStackTrace();
                    }
                });
                t.start(); // 启动新线程
            }
        });
    }
    public static String printHexString(byte[] b) {
        String res = "";
        for (int i = 0; i < b.length; i++) {
            String hex = Integer.toHexString(b[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            res += hex;
            res += " ";
        }
        return res;
    }
}