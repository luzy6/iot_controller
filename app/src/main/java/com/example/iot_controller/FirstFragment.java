package com.example.iot_controller;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.iot_controller.databinding.FragmentFirstBinding;

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

        binding.buttonOpen.setOnClickListener(view1 -> UDPReceiver.startAction(
                this.getContext(), UDPReceiver.OPEN));
        binding.buttonClose.setOnClickListener(view12 -> UDPReceiver.startAction(
                this.getContext(), UDPReceiver.CLOSE));
        binding.buttonSwitch.setOnClickListener(view13 -> UDPReceiver.startAction(
                this.getContext(), UDPReceiver.SWITCH));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}