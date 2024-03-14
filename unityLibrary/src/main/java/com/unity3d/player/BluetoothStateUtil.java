package com.unity3d.player;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;



public class BluetoothStateUtil {

    private static final String TAG = "Main_BluetoothStateUtil";

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothStateBroadcastReceive mReceive;
    private Context mContext;
    private BluetoothStateInterface mBluetoothStateInterface;

    public BluetoothStateUtil(Context context) {
        mContext = context;
    }

    private List<String> discoveredDevices = new ArrayList<>();
    private BluetoothDiscoveryListener mBluetoothDiscoveryListener;
    private BluetoothLeScanner bluetoothLeScanner;

    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private String bluetoothDeviceNamed = "";

    /**
     * 初始化
     */
    public void Init(BluetoothStateInterface bluetoothStateInterface) {

        // 获得蓝牙适配器对象
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mContext = getActivity();
        mBluetoothStateInterface = bluetoothStateInterface;
    }

    public void setBluetoothDiscoveryListener(BluetoothDiscoveryListener listener) {
        mBluetoothDiscoveryListener = listener;
    }

    public void checkSelfPermission() {
        // 检查是否已经获取定位权限
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // 请求定位权限
            ActivityCompat.requestPermissions((Activity) mContext, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        } else {
            Log.d(TAG, "onRequestPermissionsResult: 已经获取定位权限");
            startDiscovery();
            // 已经获取定位权限
            // 可以在这里启动定位服务或执行其他操作
        }
    }

    public void startDiscovery() {
//        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//        Toast.makeText(mContext, "startDiscovery1", Toast.LENGTH_SHORT).show();
//        if (bluetoothAdapter == null) {
//            // 设备不支持蓝牙
//            Toast.makeText(mContext, "Device does not support Bluetooth", Toast.LENGTH_SHORT).show();
//            return;
//        }
////
//        if (bluetoothAdapter.isDiscovering()) {
//            Toast.makeText(mContext, "startDiscovery2", Toast.LENGTH_SHORT).show();
//            bluetoothAdapter.cancelDiscovery();
//        }
//
//        // 注册广播接收器
//        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
//        mContext.registerReceiver(discoveryReceiver, filter);
//        Toast.makeText(mContext, "startDiscovery3", Toast.LENGTH_SHORT).show();
//        bluetoothAdapter.startDiscovery();
//        stopScanBluetooth();
        // 检查是否已经获取定位权限
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // 请求定位权限
            ActivityCompat.requestPermissions((Activity) mContext, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        } else {
            Log.d(TAG, "onRequestPermissionsResult: 已经获取定位权限");
            // 已经获取定位权限
            // 可以在这里启动定位服务或执行其他操作
            //打开扫描
            //Android5.0（包括）以上扫描
            bluetoothLeScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
            if (bluetoothLeScanner != null) bluetoothLeScanner.startScan(highScanCallback);
        }
    }



    /**
     * 高版本扫描回调
     * Android 5.0（API 21）(包含)以上的蓝牙回调
     */
    public ScanCallback highScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
//            Toast.makeText(mContext, "startDiscovery4", Toast.LENGTH_SHORT).show();
            //根据目标蓝牙Mac地址去过滤
//            if (mMacAdress.equals(result.getDevice().getAddress())) {
            //关闭扫描(放子线程)
            //doing...
            Log.d("ThreadInfo", "Current thread: " + Thread.currentThread().getName());

