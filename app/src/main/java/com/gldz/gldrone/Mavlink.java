package com.gldz.gldrone;

import static com.MAVLink.common.msg_altitude.MAVLINK_MSG_ID_ALTITUDE;
import static com.MAVLink.common.msg_attitude.MAVLINK_MSG_ID_ATTITUDE;
import static com.MAVLink.common.msg_available_modes.MAVLINK_MSG_ID_AVAILABLE_MODES;
import static com.MAVLink.common.msg_battery_status.MAVLINK_MSG_ID_BATTERY_STATUS;
import static com.MAVLink.common.msg_command_ack.MAVLINK_MSG_ID_COMMAND_ACK;
import static com.MAVLink.common.msg_mission_request.MAVLINK_MSG_ID_MISSION_REQUEST;
import static com.MAVLink.common.msg_rc_channels_raw.MAVLINK_MSG_ID_RC_CHANNELS_RAW;
import static com.MAVLink.common.msg_serial_control.MAVLINK_MSG_ID_SERIAL_CONTROL;
import static com.MAVLink.common.msg_servo_output_raw.MAVLINK_MSG_ID_SERVO_OUTPUT_RAW;
import static com.MAVLink.common.msg_set_mode.MAVLINK_MSG_ID_SET_MODE;
import static com.MAVLink.common.msg_sys_status.MAVLINK_MSG_ID_SYS_STATUS;
import static com.MAVLink.common.msg_vfr_hud.MAVLINK_MSG_ID_VFR_HUD;
import static com.MAVLink.minimal.msg_heartbeat.MAVLINK_MSG_ID_HEARTBEAT;

import android.util.Log;

