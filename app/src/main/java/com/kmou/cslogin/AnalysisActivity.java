package com.kmou.cslogin;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.List;

public class AnalysisActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analysis);

        // LineChart 초기화
        LineChart lineChart = findViewById(R.id.lineChart);

        // 샘플 데이터 생성 (실제 데이터로 교체 가능)
        List<Entry> entries = new ArrayList<>();
        entries.add(new Entry(0, 60));
        entries.add(new Entry(1, 65));
        entries.add(new Entry(2, 70));
        entries.add(new Entry(3, 75));
        entries.add(new Entry(4, 80));

        // 데이터셋 생성
        LineDataSet dataSet = new LineDataSet(entries, "심박수 데이터");
        dataSet.setColor(getResources().getColor(android.R.color.holo_blue_dark));
        dataSet.setValueTextColor(getResources().getColor(android.R.color.black));

        // 차트에 데이터 설정
        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);

        // 설명 제거
        Description description = new Description();
        description.setText("");
        lineChart.setDescription(description);
        lineChart.invalidate(); // 차트 새로고침
    }
}