            if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "onScanResult: wwwwwwww" +result.getDevice().getName());
                return;
            }

            if (result.getDevice().getName() != null && !discoveredDevices.contains(result.getDevice().getName())) {
                discoveredDevices.add(result.getDevice().getName());
                Log.d(TAG, "onScanResult-->" + "蓝牙扫描已找到设备" + result.getDevice().getName());
                  bluetoothDeviceNamed += result.getDevice().getName();
//                bluetoothDeviceNamed += result.getDevice().getName() + "\n";
//                Log.d(TAG, "onScanResult:"+ bluetoothDeviceNamed);

                // 使用 UnityPlayer 类获取 Unity 的当前活动，并调用其静态方法 UnitySendMessage
                try {
                    Class<?> unityPlayerClass = Class.forName("com.unity3d.player.UnityPlayer");
                    Object unityPlayer = unityPlayerClass.getField("currentActivity").get(null);
                    String unityObjectName = "Canvas"; // Unity 中的对象名称
                    String unityMethodName = "OnReceiveMessage"; // Unity 中的方法名称
                    String unityMessage = bluetoothDeviceNamed; // 要传递的消息

                    // 调用 Unity 的静态方法 UnitySendMessage，传递对象名称、方法名称和消息内容
                    unityPlayerClass.getMethod("UnitySendMessage", String.class, String.class, String.class) .invoke(unityPlayer, unityObjectName, unityMethodName, unityMessage);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (result.getDevice().getName().equals("LeesonIPHONEXS")) {
                    Toast.makeText(mContext, "stopScanBluetooth", Toast.LENGTH_SHORT).show();
                    stopScanBluetooth();
                }

            }

        }



        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            Log.d(TAG, "ScanCallback: onBatchScanResults");
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.e(TAG, "ScanCallback: onScanFailed");
        }
    };
    public void stopScanBluetooth() {
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        bluetoothLeScanner.stopScan(highScanCallback);
    }
    //    private final BroadcastReceiver discoveryReceiver = new BroadcastReceiver() {
