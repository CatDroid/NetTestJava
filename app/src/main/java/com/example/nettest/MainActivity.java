package com.example.nettest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity  {

	protected static final String TAG = "nettest";

	private final static int RTMP_CMD_PORT = 	19999 ;	 // for 广播/UDP  收发rtmp命令/TCP
	
	private final static String RTMP_CMD_IP_UNKNOWN = "unknown" ;
	private static String MyRtmpCmdIP = RTMP_CMD_IP_UNKNOWN ;
	
	final static public int RTMP_STATUS_CLOSED  = 0 ;
	final static public int RTMP_STATUS_OPENING = 1;
	final static public int RTMP_STATUS_OPENED  = 2 ;
	final static public int RTMP_STATUS_CLOSING = 3 ;
	
	class UIHandler extends Handler
	{
		static final public int  RTMP_STATUS = 0 ;
		static final public int  RTMP_IP = 1 ;
		static final public int  ERROR = 100 ;
		@Override
		public void handleMessage(Message msg) {
		
			switch( msg.what){
				case RTMP_STATUS:
					if( msg.arg1 == -1){
						mRtmpViewStatus.setText("Error");
					}else{
						switch(msg.arg1){
						case RTMP_STATUS_CLOSED :
							mRtmpViewStatus.setText("Closed");
							break;
						case RTMP_STATUS_OPENING:
							mRtmpViewStatus.setText("opening");
							break;
						case RTMP_STATUS_OPENED:
							mRtmpViewStatus.setText("Opened");
							break;
						case RTMP_STATUS_CLOSING:
							mRtmpViewStatus.setText("Closing");
							break;
						default:
							mRtmpViewStatus.setText("Unknown");
							break;
						}
					}
					break;
				case RTMP_IP:
					if( msg.arg1 == -1){
						mRtmpIPAddr.setText("ERROR");
					}else if( msg.arg1 == 1 ){
						mRtmpIPAddr.setText(mRtmpAddr.getHostString());
					}
					break;
				case ERROR:
					Toast.makeText(MainActivity.this, "Err: " + (String)msg.obj, Toast.LENGTH_LONG).show(); 
					break;
				default:
					Log.e(TAG, "UIHandler unknown msg " + msg.arg1  );
					break;
			}
			super.handleMessage(msg);
		}
		
	}
	
	private UIHandler mUIHandler = new UIHandler();
 
	class MyHandler extends Handler
	{
		static final public int  MSG_OPEN  = 1 ;
		static final public int  MSG_CLOSE  = 2 ;
		static final public int  MSG_STATUS  = 3 ;
		
			
		public MyHandler(Looper looper) {
			super(looper);
			// TODO Auto-generated constructor stub
		}

		@Override
		public void handleMessage(Message msg) {
			
			if( mRtmpAddr == null){
				Message uimsg = mUIHandler.obtainMessage(UIHandler.ERROR);
				uimsg.obj = "IP Addr is NULL";
				mUIHandler.sendMessage(uimsg);
				return ;
			}
			
			switch(msg.what){
			case MSG_OPEN:
				try {
					Socket socket;
					socket = new Socket(mRtmpAddr.getHostString(),RTMP_CMD_PORT);
					OutputStream outputStream = socket.getOutputStream();
					String cmd = MSG_OPEN + "\n";
					outputStream.write(cmd.getBytes());
					socket.close();
				
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			case MSG_CLOSE:
				
				try {
					Socket socket = new Socket(mRtmpAddr.getHostString(),RTMP_CMD_PORT);
					OutputStream outputStream = socket.getOutputStream();
					String cmd = MSG_CLOSE + "\n";
					outputStream.write(cmd.getBytes());
					socket.close();	 
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				break;
			case MSG_STATUS:
				try {
					Socket socket = new Socket(mRtmpAddr.getHostString(),RTMP_CMD_PORT);
					OutputStream outputStream = socket.getOutputStream();
					String cmd = MSG_STATUS + "\n";
					outputStream.write(cmd.getBytes());
					
					socket.setSoTimeout(2000);
					InputStream is = socket.getInputStream() ; 
					byte[] rdBuffer = new byte[1];
					int ret = is.read(rdBuffer);
					if( ret >= 1 ){
						Message uimsg = mUIHandler.obtainMessage(UIHandler.RTMP_STATUS);
						uimsg.arg1 = rdBuffer[0] ; 
						mUIHandler.sendMessage(uimsg);
					}else{
						Message uimsg = mUIHandler.obtainMessage(UIHandler.RTMP_STATUS);
						uimsg.arg1 = -1 ; 
						mUIHandler.sendMessage(uimsg);
					}
					
					socket.close();	 
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				break;
			default:
				Log.e(TAG, "unknow message " + msg.what);
				break;
			}
			super.handleMessage(msg);
		}
	
	}
	
	private Handler mRtmpHandler = null;
	private TextView mRtmpViewStatus = null;
	private TextView mRtmpIPAddr = null;
	private InetSocketAddress mRtmpAddr = null;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		
		HandlerThread thread = new HandlerThread("rtmp");
		thread.start();
		mRtmpHandler = new MyHandler(thread.getLooper());
		
		mRtmpViewStatus = (TextView)findViewById(R.id.txStatus);
		mRtmpIPAddr = (TextView)findViewById(R.id.txIPAddr);

		((Button)findViewById(R.id.bOpen)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mRtmpHandler.sendEmptyMessage(MyHandler.MSG_OPEN);
			}
			
		});
		
		((Button)findViewById(R.id.bClose)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mRtmpHandler.sendEmptyMessage(MyHandler.MSG_CLOSE);
			}
			
		});
		
		((Button)findViewById(R.id.bStatus)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				
				mRtmpViewStatus.setText("wait...");
				mRtmpHandler.sendEmptyMessage(MyHandler.MSG_STATUS);
			}
			
		});
		
	
		
		((Button)findViewById(R.id.bSendIPAddr)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				
				new Thread(new Runnable(){

					@Override
					public void run() {
						DatagramSocket broadsocket;
						try {
							broadsocket = new DatagramSocket();
							broadsocket.setBroadcast(true);
							// 一般在发送UDP数据报的时候，希望该socket发送的数据具有广播特性 允许套接口传送广播信息
							// 如果不设置这个,就不能发送广播  sendto error: Permission denied 即使在root用户运行
							// 在没有 SO_BROADCAST 标识设置的情况下发送一个包到广播地址  EACCES
							
							byte[] buffer = "Hello World".getBytes();
							DatagramPacket packet = new DatagramPacket(buffer,buffer.length);
							
							InetAddress boardaddr = null; 
							// ENETUNREACH (Network is unreachable)
							// 如果手机本身作为热点  不能发送255.255.255.255 受限制的广播地址
							//addr = InetAddress.getByAddress( new byte[]{(byte)192 ,(byte)168 , (byte)43 , (byte)255} );
							
							// 单播(Unicast) 多播(Multicast)  广播 (Broadcast) 
							NetworkInterface ni =  NetworkInterface.getByName("wlan0"); // rmnet_data0

							// 获得某个网卡上所有地址信息  一个网卡可以有多个IP地址
							List<InterfaceAddress> addresslist  = ni.getInterfaceAddresses();
							Iterator<InterfaceAddress> it = addresslist.iterator();

							while (it.hasNext()) {
								InterfaceAddress ia = it.next();
								Log.d(TAG,"Address = " + ia.getAddress());
								Log.d(TAG,"Broadcast = " + ia.getBroadcast());
								Log.d(TAG,"Network prefix length = " + ia.getNetworkPrefixLength());
								Log.d(TAG," ");
								/*
								 	Address = /fe80::d648:dbff:fe32:8bf4%wlan0%75  // IPv6地址
									Broadcast = null
									Network prefix length = 64
									
									Address = /192.168.43.1
									Broadcast = /192.168.43.255					    // IPv4地址
									Network prefix length = 24
								 */
								
								if( ia.getBroadcast() != null ){
									boardaddr = ia.getBroadcast() ;
								}
								
							}// 先获取网卡的广播地址
							 
							if( boardaddr == null){
								Log.e(TAG, "Can NOT found Boardcast Address");
								return ;
							}
					 
							
							Log.d(TAG, "boardcast addr " + boardaddr.getHostAddress() );
							
							packet.setPort(RTMP_CMD_PORT);
							packet.setAddress(boardaddr);
							// 发出广播包  地址是广播地址  端口是 RTMP_CMD_PORT 数据是 "Hello World"
							
							int try_time = 5 ;
							while( try_time-- != 0 ){
								try {
									broadsocket.send(packet);
								} catch (IOException e) {
									Log.e(TAG, "send IOException " + e.getMessage() );
									e.printStackTrace();
								}
								try {
									Thread.sleep(1000);
								} catch (InterruptedException e) {
									Log.e(TAG, "sleep InterruptedException " + e.getMessage() );
									e.printStackTrace();
								}
								Log.d(TAG, "send!");
							}
						 
						} catch (SocketException e2) {
							Log.e(TAG, "SocketException " + e2.getMessage() );
							e2.printStackTrace();
						} 	
					}
				}).start();	
				
			}
		});
		
		((Button)findViewById(R.id.bGetIPAddr)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				
				mRtmpIPAddr.setText("wait...");
				
				new Thread(new Runnable(){

					@Override
					public void run() {
						
						
						try {
							
							Log.d(TAG,"TRY TO GET IP ADDR ERROR"); 
							
							ByteBuffer rddata = ByteBuffer.allocateDirect(1300);
							
							DatagramChannel channel = DatagramChannel.open();
							channel.configureBlocking(true);
							DatagramSocket socket = channel.socket();
							socket.setReuseAddress(true);
							socket.bind(new InetSocketAddress(RTMP_CMD_PORT));
							socket.setSoTimeout(6000); // 
							
							SocketAddress sa = channel.receive(rddata);
							if( sa == null){
								mRtmpAddr = null;
								Message uimsg = mUIHandler.obtainMessage( UIHandler.RTMP_IP );
								uimsg.arg1 = -1 ;
								mUIHandler.sendMessage(uimsg);
								Log.d(TAG,"GET IP ADDR ERROR");
							}else{
								mRtmpAddr = (InetSocketAddress)sa ;
								Message uimsg = mUIHandler.obtainMessage( UIHandler.RTMP_IP );
								uimsg.arg1 = 1 ;
								mUIHandler.sendMessage(uimsg);
								Log.d(TAG, "sa = " + mRtmpAddr.getPort() + " " + mRtmpAddr.getHostString()  );
							}
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}).start(); 
				
			}
			
		});
		
		
		
		
		
		((Button)findViewById(R.id.bSend)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d(TAG, "onClick 1");
				new Thread(new Runnable(){

					@Override
					public void run() {
						
						Log.d(TAG, "enter ");
						try { 
							DatagramChannel channel;
							
							channel = DatagramChannel.open();
						
							channel.configureBlocking(true);
							DatagramSocket socket = channel.socket();
							
							socket.setReuseAddress(true);
							socket.bind(new InetSocketAddress(9999));
							int old1 = socket.getSendBufferSize();
							//socket.setSendBufferSize(512*1024);
							int new1 = socket.getSendBufferSize();
							Log.d(TAG, " " + old1 + " --> " + new1 );
				  
							Log.d(TAG, "go to listen  send sleep 1ms ");
							ByteBuffer rddata = ByteBuffer.allocateDirect(1300);
							ByteBuffer snddata = ByteBuffer.allocateDirect(1300);
							SocketAddress sa = channel.receive(rddata);
							InetSocketAddress inetaddr = (InetSocketAddress)sa ; 
							Log.d(TAG, "sa = " + inetaddr.getPort() + " " + inetaddr.getHostString()  );
							int video_frame_index = 0 ;
							while( true ){
								snddata.put(0, (byte) ( video_frame_index & 0x000000FF  )        );
								snddata.put(1, (byte) ( (video_frame_index & 0x0000FF00 ) >> 8 ) );
								snddata.put(2, (byte) ( (video_frame_index & 0x00FF0000 ) >> 16) );
								snddata.put(3, (byte) ( (video_frame_index & 0xFF000000 ) >> 24) );
					 
								snddata.mark(); channel.send(snddata, sa); snddata.reset();
								
								Thread.sleep(1);
								
								video_frame_index++; 
							}
							
							//channel.close();
						} catch (IOException e) {
							Log.d(TAG, "IOException " + e.getMessage() );
							e.printStackTrace();
						} 
						catch (InterruptedException e) {
							Log.d(TAG, "InterruptedException " + e.getMessage() );
							e.printStackTrace();
						}
						
						Log.d(TAG, "exit ");
					}
					
				}).start();
				
			}
		});
		 

		((Button)findViewById(R.id.bRecv)).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				new Thread(new Runnable(){

					@Override
					public void run() {
						
						Log.d(TAG, "enter ");
						try {
							DatagramChannel channel;
							
							Selector selector = Selector.open(); 
							
							channel = DatagramChannel.open();
							channel.configureBlocking(false);

							//channel.socket().bind(new InetSocketAddress());
							channel.connect(new InetSocketAddress("192.168.43.1", 9999));
							Log.d(TAG, "connect done ");
							// channel.socket().setReuseAddress(true);
							channel.write(ByteBuffer.wrap(new String("aaaa").getBytes()));  
							channel.register(selector, SelectionKey.OP_READ);  
							Log.d(TAG, "register done ");
							ByteBuffer rndbuf = ByteBuffer.allocateDirect(1300);
							rndbuf.order(ByteOrder.nativeOrder());
							IntBuffer readbuf = rndbuf.asIntBuffer();
							int last_num = 0 ;
					        while (selector.select() > 0) {   
					            Iterator iterator = selector.selectedKeys().iterator();  
					            while (iterator.hasNext()) {  
					                SelectionKey key = null;  
					                 
				                    key = (SelectionKey) iterator.next();  
				                    iterator.remove();  
				  
				                    if (key.isReadable()) {  
				                        DatagramChannel sc = (DatagramChannel) key.channel();  
				                        
				                        rndbuf.clear();
				                        SocketAddress address = sc.receive(rndbuf);
				                        rndbuf.flip();
				                        
				                        int cur = readbuf.get(0) ;
				                        if( cur != last_num + 1 ){
				                        	Log.d(TAG, "last  " + last_num + " cur " + cur   ); 
				                        }
				                        last_num = cur ;
//				                        Byte byte1 = rndbuf.get(0);
//				                        Byte byte2 = rndbuf.get(1);
//				                        Byte byte3 = rndbuf.get(2);
//				                        Byte byte4 = rndbuf.get(3);
				                        
				                        
				                    }  
				                    if (key.isWritable()) {  
				                       
				                    }
					               
					            }
					            
//						        Log.e(TAG, "IOException e " + e.getMessage() );
//			                	e.printStackTrace();
//			                    try {
//			                    	
//			                     
//			                        if (key != null) {
//			                        	Log.d(TAG, "close Channel" );
//			                            key.cancel();
//			                            key.channel().close();
//			                        }
//			                    } catch (ClosedChannelException cex) {
//			                    	Log.e(TAG, "IOException e " + e.getMessage() );
//			                        e.printStackTrace();
//			                    }   
					        }
					        Log.d(TAG, "exit ");
						} catch (IOException e) {
							Log.d(TAG, "IOException " + e.getMessage() );
							e.printStackTrace();
						}
					}
				}  ).start(); 
			}
		});
		
	}

 
}
