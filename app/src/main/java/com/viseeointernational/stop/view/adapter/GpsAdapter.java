package com.viseeointernational.stop.view.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.viseeointernational.stop.R;
import com.viseeointernational.stop.data.entity.State;
import com.viseeointernational.stop.util.TimeUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;

public class GpsAdapter extends BaseAdapter {

    private Context context;
    private List<State> list;

    private String format;
    private Callback callback;

    @Inject
    public GpsAdapter(Context context) {
        this.context = context;
        this.list = new ArrayList<>();
    }

    public void setData(List<State> list, String format) {
        this.format = format;
        this.list.clear();
        this.list.addAll(list);
        notifyDataSetChanged();
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_gps, null);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        State state = list.get(position);
        String s = "";
        try {
            s = TimeUtil.getTime(state.time, format) + "\n";
        } catch (Exception e) {
            e.printStackTrace();
        }
        String longitude = new BigDecimal(state.longitude).setScale(6, RoundingMode.HALF_UP).toString();
        String latitude = new BigDecimal(state.latitude).setScale(6, RoundingMode.HALF_UP).toString();
        s += "Longitude:   " + longitude + "°\n";
        s += "Latitude:      " + latitude + "°";
        holder.text.setText(s);
        holder.map.setTag(position);
        holder.map.setOnClickListener(onClickListener);
        return convertView;
    }

    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.map:
                    Object tag = v.getTag();
                    if (tag instanceof Integer) {
                        int pos = (int) tag;
                        if (callback != null) {
                            callback.onMapClick(GpsAdapter.this, list.get(pos));
                        }
                    }
                    break;
            }
        }
    };

    static class ViewHolder {
        @BindView(R.id.text)
        TextView text;
        @BindView(R.id.map)
        TextView map;

        ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }

    public interface Callback {

        void onMapClick(GpsAdapter adapter, State state);
    }
}
