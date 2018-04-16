package com.example.hang.client;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    Socket socket;
    String SOCKET_HOST = "192.168.3.50";
    DataOutputStream out;
    DataInputStream in;

    TextView tv_status;
    BlockingDeque<Integer> queue;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> RSSIStrengthHandler;
    final Runnable fetchRSSI = new Runnable() {
        @Override
        public void run() {
            int strength = getSignalStrength();
            queue.add(strength);

            Message msg = Message.obtain();
            msg.what = 0;
            msg.obj = String.valueOf(strength);
            myHandler.sendMessage(msg);
        }
    };
    final Runnable stopFetchRSSI = new Runnable() {
        @Override
        public void run() {
            RSSIStrengthHandler.cancel(true);
            myHandler.sendEmptyMessage(1);
        }
    };

    Handler myHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message inputMessage) {
            // Gets the image task from the incoming Message object.
            if (inputMessage.what == 0) {
                String str = inputMessage.obj.toString();
                tv_status.setText("RSSI="+str);
            } else {
                tv_status.setText("");
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv_status = (TextView) findViewById(R.id.tv_status);
        queue = new LinkedBlockingDeque<>();

        //create socket connection
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    socket = new Socket(SOCKET_HOST, 12345);
                    out = new DataOutputStream(socket.getOutputStream());
                    in = new DataInputStream(socket.getInputStream());
                } catch (UnknownHostException e) {
                    System.out.println("Unknown host: " + SOCKET_HOST);
                    System.exit(1);
                } catch (IOException e) {
                    System.out.println("No I/O");
                    System.exit(1);
                }
            }
        };
        Thread connectThread = new Thread(runnable);
        connectThread.start();
        try {
            connectThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Runnable readTask = new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        String message = in.readUTF();
                        System.out.println("Get command from Server : " + message);
                        if (message.equals("1")) {
                            RSSIStrengthHandler = scheduler.scheduleAtFixedRate(fetchRSSI, 0, 1, TimeUnit.SECONDS);
                        } else {
                            scheduler.schedule(stopFetchRSSI, 0, TimeUnit.SECONDS);
                        }
                    } catch (IOException e) {
                        System.out.println("Read failed");
                        System.exit(-1);
                    }
                }
            }
        };
        new Thread(readTask).start();


        Runnable writeTask = new Runnable() {
            @Override
            public void run() {
                while (true) {
                    Integer RSSI = null;
                    try {
                        RSSI = queue.poll(300, TimeUnit.MILLISECONDS);
                        if (RSSI != null) {
                            System.out.println("writeTask: strength = " + RSSI);
                            out.writeUTF(String.valueOf(RSSI));
                            out.flush();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }
        };
        new Thread(writeTask).start();
    }



    private int getSignalStrength() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiManager.startScan();
        List<ScanResult> wifiList = wifiManager.getScanResults();
        if (wifiList.size() == 0) {
            return 100;
        }

        for (int i = 0; i < wifiList.size(); i++) {
            ScanResult scanResult = wifiList.get(i);
            String macAddress = scanResult.BSSID;
            if (macAddress.equals("d0:ff:98:81:46:f8")) {
                return scanResult.level;
            }
        }
        return 101;
    }
}
