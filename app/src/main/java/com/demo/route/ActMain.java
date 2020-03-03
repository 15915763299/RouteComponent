package com.demo.route;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.demo.annotation.Router;
import com.demo.core.core.JumpRouter;

//@PoetTest
@Router(path = "/app/main")
public class ActMain extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn1).setOnClickListener((View v) ->
                JumpRouter.get().jump("/module1/login").navigation(this)
        );

        findViewById(R.id.btn2).setOnClickListener((View v) ->
                JumpRouter.get().jump("/module2/pay").navigation(this)
        );
    }
}
