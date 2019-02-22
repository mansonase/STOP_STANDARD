package com.viseeointernational.stop.view.custom;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.viseeointernational.stop.R;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SelectDialog extends Dialog {

    @BindView(R.id.lv)
    ListView lv;

    private Context context;
    private MyAdapter adapter;
    private Callback callback;

    private List<String> list = new ArrayList<>();

    public SelectDialog(@NonNull Context context, @NonNull Callback callback) {
        super(context, R.style.DialogBase);
        this.context = context;
        this.callback = callback;
    }

    private SelectDialog(@NonNull Context context, int themeResId) {
        super(context, themeResId);
    }

    private SelectDialog(@NonNull Context context, boolean cancelable, @Nullable OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_select);
        ButterKnife.bind(this);

        setCanceledOnTouchOutside(true);

        adapter = new MyAdapter();
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                callback.onSelect(SelectDialog.this, position, list.get(position));
                dismiss();
            }
        });
    }

    @OnClick(R.id.dismiss)
    public void onViewClicked() {
        dismiss();
    }

    public void show(@NonNull List<String> list) {
        super.show();
        this.list.clear();
        this.list.addAll(list);
        adapter.notifyDataSetChanged();
    }

    public interface Callback {

        void onSelect(SelectDialog dialog, int position, String item);
    }

    private class MyAdapter extends BaseAdapter {

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
                convertView = LayoutInflater.from(context).inflate(R.layout.item_select, null);
                holder = new ViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.text.setText(list.get(position));
            return convertView;
        }
    }

    static class ViewHolder {
        @BindView(R.id.text)
        TextView text;

        ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }
}