//
//        public void onReceive(Context context, Intent intent) {
//            Toast.makeText(mContext, "discoveryReceiver1111", Toast.LENGTH_SHORT).show();
//            String action = intent.getAction();
//            Toast.makeText(mContext, "discoveryReceiver1", Toast.LENGTH_SHORT).show();
//            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
//                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//                Log.d(TAG, "BluetoothDevice---discoveredDevices:"+discoveredDevices);
//                if (device != null &&!discoveredDevices.contains(device) && device.getName()!= null) {
//                    discoveredDevices.add(device.getName());
//                    Toast.makeText(context, "stopDiscovery"+discoveryReceiver.toString(),
//                            Toast.LENGTH_SHORT).show();
//                    if (device.getName().equals("LeesonIPHONEXS")) {
//                        stopDiscovery();
//                        Toast.makeText(context, "stopDiscovery", Toast.LENGTH_SHORT).show();
//                    }
//                    Log.d(TAG, "discoveredDevices:"+discoveredDevices);
//                    if (mBluetoothDiscoveryListener != null) {
//                        mBluetoothDiscoveryListener.onDeviceDiscovered(device, device.getName());
//                    }
//                }
//            }
//        }
//    };


    public interface BluetoothDiscoveryListener {
        void onDeviceDiscovered(BluetoothDevice device, String deviceName);
    }
    /**
     * 获取蓝牙状态
     * @return
     */
    public boolean getBlueToothState() {
        // 获取蓝牙状态
        Toast.makeText(mContext, "getBlueToothState", Toast.LENGTH_SHORT).show();
        String message = "Bluetooth is " + (bluetoothAdapter.isEnabled() ? "enabled" : "disabled");
        Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
        return bluetoothAdapter.isEnabled();
    }
    /*
     * 蓝牙设备列表
     * */
    public List<String> getBluetoothDeviceList() {
        Log.d(TAG, "getBluetoothDeviceList: " + discoveredDevices);
//        stopDiscovery();
        return discoveredDevices;
    }
    /**
     * 开启蓝牙
     * @return
     */
    public boolean openBlueTooth() {
        // 打开蓝牙
        Toast.makeText(mContext, "openBlueTooth", Toast.LENGTH_SHORT).show();

        // 注册蓝牙状态变化的广播接收器
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                if (state == BluetoothAdapter.STATE_ON) {
                    // 蓝牙已打开，执行其他操作
                    Toast.makeText(mContext, "Bluetooth is enabled", Toast.LENGTH_SHORT).show();
                    // 可以在这里执行其他操作，如通知 Unity 蓝牙已打开等

                    setBluetoothDiscoveryListener(new BluetoothDiscoveryListener() {
                        public void onDeviceDiscovered(BluetoothDevice device, String deviceName) {
                            if (device != null && deviceName != null) {
                                // 处理发现的蓝牙设备
                                Log.d("Bluetooth", "device.getName:" + deviceName + " - " + device.getAddress());
                                Toast.makeText(mContext, "device.getName:" + deviceName + " - " + device.getAddress(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                    startDiscovery();
                }
            }
        }, filter);

        // 打开蓝牙
        return bluetoothAdapter.enable();
    }

    /**
     * 关闭蓝牙
     * @return
     */
    public boolean colseBlueTooth() {
        if (!getBlueToothState()) return true;
        Toast.makeText(mContext, "colseBlueTooth", Toast.LENGTH_SHORT).show();
        // 关闭蓝牙
        return bluetoothAdapter.disable();
    }

    // 调用系统的请求打开蓝牙
    public void gotoSystem(Context context){
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        context.startActivity(intent);
    }

    /**
     * 注册监听蓝牙变化
     */
    public void registerBluetoothReceiver(){
        if(mReceive == null){
            mReceive = new BluetoothStateBroadcastReceive();
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        intentFilter.addAction("android.bluetooth.BluetoothAdapter.STATE_OFF");
        intentFilter.addAction("android.bluetooth.BluetoothAdapter.STATE_ON");
        mContext.registerReceiver(mReceive, intentFilter);
    }

    /**
     * 取消监听蓝牙变化
     */
    public void unregisterBluetoothReceiver(){
        if(mReceive != null){
            mContext.unregisterReceiver(mReceive);
            mReceive = null;
        }
    }

    /**
     * 蓝牙状态变化监听
     */
    class BluetoothStateBroadcastReceive extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            switch (action){
                case BluetoothDevice.ACTION_ACL_CONNECTED:
                    //Toast.makeText(context , "蓝牙设备:" + device.getName() + "已连接", Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "onReceive: "+"蓝牙设备:" + device.getName() + "已连接");
                    break;
                case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                    //Toast.makeText(context , "蓝牙设备:" + device.getName() + "已断开", Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "onReceive: "+"蓝牙设备:" + device.getName() + "已断开");
                    break;
                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    int blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                    switch (blueState){
                        case BluetoothAdapter.STATE_OFF:
                            //Toast.makeText(context , "蓝牙已关闭", Toast.LENGTH_SHORT).show();
                            Log.i(TAG, "onReceive: "+"蓝牙已关闭:" );
                            mBluetoothStateInterface.onBluetoothStateOFF();
                            break;
                        case BluetoothAdapter.STATE_ON:
                            //Toast.makeText(context , "蓝牙已开启"  , Toast.LENGTH_SHORT).show();
                            Log.i(TAG, "onReceive: "+"蓝牙已开启:");
                            mBluetoothStateInterface.onBluetoothStateON();
                            break;
                    }
                    break;
            }
        }
    }


    // 设置一个 Activity 参数
    private Activity _unityActivity;

    // 通过反射获取 Unity 的 Activity 的上下文
    Activity getActivity(){

        if(null == _unityActivity){

            try{

                Class<?> classtype = Class.forName("com.unity3d.player.UnityPlayer");

                Activity activity = (Activity) classtype.getDeclaredField("currentActivity").get(classtype);

                _unityActivity = activity;

            }catch (ClassNotFoundException e){

                e.printStackTrace();

            }catch (IllegalAccessException e){

                e.printStackTrace();

            }catch (NoSuchFieldException e){

                e.printStackTrace();

            }

        }

        return _unityActivity;

    }

}
