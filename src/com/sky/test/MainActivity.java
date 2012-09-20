package com.sky.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
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
				//new Thread(new CheckOMRONTask()).start();
				new CheckOMRONTask().execute("");
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
		intentFilter.addAction("android.bluetooth.device.action.PAIRING_REQUEST");
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
	
	private class RecvDataTask implements Runnable  {
			
		private BluetoothServerSocket bluetoothServerSocket = null;
				
		private BluetoothSocket bluetoothRecvSocket = null;
		
		private byte[] recvBufer = new byte[1024];
		
		private boolean flag = true;
		
		public RecvDataTask() {
			
		}
		
		@Override
		public void run() {
			try {
				bluetoothServerSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord("OMRON", SPP_UUID);
				//Method listener = BluetoothAdapter.class.getMethod("listenUsingInsecureRfcommWithServiceRecord",String.class, UUID.class);
				//bluetoothServerSocket = (BluetoothServerSocket)listener.invoke(bluetoothAdapter, bluetoothAdapter.getName(), SPP_UUID);
				InputStream is = null;
				while(flag) {
					Log.d(TAG, "### wait for connect.");
					sb.append("### wait for connect\n");
					handler.sendEmptyMessage(update_info);
					bluetoothRecvSocket = bluetoothServerSocket.accept();
					Log.d(TAG, "### connected");
					sb.append("### connected\n");
					handler.sendEmptyMessage(update_info);
					is = bluetoothRecvSocket.getInputStream();
					int recvLength = is.read(recvBufer);
					String re = new String(recvBufer, 0, recvLength);
					if("READY".equalsIgnoreCase(re)) {
						Log.d(TAG, "### rece from device = " + re);
						sb.append("### rece from device = " + re + "\n");
						handler.sendEmptyMessage(update_info);
						flag = false;
					}
				}
				int i = 0;
				byte[] sendCmd = new byte[]{0x47,0x4d,0x44,0,(byte)(i>>8),(byte)i,(byte)((i>>8)^i)};
				OutputStream os = bluetoothRecvSocket.getOutputStream();
				os.write(sendCmd);
				Thread.sleep(500);
				int rl = -1;
				while((rl = is.read(recvBufer)) != -1) {
					String recv = new String(recvBufer, 0, rl);
					sb.append("### " + recv +  "\n");
					handler.sendEmptyMessage(update_info);
					Log.d(TAG, recv);
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
		}
	}
		
/*	private class CheckOMRONTask implements Runnable  {
		
		public CheckOMRONTask() {
			
		}
		
		@Override
		public void run() {
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
		}
	}*/

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
			byte[] recv = new byte[100];
			InputStream is = bluetoothSocket.getInputStream();
			int readLength = is.read(recv);
			String recvStr = new String(recv,0,readLength);
			sb.append("### " + device.getName() + " "  + recvStr + "\n");
			handler.sendEmptyMessage(update_info);
			if("READY".equalsIgnoreCase(recvStr)) {
				byte[] sendCmd = new byte[]{0x54,0x4f,0x4b,(byte)0xff,(byte)0xff};
				OutputStream os = bluetoothSocket.getOutputStream();
				os.write(sendCmd);
				os.flush();
				Thread.sleep(500);
				readLength = is.read(recv);
				if(recv[0] == 'O' && recv[1] == 'K') {
					sb.append("### 配对成功！ " + new String(recv,0, readLength)+"\n");
					handler.sendEmptyMessage(update_info);
					Log.d(TAG, "### 配对成功！\n");
					// TODO
					//ClsUtils.printAllInform(device.getClass());
					new Thread(new RecvDataTask()).start();
				}else {
					sb.append("### 配对失败！ ");
					handler.sendEmptyMessage(update_info);
					Log.d(TAG, "### 配对失败！");
				}
			}

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
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
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
			} else if("android.bluetooth.device.action.PAIRING_REQUEST".equals(intent.getAction())) {
				BluetoothDevice d = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE); 
				 try {
					 boolean setPin = ClsUtils.setPin(d.getClass(), d, "0000");
					 Log.d(TAG, "### setPin = " + setPin);
					 boolean createBoud = ClsUtils.createBond(d.getClass(), d);
					 Log.d(TAG, "### createBoud = " + createBoud);
					 device = d;
					 boolean canclePare = ClsUtils.cancelPairingUserInput(d.getClass(), d);
					 Log.d(TAG, "### canclePare = " + canclePare);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} // 手机和蓝牙采集器配对

			}else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(intent
					.getAction())) {
				sb.append("### start discovery...\n");
				textView.setText(sb.toString());
			} else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(intent
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
					//connect();
					break;
				case BluetoothDevice.BOND_NONE:
					sb.append("### cancle pair!\n");
					textView.setText(sb.toString());
					Log.d(TAG, "配对取消。");
					break;
				default:
					break;
				}
			} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(intent
					.getAction())) {
				sb.append("### start discovery  over...\n");
				textView.setText(sb.toString());
				sb.delete(0, sb.length());
				textView.setText(sb.toString());
				if (loopSearchFlag) {
					bluetoothAdapter.cancelDiscovery();
					handler.postDelayed(DiscoveryTask, 10000);
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
