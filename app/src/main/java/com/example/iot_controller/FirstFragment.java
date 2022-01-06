package com.example.iot_controller;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.iot_controller.databinding.FragmentFirstBinding;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class FirstFragment extends Fragment {

    private FragmentFirstBinding binding;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentFirstBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.buttonOpen.setOnClickListener(view1 -> send("0"));
        binding.buttonClose.setOnClickListener(view12 -> send("1"));
        binding.buttonSwitch.setOnClickListener(view13 -> send("2"));
    }

    public static void send(String s) {
        new Thread(() -> {
            byte[] data = s.getBytes();
            try (DatagramSocket mSocket = new DatagramSocket()) {
                mSocket.setBroadcast(true);
                InetAddress targetInetAddress = InetAddress.getByName("255.255.255.255");
                DatagramPacket datagramPacket = new DatagramPacket(data, data.length, targetInetAddress, 8266);
                mSocket.send(datagramPacket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}