package com.gldz.gldrone;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import android.view.View;
import android.view.WindowManager;
import android.widget.Button;


public class MainActivity extends AppCompatActivity {


    Mavlink mavlink;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // KEEP_SCREEN_ON
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mavlink = new Mavlink();

        Button btTest1 = (Button) findViewById(R.id.bt_send1);
        btTest1.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                mavlink.sendMsgSetMode(1,0);
            }
        });

        Button btTest2 = (Button) findViewById(R.id.bt_send2);
        btTest2.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                mavlink.sendMsgSetMode(7,0);
            }
        });
    }

    public static String printHexString(byte[] b) {
        String res = "";
        for (int i = 0; i < b.length; i++) {
            String hex = Integer.toHexString(b[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            res += hex;
            res += " ";
        }
        return res;
    }
}