import com.MAVLink.MAVLinkPacket;
import com.MAVLink.Parser;
import com.MAVLink.common.msg_attitude;
import com.MAVLink.common.msg_available_modes;
import com.MAVLink.common.msg_command_ack;
import com.MAVLink.common.msg_command_long;
import com.MAVLink.common.msg_manual_control;
import com.MAVLink.common.msg_rc_channels_override;
import com.MAVLink.common.msg_rc_channels_raw;
import com.MAVLink.common.msg_servo_output_raw;
import com.MAVLink.common.msg_set_mode;
import com.MAVLink.common.msg_sys_status;
import com.MAVLink.enums.MAV_AUTOPILOT;
import com.MAVLink.enums.MAV_CMD;
import com.MAVLink.enums.MAV_COMPONENT;
import com.MAVLink.enums.MAV_MODE;
import com.MAVLink.enums.MAV_MODE_FLAG;
import com.MAVLink.enums.MAV_STATE;
import com.MAVLink.enums.MAV_TYPE;
import com.MAVLink.minimal.msg_heartbeat;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class Mavlink {

    public static String[] MODE = {"MANUAL", "STABILIZED","OFFBOARD"};//, "ACRO",  "ALTCTL", "POSCTL", "AUTO_MISSION", "AUTO_LOITER", "AUTO_RTL", "AUTO_TAKEOFF", "STABILIZED", "AUTO_LAND", "AUTO_PRECLAND"};

    public ArrayList<String> modeList = new ArrayList<>();
    public ArrayList<Long> modeId = new ArrayList<>();
    public int fullModeCount = -1;

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

    private int heartBeatCheckCount = 0;
    private int cnt2s = 0;

    private boolean isFirstConnect = true;

    public boolean socketInitSuccess = false;
    public boolean is_armed = false;

    public Mavlink()
    {
        try {
            socket = new DatagramSocket(14550);
        } catch (SocketException e) {
            e.printStackTrace();
        }

        if(socket == null)
            return;

        Thread recvThread = new Thread(() -> {
            try {
                Parser parser = new Parser();
                byte[] receiveData = new byte[25565];

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




        Timer mTimer = new Timer();
        TimerTask mTimerTask = new TimerTask() {
            @Override
            public void run() {

                sendMsgHeartbeat();

                cnt2s++;
                if(cnt2s >= 2)
                {
                    //Log.i("MAVLINK", "mTimerTask");
                    cnt2s = 0;
                    if(heartBeatCheckCount > 0)
                    {
                        heartBeatCheckCount = 0;
                        mMavlinkListener.connect(true);
                        if(isFirstConnect)
                        {
                            isFirstConnect = false;
                            setFirst();
                        }

                    }else{
                        isFirstConnect = true;
                        mMavlinkListener.connect(false);
                    }
                }

            }
        };
        mTimer.schedule(mTimerTask, 1000,1000);

        socketInitSuccess = true;
    }

    public void setFirst()
    {
        setMode("STABILIZED");
        setMsgInterval();
    }

    public void setMsgInterval()
    {
        sendSetMsgInterval(MAVLINK_MSG_ID_ATTITUDE,1000000);
        sendSetMsgInterval(MAVLINK_MSG_ID_ALTITUDE,1000000);
        sendSetMsgInterval(MAVLINK_MSG_ID_RC_CHANNELS_RAW,1000000);
    }

    public void sendSetMsgInterval(int msgid,int interval)
    {
        msg_command_long cmd = new msg_command_long();
        cmd.sysid = M_SYS_ID;
        cmd.compid = M_COMP_ID;
        cmd.target_system = TARGET_SYS_ID;
        cmd.target_component = TARGET_COMP_ID;

        cmd.command = MAV_CMD.MAV_CMD_SET_MESSAGE_INTERVAL;
        cmd.param1 = msgid;
        cmd.param2 = interval;
        cmd.isMavlink2 = true;

//        sendMsg(cmd.pack());
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
        cmd.isMavlink2=true;

        //Log.i("MAVLINK", "send " + cmd.toString());

//        sendMsg(cmd.pack());
    }

    public void sendMsgSetCustomMode(long customMode)
    {
//        msg_command_long cmd = new msg_command_long();
//        cmd.sysid = M_SYS_ID;
//        cmd.compid = M_COMP_ID;
//        cmd.target_system = TARGET_SYS_ID;
//        cmd.target_component = TARGET_COMP_ID;
//
//        cmd.command = MAVLINK_MSG_ID_SET_MODE;
//        cmd.param1 = 1;
//        cmd.param2 = 1;
//        cmd.param3 = customMode;

        msg_set_mode msgmode = new msg_set_mode();
        msgmode.sysid = M_SYS_ID;
        msgmode.compid = M_COMP_ID;
        msgmode.base_mode = 1;
        msgmode.target_system = 1;
        msgmode.custom_mode = customMode;
        Log.i("MAVLINK", "send " + msgmode.toString());
        msgmode.isMavlink2 = true;


        sendMsg(msgmode.pack());
    }

    public void sendMsgHeartbeat()
    {
        msg_heartbeat heart = new msg_heartbeat();
        heart.type = MAV_TYPE.MAV_TYPE_GCS;
        heart.autopilot = MAV_AUTOPILOT.MAV_AUTOPILOT_INVALID;
        heart.base_mode = 192;
        heart.custom_mode = 0;
        heart.system_status = MAV_STATE.MAV_STATE_ACTIVE;
        heart.sysid = M_SYS_ID;
        heart.compid = M_COMP_ID;
        heart.isMavlink2 = true;
        heart.mavlink_version = 3;

        sendMsg(heart.pack());
    }

    public void sendMsgRC(int ch1,int ch2,int ch3,int ch4,int ch5,int ch6)
    {
        msg_rc_channels_override rc = new msg_rc_channels_override();
        rc.sysid = M_SYS_ID;
        rc.compid = M_COMP_ID;
        rc.target_system = TARGET_SYS_ID;
        rc.target_component = TARGET_COMP_ID;
        rc.chan1_raw = ch1;
        rc.chan2_raw = ch2;
        rc.chan3_raw = ch3;
        rc.chan4_raw = ch4;
        rc.chan5_raw = ch5;
        rc.chan6_raw = ch6;
        rc.chan7_raw = 1500;
        rc.chan8_raw = 1500;

//        sendMsg(rc.pack());
    }

    public void sendMsgManualControl(int channel_z, int channel_r, int channel_x, int channel_y)
    {
        msg_manual_control msgManualControl = new msg_manual_control();
        msgManualControl.isMavlink2 = true;
        msgManualControl.sysid = M_SYS_ID;
        msgManualControl.compid = M_COMP_ID;
        msgManualControl.target = 1;

        msgManualControl.x = (short) channel_x;
        msgManualControl.y = (short) channel_y;
        msgManualControl.r = (short) channel_r;
        msgManualControl.z = (short) channel_z;

        sendMsg(msgManualControl.pack());
    }


    public void sendMsgDisarm(int is_armed, float is_forced)
    {
        msg_command_long cmd = new msg_command_long();
        cmd.sysid = M_SYS_ID;
        cmd.compid = M_COMP_ID;
        cmd.target_system = TARGET_SYS_ID;
        cmd.target_component = TARGET_COMP_ID;

        cmd.command = MAV_CMD.MAV_CMD_COMPONENT_ARM_DISARM;
        cmd.param1 = is_armed;
        cmd.param2 = is_forced;

        sendMsg(cmd.pack());
    }


    private void recvMsgHeartbeat(msg_heartbeat heart)
    {

        byte mainMode = (byte)(heart.custom_mode >> 16);
        byte subMode =  (byte)(heart.custom_mode >> 24);
        PX4_MODE mode = getMode(mainMode,subMode);
        heartBeatCheckCount++;
        //Log.i("MAVLINK",String.valueOf(mainMode) + " " + String.valueOf(subMode) + " " + px4ModeString(mode));


        if((heart.base_mode & MAV_MODE_FLAG.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED) != 0)
        {

        }

        if((heart.base_mode & MAV_MODE_FLAG.MAV_MODE_FLAG_TEST_ENABLED) != 0){

        }

        if((heart.base_mode & MAV_MODE_FLAG.MAV_MODE_FLAG_AUTO_ENABLED) != 0){

        }

        if((heart.base_mode & MAV_MODE_FLAG.MAV_MODE_FLAG_GUIDED_ENABLED) != 0){

        }

        if((heart.base_mode & MAV_MODE_FLAG.MAV_MODE_FLAG_STABILIZE_ENABLED) != 0){

        }


        if((heart.base_mode & MAV_MODE_FLAG.MAV_MODE_FLAG_HIL_ENABLED) != 0){

        }

        if((heart.base_mode & MAV_MODE_FLAG.MAV_MODE_FLAG_MANUAL_INPUT_ENABLED) != 0){
        }

        if((heart.base_mode & MAV_MODE_FLAG.MAV_MODE_FLAG_SAFETY_ARMED) != 0){
            is_armed = true;
            mMavlinkListener.armed(true);
        }else{
            is_armed = false;
            mMavlinkListener.armed(false);
        }

        if((heart.base_mode & MAV_MODE_FLAG.MAV_MODE_FLAG_ENUM_END) != 0){

        }

        if(mMavlinkListener != null)
            mMavlinkListener.customMode(heart.custom_mode);
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
                DatagramPacket sendPacket = new DatagramPacket(byteMsg, byteMsg.length, InetAddress.getByName("10.0.0.2"), 14556);
//                DatagramPacket sendPacket = new DatagramPacket(byteMsg, byteMsg.length, InetAddress.getByName("172.25.151.141"), 18570);
                if(socket != null)
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
//            Log.e("MSGID RECV: ", String.valueOf(msg.msgid));

            switch (msg.msgid) {
                case MAVLINK_MSG_ID_AVAILABLE_MODES:
                    msg_available_modes msg_available_modes = new msg_available_modes();
                    msg_available_modes.unpack(msg.payload);
                    if (msg_available_modes.getMode_Name().contains("Mode not available")){
                        break;
                    }
                    if (msg_available_modes.getMode_Name().length() < 2){
                        break;
                    }
                    if (msg_available_modes.getMode_Name().charAt(0) == 0 &&
                            msg_available_modes.getMode_Name().charAt(1) == 0
                    ){
                        break;
                    }
                    fullModeCount = msg_available_modes.number_modes;
                    modeId.add(msg_available_modes.custom_mode);
                    modeList.add(msg_available_modes.getMode_Name());
                    break;
                case MAVLINK_MSG_ID_HEARTBEAT:
                    msg_heartbeat heart = new msg_heartbeat();
                    heart.unpack(msg.payload);
                    heart.sysid = msg.sysid;
                    heart.compid = msg.compid;
                    //Log.i("MAVLINK",heart.toString());
                    recvMsgHeartbeat(heart);
                    break;
                case MAVLINK_MSG_ID_SERIAL_CONTROL:
                    //Log.i("MAVLINK", "MAVLINK_MSG_ID_SERIAL_CONTROL");
                    break;

                case MAVLINK_MSG_ID_ALTITUDE:
                    //Log.i("MAVLINK", "MAVLINK_MSG_ID_ALTITUDE");
                    break;
                case MAVLINK_MSG_ID_ATTITUDE:
                    msg_attitude attitude = new msg_attitude();
                    attitude.unpack(msg.payload);
                    mMavlinkListener.attitude(attitude);
                    //Log.i("MAVLINK", "MAVLINK_MSG_ID_ATTITUDE");
                    break;
                case MAVLINK_MSG_ID_SERVO_OUTPUT_RAW:
                    //Log.i("MAVLINK", "MAVLINK_MSG_ID_SERVO_OUTPUT_RAW");
                    msg_servo_output_raw servo_output_raw = new msg_servo_output_raw();
                    servo_output_raw.unpack(msg.payload);
//                    mMavlinkListener.servo_output_raw(servo_output_raw);
                    break;
                case MAVLINK_MSG_ID_SYS_STATUS:
                    //Log.i("MAVLINK", "MAVLINK_MSG_ID_SYS_STATUS");
                    msg_sys_status sys_status = new msg_sys_status();
                    sys_status.unpack(msg.payload);
                    mMavlinkListener.sys_status(sys_status);
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
                    msg_command_ack m_msg_command_ack = new msg_command_ack();
                    m_msg_command_ack.unpack(msg.payload);

                    Log.i("MAVLINK", m_msg_command_ack.toString());
                    break;
                case MAVLINK_MSG_ID_RC_CHANNELS_RAW:
                    msg_rc_channels_raw m_msg_rc_channels_raw = new msg_rc_channels_raw();
                    m_msg_rc_channels_raw.unpack(msg.payload);
                    Log.i("MAVLINK", m_msg_rc_channels_raw.toString());
                    break;
                default:
                    //Log.e("MAVLINK", String.valueOf(msg.msgid));
                    break;
            }
        }
    }

    public void requestCustomModeList(){
            msg_command_long cmd = new msg_command_long();
            cmd.sysid = M_SYS_ID;
            cmd.compid = M_COMP_ID;
            cmd.target_system = TARGET_SYS_ID;
            cmd.target_component = TARGET_COMP_ID;
            cmd.isMavlink2 = true;

            cmd.command = MAV_CMD.MAV_CMD_REQUEST_MESSAGE;
            cmd.param1 = MAVLINK_MSG_ID_AVAILABLE_MODES;
            cmd.param2 = 0;
            cmd.param3 = 0;
            cmd.param4 = 0;
            cmd.param5 = 0;
            cmd.param6 = 0;
            cmd.param7 = 0;

            sendMsg(cmd.pack());
    }

    public void setMode(String mode){
        if(mode.equals("MANUAL"))
        {
            sendMsgSetMode(PX4_CUSTOM_MAIN_MODE_MANUAL,0);
        }
        else if(mode.equals("STABILIZED"))
        {
            sendMsgSetMode(PX4_CUSTOM_MAIN_MODE_STABILIZED,0);
        }else if(mode.equals("OFFBOARD")){
            sendMsgSetMode(PX4_CUSTOM_MAIN_MODE_OFFBOARD,0);
        }
    }

    public String px4ModeString(PX4_MODE mode)
    {
        String modeStr = "UNSUPPORT";
        switch (mode)
        {
            case MANUAL:
                modeStr = "MANUAL";
                break;
            case ALTCTL:
                modeStr = "ALTCTL";
                break;
            case POSCTL:
                modeStr = "POSCTL";
                break;
            case AUTO_MISSION:
                modeStr = "AUTO_MISSION";
                break;
            case AUTO_LOITER:
                modeStr = "AUTO_LOITER";
                break;
            case AUTO_RTL:
                modeStr = "AUTO_RTL";
                break;
            case ACRO:
                modeStr = "ACRO";
                break;
            case OFFBOARD:
                modeStr = "OFFBOARD";
                break;
            case STABILIZED:
                modeStr = "STABILIZED";
                break;
            case AUTO_TAKEOFF:
                modeStr = "AUTO_TAKEOFF";
                break;
            case AUTO_LAND:
                modeStr = "AUTO_LAND";
                break;
            case AUTO_PRECLAND:
                modeStr = "AUTO_PRECLAND";
                break;
            default:
                modeStr = "UNSUPPORT";
                break;
        }
        return modeStr;
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

    MavlinkListener mMavlinkListener = null;
    public void setMavlinkListener(MavlinkListener mavlinkListener) {
        mMavlinkListener = mavlinkListener;
    }
    public interface MavlinkListener {
        public void mode(PX4_MODE mode);
        public void customMode(long mode);
        public void connect(boolean isConnect);
        public void armed(boolean isArm);
        public void sys_status(msg_sys_status sys_status);
        public void servo_output_raw(msg_servo_output_raw servo_output_raw);
        public void attitude(msg_attitude attitude);

    }
}
