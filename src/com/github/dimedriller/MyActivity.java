package com.github.dimedriller;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;
import com.github.R;
import com.github.dimedriller.horizontallistview.HorizontalListView;

public class MyActivity extends Activity {
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        HorizontalListView list = (HorizontalListView) findViewById(R.id.HorizontalList);
        list.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return 100;
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
                TextView textView = (TextView) view.findViewById(R.id.Text);
                textView.setText(Integer.toString(i));

                ViewGroup.LayoutParams params = view.getLayoutParams();
                //params.width = 80 + (i % 5) * 20;
                return view;
            }
        });
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toast toast = Toast.makeText(MyActivity.this, "Position# " + position, 2000);
                toast.show();
            }
        });

        findViewById(R.id.LayoutDecrease).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeListHorizontalSize(-50);
            }
        });
        findViewById(R.id.LayoutIncrease).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeListHorizontalSize(50);
            }
        });
    }

    private void changeListHorizontalSize(int delta) {
        View list = findViewById(R.id.HorizontalList);
        ViewGroup.LayoutParams listParams = list.getLayoutParams();
        listParams.width += delta;
        list.setLayoutParams(listParams);
    }
}
