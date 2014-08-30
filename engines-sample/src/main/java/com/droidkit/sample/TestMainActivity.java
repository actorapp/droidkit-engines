package com.droidkit.sample;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.droidkit.sample.listenginetest.TestListEngineActivity;

public class TestMainActivity extends BaseActivity {

    private LinearLayout ll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initUi();
    }

    private void initUi() {
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(Color.WHITE);
        final int p = 20;
        sv.setPadding(p, p, p, p);
        ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        sv.addView(ll);
        setContentView(sv);

        addButton("Start list engine test", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(TestMainActivity.this, TestListEngineActivity.class));
            }
        });

    }

    private void addButton(String text, View.OnClickListener clickListener) {
        final Button button = new Button(this);
        button.setText(text);
        button.setOnClickListener(clickListener);
        ll.addView(button);
    }
}