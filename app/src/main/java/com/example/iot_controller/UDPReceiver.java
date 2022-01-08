package com.example.iot_controller;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
//import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPReceiver extends BroadcastReceiver {

    public static final String OPEN = "0";
    public static final String CLOSE = "1";
    public static final String SWITCH = "2";
    public static final String EXTRA_DATA = "com.example.iot_controller.extra.DATA";

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        final String data = intent.getStringExtra(EXTRA_DATA);
        send(data);
    }

    public static void startAction(Context context, String data) {
        Intent intent = new Intent(context, UDPReceiver.class);
        intent.putExtra(EXTRA_DATA, data);
        context.sendBroadcast(intent);
    }

    public static void send(String s) {
//        Log.d("111", "hahaha");
        new Thread(() -> {
            byte[] data = s.getBytes();
            try (DatagramSocket mSocket = new DatagramSocket()) {
                mSocket.setBroadcast(true);
                InetAddress targetInetAddress = InetAddress.getByName("255.255.255.255");
                DatagramPacket datagramPacket =
                        new DatagramPacket(data, data.length, targetInetAddress, 8266);
                mSocket.send(datagramPacket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
}