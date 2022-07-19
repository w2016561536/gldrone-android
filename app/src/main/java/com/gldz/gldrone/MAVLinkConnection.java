package com.gldz.gldrone;

import android.os.Handler;
import android.util.Log;
import android.widget.Spinner;

import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import io.dronefleet.mavlink.MavlinkConnection;
import io.dronefleet.mavlink.MavlinkMessage;
import io.dronefleet.mavlink.common.CommandLong;
import io.dronefleet.mavlink.common.MissionItemInt;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.stream.BaseStream;
import java.util.stream.Collector;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import io.dronefleet.mavlink.common.CommandLong;
import io.dronefleet.mavlink.common.MavCmd;
import io.dronefleet.mavlink.common.MavFrame;
import io.dronefleet.mavlink.common.MissionItemInt;

public class MAVLinkConnection {

    private final static String     SEND_IP = "27.18.140.100";      //发送IP
    private final static int        SEND_PORT = 8989;               //发送端口号
    private final static int        RECEIVE_PORT = 8080;            //接收端口号

    private DatagramSocket recvSocket;
    private DatagramSocket sendSocket;
    private InetAddress serverAddr;



    static MavlinkConnection connection;
    MavlinkMessage message;
    Socket socket;
    Handler mHandler;
    Drone_Message drone_message = new Drone_Message();
    DatagramPacket receivePacket = null;
    boolean isAskingFeedback = false;

    public MAVLinkConnection(Handler mHandler) {
        this.mHandler = mHandler;
    }

    public void Create_Connection() {
        new Thread(() -> {
            Log.i("MAVLinkConnection", "StartUp");

            if (connection == null) {
                try {
                    // TCP connection
                    {

                        PipedOutputStream pipedOutputStream = new PipedOutputStream();
                        PipedInputStream pipedInputStream = new PipedInputStream(pipedOutputStream,10240);

                        PipedOutputStream pipedOutputStream2 = new PipedOutputStream();
                        PipedInputStream pipedInputStream2 = new PipedInputStream(pipedOutputStream2,10240);


                        Log.i("MAVLinkConnection", "Creat Socket");

                        connection = MavlinkConnection.create(pipedInputStream, pipedOutputStream2);
                        byte[] sendData = new byte[1024];
                        sendSocket = new DatagramSocket(14556);
                        Thread t = new Thread(() -> {
                            while(true)
                            {
                                try
                                    {
                                        pipedInputStream2.read(sendData);
                                        System.out.println("send:" + Integer.toString(sendData.length));
                                        SocketAddress remoteAddress = receivePacket.getSocketAddress();
                                        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, remoteAddress);

                                        sendSocket.send(sendPacket);
                                    }catch(IOException e) {
                                    e.printStackTrace();
                                    }
                            }
                        });
                        //t.start(); // 启动新线程

                        Thread t2 = new Thread(() -> {
                            while(true)
                            {

                                    CommandLong cmd = new CommandLong.Builder().command(MavCmd.MAV_CMD_COMPONENT_ARM_DISARM).param1(0).build();
                                    MAVLinkConnection.Send(cmd);
                                try
                                {
                                    Thread.sleep(1000000);
                                }catch(InterruptedException ex)
                                {
                                    Thread.currentThread().interrupt();
                                }

                            }
                        });
                        t2.start(); // 启动新线程

                        Thread t3 = new Thread(() -> {
                            while(true)
                            {

                                try
                                {
                                    byte[] receiveData = new byte[512];
                                    recvSocket = new DatagramSocket(14550);
                                    receivePacket = new DatagramPacket(receiveData, receiveData.length);
                                    while(true)
                                    {

                                        recvSocket.receive(receivePacket);// 在接收到信息之前，一直保持阻塞状态
                                        //System.out.println("recv:" + Integer.toString(receiveData.length));
                                        pipedOutputStream.write(receiveData);
                                    }
                                }catch(IOException e) {
                                    e.printStackTrace();
                                }

                            }
                        });
                        t3.start(); // 启动新线程


                    }


                    while ((message = connection.next()) != null) {

                        // If receive data, push them to Drone_Message.java
                        drone_message.Message_classify(message);
                        mHandler.obtainMessage(100, "Connected...").sendToTarget();

                        // Ask Drone Feedback in first connection
                        if(!isAskingFeedback){
                            Drone_Command.STATUS();
                            isAskingFeedback = true;
                        }
                    }

                } catch (EOFException eof) {
                    Release_Connection();
                    Log.i("MAVLinkConnection", "Connection Failed (SITL crush)");
                } catch (UnknownHostException e) {
                    Release_Connection();
                    Log.i("MAVLinkConnection", "Connection Failed");
                } catch (IOException e) {
                    Release_Connection();
                    Log.i("MAVLinkConnection", "Connection Failed (No WIFI or APP crush)");
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static void Send(CommandLong commandLong) {
        new Thread(() -> {
            if(connection != null) {
                try {
                    connection.send1(255, 0, commandLong);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static void Send(MissionItemInt missionItemInt) {
        new Thread(() -> {
            if(connection != null) {
                try {
                    connection.send1(255, 0, missionItemInt);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void Release_Connection() {
        try {
            if (socket != null) {
                if (socket.getOutputStream() != null) {
                    socket.getOutputStream().close();
                }
                socket.close();
            }
            connection = null;
            mHandler.obtainMessage(100, "Disconnected...").sendToTarget();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

