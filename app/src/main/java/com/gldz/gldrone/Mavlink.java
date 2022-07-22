package com.gldz.gldrone;

import static com.MAVLink.common.msg_altitude.MAVLINK_MSG_ID_ALTITUDE;
import static com.MAVLink.common.msg_attitude.MAVLINK_MSG_ID_ATTITUDE;
import static com.MAVLink.common.msg_battery_status.MAVLINK_MSG_ID_BATTERY_STATUS;
import static com.MAVLink.common.msg_command_ack.MAVLINK_MSG_ID_COMMAND_ACK;
import static com.MAVLink.common.msg_mission_request.MAVLINK_MSG_ID_MISSION_REQUEST;
import static com.MAVLink.common.msg_serial_control.MAVLINK_MSG_ID_SERIAL_CONTROL;
import static com.MAVLink.common.msg_servo_output_raw.MAVLINK_MSG_ID_SERVO_OUTPUT_RAW;
import static com.MAVLink.common.msg_sys_status.MAVLINK_MSG_ID_SYS_STATUS;
import static com.MAVLink.common.msg_vfr_hud.MAVLINK_MSG_ID_VFR_HUD;
import static com.MAVLink.minimal.msg_heartbeat.MAVLINK_MSG_ID_HEARTBEAT;

import android.util.Log;

