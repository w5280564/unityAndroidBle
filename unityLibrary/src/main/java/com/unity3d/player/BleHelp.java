package com.unity3d.player;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.location.LocationManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class BleHelp {

    private Activity context;
    private static final String TAG = "BleHelp-->";

    //UUID和Mac地址
    private String mServiceUUID;
    private String mReadCharacteristicUUID;//特征uuid
    private String mWriteCharacteristicUUID;//特征uuid
    private String mMacAdress;

    //蓝牙设备
    private BluetoothDevice mBluetoothDevice;
    //蓝牙服务
    private BluetoothGatt mBluetoothGatt;
    //子线程的HandlerThread，为子线程提供Looper
    private HandlerThread workHandlerThread;
    //子线程
    private Handler workHandler;
    //蓝牙读取特征值
    BluetoothGattCharacteristic mReadGattCharacteristic;
    //蓝牙写出特征值
    BluetoothGattCharacteristic mWriteGattCharacteristic;


    private static final int LINK_TIME_OUT = 1000;
    private static final int START_SCAN = 1001;
    private static final int STOP_SCAN = 1002;
    private static final int CONNECT_GATT = 1003;
    private static final int DISCOVER_SERVICES = 1004;
    private static final int DISCONNECT_GATT = 1005;
    private static final int CLOSE_GATT = 1006;
    private static final int SEND_DATA = 1007;

    //调用disConnect()方法后是否需要调用close方法
    private boolean isDisConnectNeedClose = true;
    //Android8.0以上，退到后台或者息屏后，是否还需要扫描（谷歌为省电8.0以上默认关闭）
    private boolean isAllowSacnHomeSuperM = false;
    //默认连接时间25秒
    private int linkTime = 25000;

    //设备列表
    List<HashMap<String, Object>> devices = new ArrayList<>();

    private BleCallback bleCallback;

    private BleHelp() {
    }

    public static BleHelp getInstance() {
        return SingleInstance.sInstance;
    }

    /**
     * 静态内部类，单例
     */
    private static class SingleInstance {
        private static final BleHelp sInstance = new BleHelp();
    }

    public void init(Activity activity, BleCallback bleCallback) {
        this.context = activity;
        this.bleCallback = bleCallback;
    }

    private boolean checkAllUUID(Activity activity) {
        this.context = activity;
        if (this.context == null) {
            Log.e(TAG, "BleHelp初始化失败：" + "context为空......");
            Toast.makeText(context, "蓝牙模块初始化失败，请联系开发商...", Toast.LENGTH_LONG).show();
            return false;
        }
        if (this.bleCallback == null) {
            Log.e(TAG, "BleHelp初始化失败：" + "bleCallback为空......");
            Toast.makeText(context, "蓝牙模块初始化失败，请联系开发商...", Toast.LENGTH_LONG).show();
            return false;
        }
        if (!enable()) {
            Log.e(TAG, "BleHelp初始化失败：" + "（用户操作）未打开手机蓝牙，蓝牙功能无法使用......");
            Toast.makeText(context, "未打开手机蓝牙,蓝牙功能无法使用...", Toast.LENGTH_LONG).show();
            return false;
        }
        if (!isOPenGps()) {
            Log.e(TAG, "BleHelp初始化失败：" + "（用户操作）GPS未打开，蓝牙功能无法使用...");
            Toast.makeText(context, "GPS未打开，蓝牙功能无法使用", Toast.LENGTH_LONG).show();
            return false;
        }
//        if (!BluetoothAdapter.checkBluetoothAddress(mMacAdress)) {
//            Log.e(TAG, "BleHelp初始化失败：" + "不是一个有效的蓝牙MAC地址，蓝牙功能无法使用...");
//            Toast.makeText(context, "不是一个有效的蓝牙MAC地址，蓝牙功能无法使用", Toast.LENGTH_LONG).show();
//            return false;
//        }
//        if (mServiceUUID == null) {
//            Log.e(TAG, "BleHelp初始化失败：" + "gattServiceUUID为空，蓝牙功能无法使用...");
//            Toast.makeText(context, "gattServiceUUID为空，蓝牙功能无法使用", Toast.LENGTH_LONG).show();
//            return false;
//        }
//        if (mReadCharacteristicUUID == null) {
//            Log.e(TAG, "BleHelp初始化失败：" + "mReadCharacteristicUUID为空，蓝牙功能无法使用...");
//            Toast.makeText(context, "mReadCharacteristicUUID为空，蓝牙功能无法使用", Toast.LENGTH_LONG).show();
//            return false;
//        }
//        if (mWriteCharacteristicUUID == null) {
//            Log.e(TAG, "BleHelp初始化失败：" + "mWriteCharacteristicUUID为空，蓝牙功能无法使用...");
//            Toast.makeText(context, "mWriteCharacteristicUUID为空，蓝牙功能无法使用", Toast.LENGTH_LONG).show();
//            return false;
//        }
        return true;

    }

    public void setMacAndUuids(String macAdress, String gattServiceUUID,
                               String readGattCharacteristicUUID, String writeGattCharacteristicUUID) {
        this.mMacAdress = macAdress;
        this.mServiceUUID = gattServiceUUID;
        this.mReadCharacteristicUUID = readGattCharacteristicUUID;
        this.mWriteCharacteristicUUID = writeGattCharacteristicUUID;
    }

    public void setLinkTime(int linkTime) {
        this.linkTime = linkTime;
    }

    public void start() {
        if (!checkAllUUID(context)) return;
        initWorkHandler();
        permissionLocation();
    }

    private void initWorkHandler() {
        workHandlerThread = new HandlerThread("BleWorkHandlerThread");
        workHandlerThread.start();
        workHandler = new Handler(workHandlerThread.getLooper()) {
            @SuppressLint("MissingPermission")
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case LINK_TIME_OUT:
                        removeMessages(LINK_TIME_OUT);
                        sendEmptyMessage(STOP_SCAN);
                    case START_SCAN: {
                        BluetoothLeScanner bluetoothLeScanner = getBluetoothAdapter().getBluetoothLeScanner();
                        if (bluetoothLeScanner == null) return;
                        //允许8.0以上退到后台能继续扫描
                        if (isAllowSacnHomeSuperM) {//Android8.0以上退到后台或息屏后是否还要扫描。我们将其默认为false
                            //doing....
                            return;
                        }
                        bluetoothLeScanner.startScan(highScanCallback);
                        return;
                    }
                    case STOP_SCAN:
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                            BluetoothLeScanner bluetoothLeScanner = getBluetoothAdapter().getBluetoothLeScanner();
                            if (bluetoothLeScanner != null)
                                bluetoothLeScanner.stopScan(highScanCallback);
                        } else
                            getBluetoothAdapter().stopLeScan(lowScanCallback);
                        //停止搜索需要一定的时间来完成，建议加以100ms的延时，保证系统能够完全停止搜索蓝牙设备。
                        sleep();
                        break;
                    case CONNECT_GATT:
                        mBluetoothGatt = mBluetoothDevice.connectGatt(context, false, bluetoothGattCallback);
                        break;
                    case DISCOVER_SERVICES:
                        mBluetoothGatt.discoverServices();
                        break;
                    case DISCONNECT_GATT:
                        boolean isRefreshSuccess = refreshDeviceCache(mBluetoothGatt);
                        if (isRefreshSuccess) mBluetoothGatt.disconnect();
                        else
                            Log.e(TAG, "bluetoothGatt断开连接失败：因清除bluetoothGatt缓存失败,故未调用disconnect()方法");
                        break;
                    case CLOSE_GATT://需要disconnect()方法后回调onConnectionStateChange，再调用close()，
                        mBluetoothGatt.close();
                        mBluetoothGatt = null;
                        Log.d(TAG, "bluetoothGatt关闭成功并置为null");
                        break;
                    case SEND_DATA:
                        sendData((byte[]) msg.obj);
                        break;
                }
            }
        };
    }

    /**
     * 是否手机蓝牙状态
     *
     * @return true 表示处于打开状态，false表示处于关闭状态
     */
    public boolean isEnabled() {
        boolean isEnabled = getBluetoothAdapter().isEnabled();
        Log.d(TAG, "手机蓝牙是否打开：" + isEnabled);
        return isEnabled;
    }

    /**
     * 打开手机蓝牙
     *
     * @return true 表示打开成功
     */
    public boolean enable() {
        if (!getBluetoothAdapter().isEnabled()) {
            //若未打开手机蓝牙，则会弹出一个系统的是否打开/关闭蓝牙的对话框，禁止或者未处理返回false，允许返回true
            //若已打开手机蓝牙，直接返回true
            @SuppressLint("MissingPermission") boolean enableState = getBluetoothAdapter().enable();
            Log.d(TAG, "（用户操作）手机蓝牙是否打开成功：" + enableState);
            return enableState;
        } else return true;
    }

    /**
     * 关闭手机蓝牙
     *
     * @return true 表示关闭成功
     */
    public boolean disable() {
        if (getBluetoothAdapter().isEnabled()) {
            @SuppressLint("MissingPermission") boolean disabledState = getBluetoothAdapter().disable();
            Log.d(TAG, "（用户操作）手机蓝牙是否关闭成功：" + disabledState);
            return disabledState;
        } else return true;
    }

    /**
     * 判断是否可以通过Mac地址直连
     * 判断通过Mac地址获取到的Device的name是否为空来确定是否可以直连
     * 该方式不是绝对的，仅供参考，需具体情况具体分析
     */
    @SuppressLint("MissingPermission")
    private boolean isDirectConnect() {
        if (mMacAdress == null) return false;
        BluetoothDevice device = getBluetoothAdapter().getRemoteDevice(mMacAdress);
        if (device.getName() != null) {
            mBluetoothDevice = null;
            mBluetoothDevice = device;
            return true;
        } else return false;
    }

    /**
     * 断开连接
     *
     * @param isNeedClose 执行mBluetoothGatt.disconnect方法后是否需要执行mBluetoothGatt.close方法
     *                    执行
     */
    public void disConnect(boolean isNeedClose) {
        if (mBluetoothGatt == null) return;
        isDisConnectNeedClose = isNeedClose;
        workHandler.sendEmptyMessage(DISCONNECT_GATT);
    }

    /**
     * 该方法作为扩展方法，暂时设为private
     * 8.0以上退到后台或者息屏后，在没有停止扫描的情况下是否还能继续扫描，谷歌默认不扫描
     */
    private void allowSacnHomeSuperM(boolean isAllow) {
        this.isAllowSacnHomeSuperM = isAllow;
    }

    public void sendDataToDevice(byte[] data) {
        Message message = new Message();
        message.what = SEND_DATA;
        message.obj = data;
        workHandler.sendMessage(message);
    }

    @SuppressLint("MissingPermission")
    private void sendData(byte[] data) {
        try {
            if (data.length <= 20) {
                if (mWriteGattCharacteristic == null) {
                    Log.e(TAG, "mWriteGattCharacteristic为空，发送数据失败");
                    return;
                }
                if (mBluetoothGatt == null) {
                    Log.e(TAG, "mBluetoothGatt为空，发送数据包失败");
                    return;
                }
                mWriteGattCharacteristic.setValue(data);
                mBluetoothGatt.writeCharacteristic(mWriteGattCharacteristic);
            } else {
                Log.i(TAG, "数据包分割");
                byte[] b1 = new byte[20];
                byte[] b2 = new byte[data.length - 20];
                for (int i = 0; i < 20; i++) {
                    b1[i] = data[i];
                }
                for (int i = 20; i < data.length; i++) {
                    b2[i - 20] = data[i];
                }
                sendData(b1);
                sleep();
                sendData(b2);
                sleep();//防止下一条数据发送过快
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "发送数据包异常" + e.toString());
        }
    }

    public void startBle() {

    }

    List<BluetoothDevice> deviceS = new ArrayList<>();
    String deviceNameString = "";
    /**
     * 高版本扫描回调
     * Android 5.0（API 21）(包含)以上的蓝牙回调
     */
    private ScanCallback highScanCallback = new ScanCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            Log.d(TAG, "LeScanCallback-->" + "扫描啊啊" + result.getDevice().getAddress());
            Log.d(TAG, "蓝牙扫描地址" + result.getDevice().getAddress() + result.getDevice().getName());
