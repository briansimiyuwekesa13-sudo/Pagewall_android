package com.pagewall.app;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.view.Gravity;
import android.graphics.Color;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(30, 30, 30, 30);
        layout.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("PAGEWALL");
        title.setTextSize(32);
        title.setTextColor(Color.BLACK);

        TextView message = new TextView(this);
        message.setText("Your personal digital library.");
        message.setTextSize(18);
        message.setTextColor(Color.DKGRAY);

        Button button = new Button(this);
        button.setText("PAGEWALL IS READY");

        layout.addView(title);
        layout.addView(message);
        layout.addView(button);

        setContentView(layout);
    }
}
