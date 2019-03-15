package com.viseeointernational.stop.view.custom;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;

import com.viseeointernational.stop.R;
import com.viseeointernational.stop.util.TimeUtil;

import java.util.Calendar;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class DateDialog extends Dialog {

    @BindView(R.id.title)
    TextView title;
    @BindView(R.id.gv)
    GridView gv;

    private Context context;
    private MyAdapter adapter;
    private Callback callback;

    private int baseYear;
    private int baseMonth;
    private int baseDay;

    private int year;
    private int month;

    public DateDialog(@NonNull Context context, Callback callback) {
        super(context, R.style.DialogBase);
        this.context = context;
        this.callback = callback;
    }

    private DateDialog(@NonNull Context context, @StyleRes int themeResId) {
        super(context, themeResId);
    }

    private DateDialog(@NonNull Context context, boolean cancelable, @Nullable OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_date);
        ButterKnife.bind(this);

        setCanceledOnTouchOutside(true);
        adapter = new MyAdapter();
        gv.setAdapter(adapter);
    }

    public void show(long baseTime, long showTime) {
        super.show();

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(baseTime);
        baseYear = calendar.get(Calendar.YEAR);
        baseMonth = calendar.get(Calendar.MONTH);
        baseDay = calendar.get(Calendar.DAY_OF_MONTH);

        calendar.setTimeInMillis(showTime);
        year = calendar.get(Calendar.YEAR);
        month = calendar.get(Calendar.MONTH);
        setYearAndMonth(year, month);
    }

    private void setYearAndMonth(int year, int month) {
        title.setText(TimeUtil.getEnglishMonth(month) + "  " + year);
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, 1);
        int datas[][] = new int[6][7];
        int daysOfFirstWeek = calendar.get(Calendar.DAY_OF_WEEK);
        int daysOfMonth = getDaysOfMonth(year, month + 1);
        int daysOfLastMonth = getDaysOfLastMonth(year, month + 1);
        int dayNum = 1;
        int nextDayNum = 1;
        for (int i = 0; i < datas.length; i++) {
            for (int j = 0; j < datas[i].length; j++) {
                if (i == 0 && j < daysOfFirstWeek - 1) {
                    datas[i][j] = daysOfLastMonth - daysOfFirstWeek + 2 + j;
                } else if (dayNum <= daysOfMonth) {
                    datas[i][j] = dayNum++;
                } else {
                    datas[i][j] = nextDayNum++;
                }
            }
        }
        adapter.setData(datas);
        adapter.notifyDataSetChanged();
    }

    private int getDaysOfLastMonth(int year, int realMonth) {
        int ret;
        if (realMonth == 1) {
            ret = getDaysOfMonth(year - 1, 12);
        } else {
            ret = getDaysOfMonth(year, realMonth - 1);
        }
        return ret;
    }

    private int getDaysOfMonth(int year, int realMonth) {
        switch (realMonth) {
            case 1:
            case 3:
            case 5:
            case 7:
            case 8:
            case 10:
            case 12:
                return 31;
            case 2:
                if (isLeap(year)) {
                    return 29;
                } else {
                    return 28;
                }
            case 4:
            case 6:
            case 9:
            case 11:
                return 30;
        }
        return -1;
    }

    private boolean isLeap(int year) {
        return (year % 4 == 0 && year % 100 != 0) || year % 400 == 0;
    }

    @OnClick({R.id.sub, R.id.add})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.sub:
                month--;
                if (month == -1) {
                    year--;
                    month = 11;
                }
                if (year == 1999) {
                    year++;
                    month = 0;
                    return;
                }
                setYearAndMonth(year, month);
                break;
            case R.id.add:
                month++;
                if (month == 12) {
                    year++;
                    month = 0;
                }
                if (year == 2100) {
                    year--;
                    month = 11;
                    return;
                }
                setYearAndMonth(year, month);
                break;
        }
    }

    private class MyAdapter extends BaseAdapter {

        int[] days = new int[42];

        public void setData(int[][] data) {
            int dayNum = 0;
            for (int i = 0; i < data.length; i++) {
                for (int j = 0; j < data[i].length; j++) {
                    days[dayNum] = data[i][j];
                    dayNum++;
                }
            }
        }

        @Override
        public int getCount() {
            return days.length;
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
            Holder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.item_day, null);
                holder = new Holder();
                holder.day = (TextView) convertView.findViewById(R.id.day);
                convertView.setTag(holder);
            } else {
                holder = (Holder) convertView.getTag();
            }
            if ((position < 7 && days[position] > 20) || (position > 20 && days[position] < 15)) {
                holder.day.setBackgroundResource(R.drawable.bg_day_of_other_month);
                holder.day.setTextColor(Color.GRAY);
                holder.day.setOnClickListener(null);
            } else {
                if (year == baseYear && month == baseMonth && days[position] == baseDay) {
                    holder.day.setBackgroundResource(R.drawable.bg_today);
                } else {
                    holder.day.setBackground(null);
                }
                holder.day.setTextColor(Color.WHITE);
                holder.day.setTag(days[position]);
                holder.day.setOnClickListener(onClickListener);
            }
            holder.day.setText(String.valueOf(days[position]));
            return convertView;
        }

        class Holder {
            TextView day;
        }
    }

    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.day:
                    if (v.getTag() instanceof Integer) {
                        int day = (int) v.getTag();
                        callback.onSelect(DateDialog.this, year, month, day);
                        dismiss();
                    }
                    break;
            }
        }
    };

    public interface Callback {
        void onSelect(DateDialog dialog, int year, int month, int day);
    }

}
