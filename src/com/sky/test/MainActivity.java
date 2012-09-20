package com.sky.test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

public class MainActivity extends Activity {

	private UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

	private boolean loopSearchFlag = true;

	private BluetoothAdapter bluetoothAdapter = null;

	private BluetoothSocket bluetoothSocket = null;
	
	private BluetoothDevice device = null;

	private TextView textView = null;

	private StringBuffer sb = new StringBuffer();

	private String TAG = "MainActivity";

	private final int unpair_msg = 2012;

	private final int update_info = 2112;

	private Handler handler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case unpair_msg:
				new CheckOMRONTask().execute();
				break;
			case update_info:
				textView.setText(sb.toString());
				break;
			default:

				break;

			}
		};
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		textView = (TextView) findViewById(R.id.searchInfo);
		textView.setText("");
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
		intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
		intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
		intentFilter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
		intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
		registerReceiver(broadcastReceiver, intentFilter);
		handler.post(DiscoveryTask);
		// new CheckOMRONTask().execute("");
	}

	private Runnable DiscoveryTask = new Runnable() {

		@Override
		public void run() {
			if (bluetoothAdapter.isEnabled()) {
				sb.append("### launched.\n");
				textView.setText(sb.toString());
				if (!bluetoothAdapter.isDiscovering()) {
					bluetoothAdapter.startDiscovery();
				}
			} else {
				System.out.println("bluetoothAdapter disable ...");
			}
		}
	};

	private class CheckOMRONTask extends AsyncTask<String, Void, Void> {

		// android客户端
		// 1、查找设备
		@Override
		protected Void doInBackground(String... params) {
			unpair(device);
			sb.append("### unpair success!" + "\n");
			handler.sendEmptyMessage(update_info);
			try {
				ClsUtils.setPin(device.getClass(), device, "1234");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			boolean res = pair(device);
			Log.d(TAG, "####  pair result = " + res);
			if (res) {
				sb.append("### pair success!" + "\n");
				handler.sendEmptyMessage(update_info);
				connect();
			} else {
				sb.append("### sorry! pair fail! please try again!" + "\n");
				handler.sendEmptyMessage(update_info);
			}
			return null;
		}

	}

	public boolean pair(BluetoothDevice remoteDevice) {
		if (remoteDevice.getBondState() == BluetoothDevice.BOND_NONE) {
			Method createBond;
			try {
				createBond = BluetoothDevice.class.getMethod("createBond");
				return (Boolean) createBond.invoke(remoteDevice);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	public boolean unpair(BluetoothDevice remoteDevice) {
		if (remoteDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
			Method createBond;
			try {
				createBond = BluetoothDevice.class.getMethod("removeBond");
				return (Boolean) createBond.invoke(remoteDevice);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	private void connect() {
		/*try {
			bluetoothSocket = device.createRfcommSocketToServiceRecord(SPP_UUID);
			Log.d(TAG, "开始连接...");  
			bluetoothSocket.connect();
			byte[] recv = new byte[5];
			InputStream is = bluetoothSocket.getInputStream();
			int readLength = is.read(recv);
			Log.d(TAG, "连接结果" +  new String(recv,0,readLength));  
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		
		Method client;
		try {
			client = BluetoothDevice.class.getMethod("createRfcommSocketToServiceRecord",UUID.class);//createRfcommSocketToServiceRecord
			bluetoothSocket = (BluetoothSocket)client.invoke(device, SPP_UUID);
			bluetoothSocket.connect();
			byte[] recv = new byte[5];
			InputStream is = bluetoothSocket.getInputStream();
			int readLength = is.read(recv);
			System.out.println("connect result =" +  new String(recv,0,readLength));
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			System.out.println("### " + intent.getAction());
			if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
				device = intent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				Log.d(TAG, "device = " + device.describeContents());
				Log.d(TAG, "device = " + device.getName());
				if ("HEM-7081-IT".equalsIgnoreCase(device.getName())) { // HEM-7081-IT
					loopSearchFlag = false;
					bluetoothAdapter.cancelDiscovery();
					handler.removeCallbacks(DiscoveryTask);
					sb.append("###  device found\n");
					sb.append(device.getName() + "\n");
					sb.append("### found over!" + "\n");
					textView.setText(sb.toString());
					sb.append("### unpair..." + "\n");
					textView.setText(sb.toString());
					handler.sendEmptyMessage(unpair_msg);
					// pair(bluetoothDevice);
					setTitle(device.getName());
					return;
				} else {
					setTitle("");
				}
			}
			if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(intent
					.getAction())) {
				sb.append("### start discovery...\n");
				textView.setText(sb.toString());
			}
			if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(intent
					.getAction())) {
				device = intent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				switch (device.getBondState()) {
				case BluetoothDevice.BOND_BONDING:
					sb.append("### pairing, please wait...\n");
					textView.setText(sb.toString());
					Log.d(TAG, "正在配对。。。");
					break;
				case BluetoothDevice.BOND_BONDED:
					sb.append("### paired!");
					textView.setText(sb.toString());
					Log.d(TAG, "配对完成。");
					break;
				case BluetoothDevice.BOND_NONE:
					sb.append("### cancle pair!\n");
					textView.setText(sb.toString());
					Log.d(TAG, "配对取消。");
					break;
				default:
					break;
				}
			}
			if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(intent
					.getAction())) {
				sb.append("### start discovery  over...\n");
				textView.setText(sb.toString());
				sb.delete(0, sb.length());
				textView.setText(sb.toString());
				if (loopSearchFlag) {
					bluetoothAdapter.cancelDiscovery();
					handler.postDelayed(DiscoveryTask, 1000);
				}
			}
		}
	};

	protected void onDestroy() {
		super.onDestroy();
		if (bluetoothAdapter.isDiscovering()) {
			bluetoothAdapter.cancelDiscovery();
			System.out.println("exit");
		}
		unregisterReceiver(broadcastReceiver);
	}
}
