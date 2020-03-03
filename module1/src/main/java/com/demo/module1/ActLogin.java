package com.demo.module1;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.demo.annotation.Router;
import com.demo.core.core.JumpRouter;

/**
 * @author 尉迟涛
 * create time : 2020/3/3 18:28
 * description :
 */
@Router(path = "/module1/login")
public class ActLogin extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_login);

        findViewById(R.id.btn).setOnClickListener((View v) ->
                JumpRouter.get().jump("/module2/pay").navigation(this)
        );
    }
}
