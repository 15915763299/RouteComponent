package com.demo.module2;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.demo.annotation.Router;
import com.demo.core.core.JumpRouter;

/**
 * @author 尉迟涛
 * create time : 2020/3/3 18:28
 * description : 注意不同的module 不能使用同一个 group 作为分组名 否则会生成相同的类导致报
 */
@Router(path = "/module2/pay")
public class ActPay extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_pay);

        findViewById(R.id.btn).setOnClickListener((View v) ->
                JumpRouter.get().jump("/module1/login").navigation(this)
        );
    }
}
