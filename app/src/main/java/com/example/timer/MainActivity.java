package com.example.timer;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private ViewPager viewPager;
    private LinearLayout tabTimer, tabRecords;
    private TextView tabTimerText, tabRecordsText;
    private View tabTimerLine, tabRecordsLine;
    private RecordsFragment recordsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewPager = findViewById(R.id.view_pager);
        tabTimer = findViewById(R.id.tab_timer);
        tabRecords = findViewById(R.id.tab_records);
        tabTimerText = findViewById(R.id.tab_timer_text);
        tabRecordsText = findViewById(R.id.tab_records_text);
        tabTimerLine = findViewById(R.id.tab_timer_line);
        tabRecordsLine = findViewById(R.id.tab_records_line);

        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        final TimerFragment timerFragment = new TimerFragment();
        recordsFragment = new RecordsFragment();

        adapter.addFragment(timerFragment, "timer");
        adapter.addFragment(recordsFragment, "records");

        viewPager.setAdapter(adapter);

        timerFragment.setOnRecordAddedListener(() -> {
            if (recordsFragment != null) {
                recordsFragment.refresh();
            }
        });

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            @Override
            public void onPageSelected(int position) {
                updateTabSelection(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {}
        });

        tabTimer.setOnClickListener(v -> viewPager.setCurrentItem(0));
        tabRecords.setOnClickListener(v -> viewPager.setCurrentItem(1));

        updateTabSelection(0);
    }

    private void updateTabSelection(int position) {
        if (position == 0) {
            tabTimerText.setTextColor(getResources().getColor(R.color.accent));
            tabRecordsText.setTextColor(getResources().getColor(R.color.text_secondary));
            tabTimerLine.setVisibility(View.VISIBLE);
            tabRecordsLine.setVisibility(View.INVISIBLE);
        } else {
            tabRecordsText.setTextColor(getResources().getColor(R.color.accent));
            tabTimerText.setTextColor(getResources().getColor(R.color.text_secondary));
            tabRecordsLine.setVisibility(View.VISIBLE);
            tabTimerLine.setVisibility(View.INVISIBLE);
        }
    }

    private static class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }
    }
}