//            if (mMacAdress != null && mMacAdress.equals(result.getDevice().getAddress())) {
//                workHandler.removeMessages(LINK_TIME_OUT);
//                workHandler.sendEmptyMessage(STOP_SCAN);
//                Log.d(TAG, "LeScanCallback-->" + "蓝牙扫描已找到设备，即将开始连接");
//                mBluetoothDevice = null;
//                mBluetoothDevice = result.getDevice();
//                workHandler.sendEmptyMessage(CONNECT_GATT);
//            }

            if (result.getDevice().getName() != null && !deviceS.contains(result.getDevice())) {
                deviceS.add(result.getDevice());
                deviceNameString = result.getDevice().getName();
                sendUnity(deviceNameString);
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


    /**
     * 低版本扫描回调
     * Android 4.3（API 18）（包含）以上，Android 5.0（API 21）（不包含）以下的蓝牙回调
     */
    private BluetoothAdapter.LeScanCallback lowScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            if (mMacAdress.equals(device.getAddress())) {
                workHandler.removeMessages(LINK_TIME_OUT);
                workHandler.sendEmptyMessage(STOP_SCAN);
                Log.d(TAG, "LeScanCallback-->" + "蓝牙扫描已找到设备，即将开始连接");
                mBluetoothDevice = null;
                mBluetoothDevice = device;
                workHandler.sendEmptyMessage(CONNECT_GATT);
            }
        }
    };

    /**
     * 回调都是在子线程中，不可做更新 UI 操作
     */
    private BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            super.onPhyUpdate(gatt, txPhy, rxPhy, status);
            Log.d(TAG, "onPhyUpdate");
        }

        @Override
        public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            super.onPhyRead(gatt, txPhy, rxPhy, status);
            Log.d(TAG, "onPhyRead");
        }

        //
        //status-->操作是否成功，如连接成功这个操作是否成功。会返回异常码
        //newState-->新的连接的状态。共四种：STATE_DISCONNECTED，STATE_CONNECTING，STATE_CONNECTED，STATE_DISCONNECTING
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                switch (newState) {
                    case BluetoothProfile.STATE_DISCONNECTED:
                        Log.d(TAG, "BluetoothGattCallback：onConnectionStateChange-->" + "status：" + status + "操作成功；" + " newState：" + newState + " 已断开连接状态");
                        if (isDisConnectNeedClose) workHandler.sendEmptyMessage(CLOSE_GATT);
                        break;
                    case BluetoothProfile.STATE_CONNECTED:
                        Log.d(TAG, "BluetoothGattCallback：onConnectionStateChange-->" + "status：" + status + "操作成功；" + " newState：" + newState + " 已连接状态，可进行发现服务");
                        //发现服务
                        workHandler.sendEmptyMessage(DISCOVER_SERVICES);
                        break;
                }
                return;
            }
            Log.e(TAG, "BluetoothGattCallback：onConnectionStateChange-->" + "status：" + status + "操作失败；" + " newState：" + newState);
            if (status == 133) {//需要清除Gatt缓存并断开连接和关闭Gatt，然后重新连接
                gattError133("onConnectionStateChange");
            }

        }

        //发现服务成功后，会触发该回调方法。status：远程设备探索是否成功
        @SuppressLint("MissingPermission")
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            for (int i = 0; i < gatt.getServices().size(); i++) {
                Log.d(TAG, "onServicesDiscovered-->" + "status:" + status + "操作成功急啊急啊" + gatt.getServices().get(i).getUuid());
            }
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "onServicesDiscovered-->" + "status:" + status + "操作成功");
                //根据指定的服务uuid获取指定的服务
                BluetoothGattService gattService = gatt.getService(UUID.fromString(mServiceUUID));
                if (gattService == null) {
                    Log.e(TAG, "onServicesDiscovered-->" + "获取服务指定uuid：" + mServiceUUID + "的BluetoothGattService为空，请联系外设设备开发商确认uuid是否正确");
                    return;
                }
                //根据指定特征值uuid获取指定的特征值一
                mReadGattCharacteristic = gattService.getCharacteristic(UUID.fromString(mReadCharacteristicUUID));
                if (mReadGattCharacteristic == null) {
                    Log.e(TAG, "onServicesDiscovered-->" + "获取指定特征值的uuid：" + mReadCharacteristicUUID + "的BluetoothGattCharacteristic为空，请联系外设设备开发商确认特征值uuid是否正确");
                    return;
                }
                //根据指定特征值uuid获取指定的特征值二
                mWriteGattCharacteristic = gattService.getCharacteristic(UUID.fromString(mWriteCharacteristicUUID));
                if (mWriteGattCharacteristic == null) {
                    Log.e(TAG, "onServicesDiscovered-->" + "获取指定特征值的uuid：" + mReadCharacteristicUUID + "的BluetoothGattCharacteristic为空，请联系外设设备开发商确认特征值uuid是否正确");
                    return;
                }
                //设置特征值通知,即设备的值有变化时会通知该特征值，即回调方法onCharacteristicChanged会有该通知
                mBluetoothGatt.setCharacteristicNotification(mReadGattCharacteristic, true);
                //获取特征值其对应的通知Descriptor
                BluetoothGattDescriptor descriptor = mReadGattCharacteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                //写入你需要传递给外设的特征的描述值（即传递给外设的信息）
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                //通过GATT实体类将，特征值写入到外设中。在 onDescriptorWrite 回调里面发送握手
                boolean isSuccessWriteDescriptor = mBluetoothGatt.writeDescriptor(descriptor);
                if (!isSuccessWriteDescriptor) {
                    Log.e(TAG, "onServicesDiscovered-->" + "bluetoothGatt将特征值BluetoothGattDescriptor写入外设失败");
                }
                //通过Gatt对象读取特定特征（Characteristic）的特征值。从外设读取特征值，这个可有可无，一般远程设备的硬件工程师可能不会给该权限
                boolean isSuccessReadCharacteristic = mBluetoothGatt.readCharacteristic(mReadGattCharacteristic);
                if (!isSuccessReadCharacteristic) {
                    Log.e(TAG, "onServicesDiscovered-->" + "读取外设返回的值的操作失败,无法回调onCharacteristicRead，多半硬件工程师的问题或者没给权限");
                }
                return;
            }
            Log.e(TAG, "onServicesDiscovered-->" + "status:" + status + "操作失败");
            if (status == 133) {//需要清除Gatt缓存并断开连接和关闭Gatt，然后重新连接
                gattError133("onServicesDiscovered");
            }

        }

        //接收到的数据，不一定会回调该方法
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            Log.d(TAG, "onCharacteristicRead-->" + characteristic.getValue().toString());
        }

        //发送数据后的回调，可以在此检测发送的数据包是否有异常
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "onCharacteristicWrite:发送数据成功：" + binaryToHexString(characteristic.getValue()));
            } else Log.e(TAG, "onCharacteristicWrite:发送数据失败");
        }

        //设备的值有变化时会主动返回
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            Log.d(TAG, "onCharacteristicChanged-->" + characteristic.getUuid());
            //过滤，判断是否是目标特征值
            if (!mReadCharacteristicUUID.equals(characteristic.getUuid().toString())) return;
            if (bleCallback != null)
                bleCallback.getDeviceReturnData(characteristic.getValue());
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            Log.d(TAG, "onDescriptorRead-->" + "status:" + status + descriptor.getUuid());
        }

        //设置Descriptor后回调
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "onDescriptorWrite-->" + "描述符写入操作成功，蓝牙连接成功并可以通信成功！！！" + descriptor.getUuid());
                if (bleCallback != null)
                    bleCallback.connectSuccess();
            } else {
                Log.e(TAG, "onDescriptorWrite-->" + "描述符写入操作失败，蓝牙通信失败...");
            }
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
            Log.d(TAG, "onReliableWriteCompleted");
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
            Log.d(TAG, "onReadRemoteRssi");
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            Log.d(TAG, "onMtuChanged");
        }
    };

    //Gatt操作失败status为133时
    private void gattError133(String method) {
        Log.e(TAG, "BluetoothGattCallback：" + method + "--> 因status=133，所以将关闭Gatt重新连接...");
        disConnect(true);//断开连接并关闭Gatt
        if (isDirectConnect()) {
            Log.d(TAG, "此次为MAC地址直连");
            workHandler.sendEmptyMessage(CONNECT_GATT);
        } else {
            Log.d(TAG, "此次为蓝牙扫描连接");
            workHandler.sendEmptyMessage(START_SCAN);
        }
    }

    /**
     * 清理本地的BluetoothGatt 的缓存，以保证在蓝牙连接设备的时候，设备的服务、特征是最新的
     */
    private boolean refreshDeviceCache(BluetoothGatt gatt) {
        Method refreshtMethod = null;
        if (null != gatt) {
            try {
                for (Method methodSub : gatt.getClass().getDeclaredMethods()) {
                    if ("connect".equalsIgnoreCase(methodSub.getName())) {
                        Class<?>[] types = methodSub.getParameterTypes();
                        if (types.length > 0) {
                            if ("int".equalsIgnoreCase(types[0].getName())) {
                                refreshtMethod = methodSub;
                            }
                        }
                    }
                }
                if (refreshtMethod != null) {
                    refreshtMethod.invoke(gatt);
                }
                Log.d(TAG, "refreshDeviceCache-->" + "清理本地的BluetoothGatt 的缓存成功");
                return true;
            } catch (Exception localException) {
                localException.printStackTrace();
            }
        }
        Log.e(TAG, "refreshDeviceCache-->" + "清理本地清理本地的BluetoothGatt缓存失败");
        return false;
    }


    /**
     * 获取BluetoothAdapter，使用默认获取方式。无论如何都不会为空
     */
    private BluetoothAdapter getBluetoothAdapter() {
        return BluetoothAdapter.getDefaultAdapter();//用默认的
    }

    /**
     * 定位权限
     */
    private void permissionLocation() {
        if (context == null) return;
        XXPermissions.with(context)
                .permission(Permission.ACCESS_FINE_LOCATION)
                .permission(Permission.BLUETOOTH_ADVERTISE)
                .permission(Permission.BLUETOOTH_CONNECT)
                .request(new OnPermissionCallback() {
                    @Override
                    public void onGranted(@NonNull List<String> permissions, boolean allGranted) {
                        if (allGranted) {
                            //申请的定位权限允许
                            //设置整个连接过程超时时间
                            workHandler.sendEmptyMessageDelayed(LINK_TIME_OUT, linkTime);
                            //如果可以Mac直连则不扫描
//                            if (isDirectConnect()) {
//                                Log.d(TAG, "此次为MAC地址直连");
//                                workHandler.sendEmptyMessage(CONNECT_GATT);
//                            } else {
                            Log.d(TAG, "此次为蓝牙扫描连接");
                            workHandler.sendEmptyMessage(START_SCAN);
//                            }
                        } else {
                            //只要有一个权限被拒绝，就会执行
                            Log.d(TAG, "未授权定位权限，蓝牙功能不能使用：");
                            Toast.makeText(context, "未授权定位权限，蓝牙功能不能使用", Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onDenied(List<String> permissions, boolean doNotAskAgain) {
                        if (doNotAskAgain) {
                            Toast.makeText(context, "被永久拒绝授权，请手动授予录音和日历权限", Toast.LENGTH_SHORT).show();
                            // 如果是被永久拒绝就跳转到应用权限系统设置页面
                            XXPermissions.startPermissionActivity(context, permissions);
                        } else {
                            Toast.makeText(context, "获取录音和日历权限失败", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

    }

    /**
     * 判断GPS是否开启，GPS或者AGPS开启一个就认为是开启的
     *
     * @return true 表示开启
     */
    private boolean isOPenGps() {
        LocationManager locationManager
                = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        // 通过GPS卫星定位，定位级别可以精确到街（通过24颗卫星定位，在室外和空旷的地方定位准确、速度快）
        boolean gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        // 通过WLAN或移动网络(3G/2G)确定的位置（也称作AGPS，辅助GPS定位。主要用于在室内或遮盖物（建筑群或茂密的深林等）密集的地方定位）
        boolean network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (gps || network) {
            Log.d(TAG, "GPS状态：打开");
            return true;
        }
        Log.e(TAG, "GPS状态：关闭");
        return false;
    }

    /**
     * 睡一下：1.停止扫描时需要调用；2.发送特征值给外设时需要有一定的间隔
     */
    private void sleep() {
        try {
            Thread.sleep(100);//延时100ms
        } catch (InterruptedException e) {
            e.printStackTrace();
            Log.i("测试", "延迟异常");
        }
    }

    /**
     * @param bytes
     * @return 将二进制转换为十六进制字符输出
     * new byte[]{0b01111111}-->"7F" ;  new byte[]{0x2F}-->"2F"
     */
    private static String binaryToHexString(byte[] bytes) {
        String result = "";
        if (bytes == null) {
            return result;
        }
        String hex = "";
        for (int i = 0; i < bytes.length; i++) {
            //字节高4位
            hex = String.valueOf("0123456789ABCDEF".charAt((bytes[i] & 0xF0) >> 4));
            //字节低4位
            hex += String.valueOf("0123456789ABCDEF".charAt(bytes[i] & 0x0F));
            result += hex + ",";
        }
        return result;
    }

    public interface BleCallback {
        void connectSuccess();//连接成功

        void getDeviceReturnData(byte[] data);

        void error(int e);
    }

    public void connectBle(String bleName) {
        if (souDevice(bleName) == null) return;
        BluetoothDevice device = souDevice(bleName);
        if (mMacAdress != null && mMacAdress.equals(device.getAddress())) {
            workHandler.removeMessages(LINK_TIME_OUT);
            workHandler.sendEmptyMessage(STOP_SCAN);
            Log.d(TAG, "LeScanCallback-->" + "蓝牙扫描已找到设备，即将开始连接");
            mBluetoothDevice = null;
            mBluetoothDevice = device;
            workHandler.sendEmptyMessage(CONNECT_GATT);
            sleep();
        }
    }

    @SuppressLint("MissingPermission")
    private BluetoothDevice souDevice(String name) {
        for (BluetoothDevice device : deviceS) {
            if (device.getName().equals(name)) return device;
        }
        return null;
    }

    //发送到unity
    public void sendUnity(String bluetoothDeviceNamed) {
        // 使用 UnityPlayer 类获取 Unity 的当前活动，并调用其静态方法 UnitySendMessage
        try {
            Class<?> unityPlayerClass = Class.forName("com.unity3d.player.UnityPlayer");
            Object unityPlayer = unityPlayerClass.getField("currentActivity").get(null);
            String unityObjectName = "Canvas"; // Unity 中的对象名称
            String unityMethodName = "OnReceiveMessage"; // Unity 中的方法名称
            String unityMessage = bluetoothDeviceNamed; // 要传递的消息

            // 调用 Unity 的静态方法 UnitySendMessage，传递对象名称、方法名称和消息内容
            unityPlayerClass.getMethod("UnitySendMessage", String.class, String.class, String.class).invoke(unityPlayer, unityObjectName, unityMethodName, unityMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
//        if (result.getDevice().getName().equals("LeesonIPHONEXS")) {
//            Toast.makeText(mContext, "stopScanBluetooth", Toast.LENGTH_SHORT).show();
//            stopScanBluetooth();
//        }

    }

}