import com.MAVLink.MAVLinkPacket;
import com.MAVLink.Parser;
import com.MAVLink.common.msg_command_ack;
import com.MAVLink.common.msg_command_long;
import com.MAVLink.enums.MAV_AUTOPILOT;
import com.MAVLink.enums.MAV_CMD;
import com.MAVLink.enums.MAV_COMPONENT;
import com.MAVLink.enums.MAV_MODE;
import com.MAVLink.enums.MAV_STATE;
import com.MAVLink.enums.MAV_TYPE;
import com.MAVLink.minimal.msg_heartbeat;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class Mavlink {

    enum PX4_MODE
    {
        MANUAL,
        ALTCTL,
        POSCTL,
        AUTO_MISSION,
        AUTO_LOITER,
        AUTO_RTL,
        ACRO,
        OFFBOARD,
        STABILIZED,
        AUTO_TAKEOFF,
        AUTO_LAND,
        AUTO_PRECLAND,
        UNSUPPORT

    };

    final static int PX4_CUSTOM_MAIN_MODE_MANUAL = 1;
    final static int PX4_CUSTOM_MAIN_MODE_ALTCTL = 2;
    final static int PX4_CUSTOM_MAIN_MODE_POSCTL = 3;
    final static int PX4_CUSTOM_MAIN_MODE_AUTO = 4;
    final static int PX4_CUSTOM_MAIN_MODE_ACRO = 5;
    final static int PX4_CUSTOM_MAIN_MODE_OFFBOARD = 6;
    final static int PX4_CUSTOM_MAIN_MODE_STABILIZED = 7;
    final static int PX4_CUSTOM_MAIN_MODE_RATTITUDE_LEGACY = 8;
    final static int PX4_CUSTOM_MAIN_MODE_SIMPLE = 9; /* unused, but reserved for future use */

    final static int PX4_CUSTOM_SUB_MODE_AUTO_READY = 1;
    final static int PX4_CUSTOM_SUB_MODE_AUTO_TAKEOFF = 2;
    final static int PX4_CUSTOM_SUB_MODE_AUTO_LOITER = 3;
    final static int PX4_CUSTOM_SUB_MODE_AUTO_MISSION = 4;
    final static int PX4_CUSTOM_SUB_MODE_AUTO_RTL = 5;
    final static int PX4_CUSTOM_SUB_MODE_AUTO_LAND = 6;
    final static int PX4_CUSTOM_SUB_MODE_AUTO_RESERVED_DO_NOT_USE = 7; // was PX4_CUSTOM_SUB_MODE_AUTO_RTGS, deleted 2020-03-05
    final static int PX4_CUSTOM_SUB_MODE_AUTO_FOLLOW_TARGET = 8;
    final static int PX4_CUSTOM_SUB_MODE_AUTO_PRECLAND = 9;
    final static int PX4_CUSTOM_SUB_MODE_AUTO_VTOL_TAKEOFF = 10;

    final static int PX4_CUSTOM_SUB_MODE_POSCTL_POSCTL = 0;
    final static int PX4_CUSTOM_SUB_MODE_POSCTL_ORBIT = 1;

    final static int TARGET_SYS_ID = 1;
    final static int TARGET_COMP_ID = 1;

    final static int M_SYS_ID = 255;
    final static int M_COMP_ID = MAV_COMPONENT.MAV_COMP_ID_MISSIONPLANNER;

    private int seq = 0;

    private DatagramSocket socket;

    public Mavlink()
    {
        try {
            socket = new DatagramSocket(14550);
        } catch (SocketException e) {
            e.printStackTrace();
        }

        Thread recvThread = new Thread(() -> {
            try {
                Parser parser = new Parser();
                byte[] receiveData = new byte[4096];

                while (true) {
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    socket.receive(receivePacket);
                    for (int i = 0; i < receivePacket.getLength(); i++) {
                        MAVLinkPacket msg = parser.mavlink_parse_char(receiveData[i]);
                        recvMsg(msg);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        recvThread.start();

        Thread heartbeatThread = new Thread(() -> {
            try {
                while (true) {
                    sendMsgHeartbeat();
                    Thread.sleep(1000);
                }
            }catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        heartbeatThread.start();



    }

    public void sendMsgSetMode(int mainMode,int subMode)
    {
        msg_command_long cmd = new msg_command_long();
        cmd.sysid = M_SYS_ID;
        cmd.compid = M_COMP_ID;
        cmd.target_system = TARGET_SYS_ID;
        cmd.target_component = TARGET_COMP_ID;

        cmd.command = MAV_CMD.MAV_CMD_DO_SET_MODE;
        cmd.param1 = 1;
        cmd.param2 = mainMode;
        cmd.param3 = subMode;

        //Log.i("MAVLINK", "send " + cmd.toString());

        sendMsg(cmd.pack());
    }

    public void sendMsgHeartbeat()
    {
        msg_heartbeat heart = new msg_heartbeat();
        heart.type = MAV_TYPE.MAV_TYPE_GCS;
        heart.autopilot = MAV_AUTOPILOT.MAV_AUTOPILOT_INVALID;
        heart.base_mode = MAV_MODE.MAV_MODE_GUIDED_ARMED;
        heart.custom_mode = 0;
        heart.system_status = MAV_STATE.MAV_STATE_ACTIVE;
        heart.sysid = M_SYS_ID;
        heart.compid = M_COMP_ID;
        //heart.isMavlink2 = true;
        heart.mavlink_version = 3;

        sendMsg(heart.pack());
    }

    private void recvMsgHeartbeat(msg_heartbeat heart)
    {

        byte mainMode = (byte)(heart.custom_mode >> 16);
        byte subMode =  (byte)(heart.custom_mode >> 24);
        PX4_MODE mode = getMode(mainMode,subMode);
        Log.i("MAVLINK",String.valueOf(mainMode) + " " + String.valueOf(subMode));
    }

    private PX4_MODE getMode(byte mainMode,byte subMode)
    {
        if(mainMode == PX4_CUSTOM_MAIN_MODE_MANUAL)
        {
            return PX4_MODE.MANUAL;
        }else if(mainMode == PX4_CUSTOM_MAIN_MODE_ALTCTL)
        {
            return PX4_MODE.ALTCTL;
        }else if(mainMode == PX4_CUSTOM_MAIN_MODE_POSCTL)
        {
            return PX4_MODE.POSCTL;
        }else if(mainMode == PX4_CUSTOM_MAIN_MODE_AUTO)
        {
            if(subMode == PX4_CUSTOM_SUB_MODE_AUTO_LOITER)
            {
                return PX4_MODE.AUTO_LOITER;
            }else if(subMode == PX4_CUSTOM_SUB_MODE_AUTO_RTL)
            {
                return PX4_MODE.AUTO_RTL;
            }else if(subMode == PX4_CUSTOM_SUB_MODE_AUTO_MISSION)
            {
                return PX4_MODE.AUTO_MISSION;
            }else if(subMode == PX4_CUSTOM_SUB_MODE_AUTO_TAKEOFF)
            {
                return PX4_MODE.AUTO_TAKEOFF;
            }else if(subMode == PX4_CUSTOM_SUB_MODE_AUTO_LAND)
            {
                return PX4_MODE.AUTO_LAND;
            }else if(subMode == PX4_CUSTOM_SUB_MODE_AUTO_PRECLAND)
            {
                return PX4_MODE.AUTO_PRECLAND;
            }else{
                return PX4_MODE.UNSUPPORT;
            }
        }else if(mainMode == PX4_CUSTOM_MAIN_MODE_ACRO)
        {
            return PX4_MODE.ACRO;
        }else if(mainMode == PX4_CUSTOM_MAIN_MODE_OFFBOARD)
        {
            return PX4_MODE.OFFBOARD;
        }else if(mainMode == PX4_CUSTOM_MAIN_MODE_STABILIZED)
        {
            return PX4_MODE.STABILIZED;
        }

        return PX4_MODE.UNSUPPORT;
    }

    private void sendMsg(MAVLinkPacket msg)
    {
        Thread t = new Thread(() -> {
            try {
                msg.seq = seq++;
                byte[] byteMsg = msg.encodePacket();
                DatagramPacket sendPacket = new DatagramPacket(byteMsg, byteMsg.length, InetAddress.getByName("10.0.0.1"), 14556);
                socket.send(sendPacket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        t.start(); // 启动新线程
    }
    private void recvMsg(MAVLinkPacket msg)
    {
        if (msg != null) {
            switch (msg.msgid) {
                case MAVLINK_MSG_ID_HEARTBEAT:
                    msg_heartbeat heart = new msg_heartbeat();
                    heart.unpack(msg.payload);
                    heart.sysid = msg.sysid;
                    heart.compid = msg.compid;
//                    Log.i("MAVLINK",heart.toString());
                    recvMsgHeartbeat(heart);
                    break;
                case MAVLINK_MSG_ID_SERIAL_CONTROL:
                    //Log.i("MAVLINK", "MAVLINK_MSG_ID_SERIAL_CONTROL");
                    break;

                case MAVLINK_MSG_ID_ALTITUDE:
                    //Log.i("MAVLINK", "MAVLINK_MSG_ID_ALTITUDE");
                    break;
                case MAVLINK_MSG_ID_ATTITUDE:
                    //Log.i("MAVLINK", "MAVLINK_MSG_ID_ATTITUDE");
                    break;
                case MAVLINK_MSG_ID_SERVO_OUTPUT_RAW:
                    //Log.i("MAVLINK", "MAVLINK_MSG_ID_SERVO_OUTPUT_RAW");
                    break;
                case MAVLINK_MSG_ID_SYS_STATUS:
                    //Log.i("MAVLINK", "MAVLINK_MSG_ID_SYS_STATUS");
                    break;
                case MAVLINK_MSG_ID_VFR_HUD:
                    //Log.i("MAVLINK", "MAVLINK_MSG_ID_VFR_HUD");
                    break;
                case MAVLINK_MSG_ID_BATTERY_STATUS:
                    //Log.i("MAVLINK", "MAVLINK_MSG_ID_BATTERY_STATUS");
                    break;
                case MAVLINK_MSG_ID_MISSION_REQUEST:
                    //Log.i("MAVLINK", "MAVLINK_MSG_ID_MISSION_REQUEST");
                    break;
                case MAVLINK_MSG_ID_COMMAND_ACK:
                    msg_command_ack pmsg = new msg_command_ack();
                    pmsg.unpack(msg.payload);
//                                    if(pmsg.result == MAV_RESULT.

                    Log.i("MAVLINK", pmsg.toString());
                    break;

                default:
                    //Log.e("MAVLINK", String.valueOf(msg.msgid));
                    break;
            }
        }
    }
}
