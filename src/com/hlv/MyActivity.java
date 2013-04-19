package com.hlv;

import android.app.Activity;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.BaseAdapter;
import android.widget.SimpleAdapter;
import com.hlv.R;
import com.hlv.horizontallistview.HorizontalListView;

public class MyActivity extends Activity {
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        HorizontalListView list = (HorizontalListView) findViewById(R.id.List);
        list.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return 10;
            }

            @Override
            public Object getItem(int i) {
                return Integer.valueOf(i);
            }

            @Override
            public long getItemId(int i) {
                return i;
            }

            @Override
            public View getView(int i, View view, ViewGroup viewGroup) {
                if (view == null) {
                    LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
                    view = inflater.inflate(R.layout.list_item, viewGroup, false);
                }
                return view;
            }
        });

        findViewById(R.id.LayoutDecrease).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeListHorizontalSize(-5);
            }
        });
        findViewById(R.id.LayoutIncrease).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeListHorizontalSize(5);
            }
        });
    }

    private void changeListHorizontalSize(int delta) {
        View list = findViewById(R.id.List);
        ViewGroup.LayoutParams listParams = list.getLayoutParams();
        listParams.width += delta;
        list.setLayoutParams(listParams);
    }
}
