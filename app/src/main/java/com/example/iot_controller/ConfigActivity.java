package com.example.iot_controller;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
//import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.MutableLiveData;

import com.espressif.iot.esptouch.EsptouchTask;
import com.espressif.iot.esptouch.IEsptouchResult;
import com.espressif.iot.esptouch.IEsptouchTask;
import com.espressif.iot.esptouch.util.ByteUtil;
import com.espressif.iot.esptouch.util.TouchNetUtil;
import com.example.iot_controller.databinding.SettingsActivityBinding;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class ConfigActivity extends EspTouchActivityAbs {
//    private static final String TAG = ConfigActivity.class.getSimpleName();

    private static final int REQUEST_PERMISSION = 0x01;

    private EsptouchAsyncTask4 mTask;

    private SettingsActivityBinding mBinding;

    private String mSsid;
    private byte[] mSsidBytes;
    private String mBssid;

    private MutableLiveData<String> mBroadcastData;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, @NonNull Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                return;
            }

            switch (action) {
                case WifiManager.NETWORK_STATE_CHANGED_ACTION:
                case LocationManager.PROVIDERS_CHANGED_ACTION:
                    mBroadcastData.setValue(action);
                    break;
            }
        }
    };

    @Override
    protected String getEspTouchVersion() {
        return getString(R.string.esptouch1_about_version, IEsptouchTask.ESPTOUCH_VERSION);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = SettingsActivityBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
//        setContentView(R.layout.settings_activity);
//        if (savedInstanceState == null) {
//            getSupportFragmentManager()
//                    .beginTransaction()
//                    .replace(R.id.linearLayout, new SettingsFragment())
//                    .commit();
//        }

        mBinding.buttonCancel.setOnClickListener(v -> {
            showProgress(false);
            if (mTask != null) {
                mTask.cancelEsptouch();
            }
        });

        mBroadcastData = new MutableLiveData<>();
        IntentFilter filter = new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            filter.addAction(LocationManager.PROVIDERS_CHANGED_ACTION);
        }
        registerReceiver(mReceiver, filter);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION};
            requestPermissions(permissions, REQUEST_PERMISSION);
        }

        mBroadcastData.observe(this, broadcast -> {
//            Log.d(TAG, "onCreate: Broadcast=" + broadcast);
            onWifiChanged();
        });

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(mReceiver);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onWifiChanged();
            } else {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.esptouch1_location_permission_title)
                        .setMessage(R.string.esptouch1_location_permission_message)
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> finish())
                        .show();
            }

            return;
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private StateResult check() {
        StateResult result = checkPermission();
        if (!result.permissionGranted) {
            return result;
        }
        result = checkLocation();
        result.permissionGranted = true;
        if (result.locationRequirement) {
            return result;
        }
        result = checkWifi();
        result.permissionGranted = true;
        result.locationRequirement = false;
        return result;
    }

    private void onWifiChanged() {
        StateResult stateResult = check();
        mSsid = stateResult.ssid;
        mSsidBytes = stateResult.ssidBytes;
        mBssid = stateResult.bssid;
        CharSequence message = stateResult.message;
        boolean confirmEnable = false;
        if (stateResult.wifiConnected) {
            confirmEnable = true;
            if (stateResult.is5G) {
                message = getString(R.string.esptouch1_wifi_5g_message);
            }
        } else {
            if (mTask != null) {
                mTask.cancelEsptouch();
                mTask = null;
                new AlertDialog.Builder(ConfigActivity.this)
                        .setMessage(R.string.esptouch1_configure_wifi_change_message)
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            }
        }

        mBinding.textViewSsid.setText(mSsid);
        mBinding.textViewBssid.setText(mBssid);
        mBinding.textViewMessage.setText(message);
        mBinding.buttonConfirm.setEnabled(confirmEnable);
    }

    public void executeEsptouch(View view) {
        byte[] ssid = mSsidBytes == null ? ByteUtil.getBytesByString(this.mSsid)
                : mSsidBytes;
        String pwdStr = Objects.requireNonNull(mBinding.textInputLayoutPassword.getEditText()).
                getText().toString();
        byte[] password = ByteUtil.getBytesByString(pwdStr);
        byte[] bssid = TouchNetUtil.parseBssid2bytes(this.mBssid);
//        CharSequence devCountStr = mBinding.deviceCountEdit.getText();
//        byte[] deviceCount = devCountStr == null ? new byte[0] : devCountStr.toString().getBytes();
        byte[] deviceCount = "1".getBytes();
        byte[] broadcast = {(byte) 1};

        if (mTask != null) {
            mTask.cancelEsptouch();
        }
        mTask = new EsptouchAsyncTask4(this);
        mTask.execute(ssid, bssid, password, deviceCount, broadcast);
    }

    private void showProgress(boolean show) {
        if (show) {
            mBinding.content.setVisibility(View.INVISIBLE);
            mBinding.progressView.setVisibility(View.VISIBLE);
        } else {
            mBinding.content.setVisibility(View.VISIBLE);
            mBinding.progressView.setVisibility(View.GONE);
        }
    }


    private static class EsptouchAsyncTask4 extends AsyncTask<byte[], IEsptouchResult,
            List<IEsptouchResult>> {
        private final WeakReference<ConfigActivity> mActivity;

        private final Object mLock = new Object();
        private AlertDialog mResultDialog;
        private IEsptouchTask mEsptouchTask;

        EsptouchAsyncTask4(ConfigActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        void cancelEsptouch() {
            cancel(true);
            ConfigActivity activity = mActivity.get();
            if (activity != null) {
                activity.showProgress(false);
            }
            if (mResultDialog != null) {
                mResultDialog.dismiss();
            }
            if (mEsptouchTask != null) {
                mEsptouchTask.interrupt();
            }
        }

        @Override
        protected void onPreExecute() {
            ConfigActivity activity = mActivity.get();
            activity.mBinding.testResult.setText("");
            activity.showProgress(true);
        }

        @Override
        protected void onProgressUpdate(IEsptouchResult... values) {
            ConfigActivity activity = mActivity.get();
            if (activity != null) {
                IEsptouchResult result = values[0];
//                Log.i(TAG, "EspTouchResult: " + result);
                String text = result.getBssid() + " is connected to the wifi";
                Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();

                activity.mBinding.testResult.append(String.format(
                        Locale.ENGLISH,
                        "%s,%s\n",
                        result.getInetAddress().getHostAddress(),
                        result.getBssid()
                ));
            }
        }

        @Override
        protected List<IEsptouchResult> doInBackground(byte[]... params) {
            ConfigActivity activity = mActivity.get();
            int taskResultCount;
            synchronized (mLock) {
                byte[] apSsid = params[0];
                byte[] apBssid = params[1];
                byte[] apPassword = params[2];
                byte[] deviceCountData = params[3];
                byte[] broadcastData = params[4];
                taskResultCount = deviceCountData.length == 0 ? -1 :
                        Integer.parseInt(new String(deviceCountData));
                Context context = activity.getApplicationContext();
                mEsptouchTask = new EsptouchTask(apSsid, apBssid, apPassword, context);
                mEsptouchTask.setPackageBroadcast(broadcastData[0] == 1);
                mEsptouchTask.setEsptouchListener(this::publishProgress);
            }
            return mEsptouchTask.executeForResults(taskResultCount);
        }

        @Override
        protected void onPostExecute(List<IEsptouchResult> result) {
            ConfigActivity activity = mActivity.get();
            activity.mTask = null;
            activity.showProgress(false);
            if (result == null) {
                mResultDialog = new AlertDialog.Builder(activity)
                        .setMessage(R.string.esptouch1_configure_result_failed_port)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
                mResultDialog.setCanceledOnTouchOutside(false);
                return;
            }

            // check whether the task is cancelled and no results received
            IEsptouchResult firstResult = result.get(0);
            if (firstResult.isCancelled()) {
                return;
            }
            // the task received some results including cancelled while
            // executing before receiving enough results

            if (!firstResult.isSuc()) {
                mResultDialog = new AlertDialog.Builder(activity)
                        .setMessage(R.string.esptouch1_configure_result_failed)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
                mResultDialog.setCanceledOnTouchOutside(false);
                return;
            }

            ArrayList<CharSequence> resultMsgList = new ArrayList<>(result.size());
            for (IEsptouchResult touchResult : result) {
                String message = activity.getString(R.string.esptouch1_configure_result_success_item,
                        touchResult.getBssid(), touchResult.getInetAddress().getHostAddress());
                resultMsgList.add(message);
            }
            CharSequence[] items = new CharSequence[resultMsgList.size()];
            mResultDialog = new AlertDialog.Builder(activity)
                    .setTitle(R.string.esptouch1_configure_result_success)
                    .setItems(resultMsgList.toArray(items), null)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            mResultDialog.setCanceledOnTouchOutside(false);
        }
    }

//    public static class SettingsFragment extends PreferenceFragmentCompat {
//        @Override
//        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
//            setPreferencesFromResource(R.xml.root_preferences, rootKey);
//        }
//    }
}