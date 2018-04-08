package com.example.hang.client;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class MainActivity extends AppCompatActivity {
    Socket socket;
    String SOCKET_HOST = "192.168.3.68";
    PrintWriter out;
    BufferedReader in;

    Button btn_Send;
    EditText editText;

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

        //create socket connection
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    socket = new Socket(SOCKET_HOST, 12345);
                    out = new PrintWriter(socket.getOutputStream(), true);
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                } catch (UnknownHostException e) {
                    System.out.println("Unknown host: " + SOCKET_HOST);
                    System.exit(1);
                } catch (IOException e) {
                    System.out.println("No I/O");
                    System.exit(1);
                }
            }
        };
        new Thread(runnable).start();

        final Runnable runnable1 = new Runnable() {
            @Override
            public void run() {
                //receiver text from server
                try {
                    //this line should not be on main thread
                    String line = in.readLine();
                    Message msg = Message.obtain();
                    msg.what = 0;
                    msg.obj = line;
                    myHandler.sendMessage(msg);
                    System.out.println("Text received: " + line);
                } catch (IOException e){
                    System.out.println("Read failed");
                    System.exit(1);
                }
            }
        };

        btn_Send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = editText.getText().toString();
                if (!text.equals("")) {
                    out.println(text);
                    editText.setText(new String(""));
                    new Thread(runnable1).start();
                }
            }
        });
    }
}
