package com.leco.ykg;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;

/**
 * Created by zs on 2018/9/20.
 */
public class NearbyLockListActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(MResource.getIdByName(getApplication(), "layout", "nearby_lock_list"));
//        setContentView(R.layout.nearby_lock_list);
        final ImageView mImageView = (ImageView) findViewById(MResource.getIdByName(getBaseContext(), "id", "back"));
        mImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}
