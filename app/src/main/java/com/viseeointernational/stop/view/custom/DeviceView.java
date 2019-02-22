package com.viseeointernational.stop.view.custom;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.viseeointernational.stop.R;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DeviceView extends RelativeLayout {

    @BindView(R.id.image)
    ImageView image;
    @BindView(R.id.disconnected)
    ImageView disconnected;
    @BindView(R.id.count)
    TextView count;
    @BindView(R.id.border)
    View border;

    private Context context;

    public DeviceView(@NonNull Context context) {
        super(context);
        this.context = context;
        init();
    }

    private DeviceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private DeviceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private void init() {
        LayoutInflater.from(context).inflate(R.layout.device, this);
        ButterKnife.bind(this);
    }

    public void setImage(String imagePath) {
        try {
            Picasso.with(context).load(new File(imagePath)).placeholder(R.mipmap.ic_default_header).into(image);
        } catch (Exception e) {
            e.printStackTrace();
            Picasso.with(context).load(R.mipmap.ic_default_header).into(image);
        }
    }

    public void setCheck(boolean isCheck) {
        if (isCheck) {
            border.setVisibility(VISIBLE);
        } else {
            border.setVisibility(INVISIBLE);
        }
    }

    public void setMovements(int i) {
        if (i < 1) {
            count.setVisibility(INVISIBLE);
        } else {
            if (i > 99) {
                count.setText("99+");
            } else {
                count.setText(String.valueOf(i));
            }
            count.setVisibility(VISIBLE);
        }
    }

    public void setConnected(boolean b) {
        if (b) {
            disconnected.setVisibility(INVISIBLE);
        } else {
            disconnected.setVisibility(VISIBLE);
        }
    }

}
