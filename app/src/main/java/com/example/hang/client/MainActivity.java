package com.example.hang.client;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.BufferedReader;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    Socket socket;
    String SOCKET_HOST = "192.168.3.50";
    DataOutputStream out;
    DataInputStream in;

    Button btn_Send;
    EditText editText;
//    Button btn_startCollect;

    Handler myHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message inputMessage) {
            // Gets the image task from the incoming Message object.
            editText.setText((String)inputMessage.obj);

        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btn_Send = (Button) findViewById(R.id.btn_send);
        editText = (EditText) findViewById(R.id.et_message);
//        btn_Send.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                String text = editText.getText().toString();
//                if (!text.equals("")) {
//                    try {
//                        out.writeUTF(text);
//                        out.flush();
//                        editText.setText(new String(""));
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        });

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
                    } catch (IOException e) {
                        System.out.println("Read failed");
                        System.exit(-1);
                    }
                }
            }
        };
        new Thread(readTask).start();
    }
}
