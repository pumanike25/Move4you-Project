package com.example.maps4u;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatisticsActivity extends AppCompatActivity {

    private FirebaseHelper firebaseHelper;
    private String currentUserId;
    private BiometricData biometricData;
    private int dailyStepGoal = 10000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        firebaseHelper = new FirebaseHelper();
        currentUserId = firebaseHelper.getCurrentUserId();

        if (currentUserId == null) {
            Toast.makeText(this, "Session expired.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }


        loadBiometricDataAndCharts();
    }

    private void loadBiometricDataAndCharts() {
        firebaseHelper.getBiometricData(currentUserId, new FirebaseHelper.FirestoreCallback<BiometricData>() {
            @Override
            public void onCallback(BiometricData data) {
                if (data != null) {
                    biometricData = data;
                } else {
                    biometricData = new BiometricData(0, 0, 0, "", 0);
                }

                dailyStepGoal = biometricData.getDailyStepGoal();
                if (dailyStepGoal == 0) dailyStepGoal = 10000;

                setupAllCharts();
            }
        });
    }

    private void setupAllCharts() {
        setupTransportModeChart(findViewById(R.id.transportModeChartWebView));
        setupMonthlyActivityChart(findViewById(R.id.monthlyStepsChartWebView));
        setupStepsChart(findViewById(R.id.routesChartWebView));
        setupMoneySavedWidget(findViewById(R.id.moneySavedWebView));
        setupVirtualForestWidget(findViewById(R.id.virtualForestWebView));
        setupHeatmapWidget(findViewById(R.id.heatmapWebView));
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupTransportModeChart(WebView webView) {
        firebaseHelper.getUserHistory(currentUserId, new FirebaseHelper.FirestoreCallback<QuerySnapshot>() {
            @Override
            public void onCallback(QuerySnapshot snapshots) {
                Map<String, Integer> transportStats = new HashMap<>();

                if (snapshots != null) {
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        String mode = doc.getString("transportMode");
                        if (mode == null) mode = doc.getString("transport_mode");
                        if (mode != null) {
                            mode = mode.substring(0, 1).toUpperCase() + mode.substring(1).toLowerCase();
                            transportStats.put(mode, transportStats.getOrDefault(mode, 0) + 1);
                        }
                    }
                }

                try {
                    JSONArray dataArray = new JSONArray();
                    for (Map.Entry<String, Integer> entry : transportStats.entrySet()) {
                        JSONObject dataPoint = new JSONObject();
                        dataPoint.put("mode", entry.getKey());
                        dataPoint.put("count", entry.getValue());
                        dataArray.put(dataPoint);
                    }
                    loadChartHtml(webView, getTransportModeChartHtml(dataArray.toString()));
                } catch (JSONException e) { e.printStackTrace(); }
            }
        });
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupMonthlyActivityChart(WebView webView) {
        firebaseHelper.getAllDailySteps(currentUserId, new FirebaseHelper.FirestoreCallback<List<Map<String, Object>>>() {
            @Override
            public void onCallback(List<Map<String, Object>> stepsList) {
                Map<String, Integer> monthlySteps = new HashMap<>();
                SimpleDateFormat monthFormat = new SimpleDateFormat("MMM", Locale.getDefault());

                for (Map<String, Object> stepData : stepsList) {
                    try {
                        String dateString = (String) stepData.get("date");
                        Long stepsLong = (Long) stepData.get("steps");
                        int steps = stepsLong != null ? stepsLong.intValue() : 0;

                        if (dateString != null && steps > 0) {
                            Date date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateString);
                            String monthKey = monthFormat.format(date);
                            monthlySteps.put(monthKey, monthlySteps.getOrDefault(monthKey, 0) + steps);
                        }
                    } catch (Exception e) { e.printStackTrace(); }
                }

                try {
                    JSONArray dataArray = new JSONArray();
                    for (Map.Entry<String, Integer> entry : monthlySteps.entrySet()) {
                        JSONObject dataPoint = new JSONObject();
                        int steps = entry.getValue();
                        double co2SavedKg = (steps / 10000.0) * 1.2;

                        dataPoint.put("month", entry.getKey());
                        dataPoint.put("steps", steps);
                        dataPoint.put("co2", co2SavedKg);
                        dataArray.put(dataPoint);
                    }
                    loadChartHtml(webView, getMonthlyActivityChartHtml(dataArray.toString()));
                } catch (JSONException e) { e.printStackTrace(); }
            }
        });
    }

    private void loadChartHtml(WebView webView, String html) {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
    }

    // --- graphs ---

    private String getTransportModeChartHtml(String jsonData) {
        return "<!DOCTYPE html><html><head>" +
                "<script type=\"text/javascript\" src=\"https://www.gstatic.com/charts/loader.js\"></script>" +
                "<script type=\"text/javascript\">" +
                "google.charts.load('current', {'packages':['corechart']});" +
                "google.charts.setOnLoadCallback(drawChart);" +
                "function drawChart() {" +
                "var data = new google.visualization.DataTable();" +
                "data.addColumn('string', 'Mode');" +
                "data.addColumn('number', 'Trips');" +
                "var jsonData = " + jsonData + ";" +
                "if(jsonData.length === 0) { jsonData = [{mode:'No Data', count:1}]; }" +
                "jsonData.forEach(function(row) { data.addRow([row.mode, row.count]); });" +
                "var options = { " +
                "   title: 'Transport Preferences', " +
                "   is3D: true, " +
                "   colors: ['#34C759', '#FF3B30', '#8E8E93']," +
                "   chartArea: {width: '100%', height: '80%'}," +
                "   legend: { position: 'bottom', textStyle: {fontSize: 14} }" +
                "};" +
                "var chart = new google.visualization.PieChart(document.getElementById('chart_div'));" +
                "chart.draw(data, options); }" +
                "</script></head><body style=\"margin:0; padding:0;\"><div id=\"chart_div\" style=\"width: 100%; height: 100vh;\"></div></body></html>";
    }

    private String getMonthlyActivityChartHtml(String jsonData) {
        return "<!DOCTYPE html><html><head>" +
                "<script type=\"text/javascript\" src=\"https://www.gstatic.com/charts/loader.js\"></script>" +
                "<script type=\"text/javascript\">" +
                "google.charts.load('current', {'packages':['corechart']});" +
                "google.charts.setOnLoadCallback(drawChart);" +
                "function drawChart() {" +
                "var data = new google.visualization.DataTable();" +
                "data.addColumn('string', 'Month');" +
                "data.addColumn('number', 'Steps');" +
                "data.addColumn('number', 'CO2 Saved (kg)');" +
                "var jsonData = " + jsonData + ";" +
                "if(jsonData.length === 0) { jsonData = [{month:'No Data', steps:0, co2:0}]; }" +
                "jsonData.forEach(function(row) { data.addRow([row.month, row.steps, row.co2]); });" +
                "var options = { " +
                "   title: 'Activity & Eco Impact', " +
                "   vAxes: { 0: {title: 'Total Steps', format: 'short'}, 1: {title: 'CO2 (kg)', textStyle: {color: '#34A853'}} }, " +
                "   hAxis: { title: 'Month' }, " +
                "   seriesType: 'bars', " +
                "   series: { 1: {type: 'line', targetAxisIndex: 1, color: '#34A853', lineWidth: 3, pointSize: 5} }, " +
                "   colors: ['#4285F4'], " +
                "   animation: { startup: true, duration: 1000, easing: 'out' }," +
                "   legend: { position: 'top' }" +
                "};" +
                "var chart = new google.visualization.ComboChart(document.getElementById('chart_div'));" +
                "chart.draw(data, options); }" +
                "</script></head><body style=\"margin:0; padding:0;\"><div id=\"chart_div\" style=\"width: 100%; height: 100vh;\"></div></body></html>";
    }

    private void setupStepsChart(android.webkit.WebView webView) {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setBackgroundColor(android.graphics.Color.WHITE); // Corectat din fundalul negru

        if (currentUserId != null) {
            firebaseHelper.getAllDailySteps(currentUserId, new FirebaseHelper.FirestoreCallback<List<Map<String, Object>>>() {
                @Override
                public void onCallback(List<Map<String, Object>> stepsList) {
                    try {
                        org.json.JSONArray jsonArray = new org.json.JSONArray();
                        for (Map<String, Object> stepData : stepsList) {
                            org.json.JSONObject obj = new org.json.JSONObject();
                            obj.put("date", stepData.get("date"));
                            obj.put("steps", stepData.get("steps"));
                            jsonArray.put(obj);
                        }
                        String finalHtml = getStepsChartHtml(jsonArray.toString());
                        webView.loadDataWithBaseURL(null, finalHtml, "text/html", "UTF-8", null);
                    } catch (org.json.JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    private String getStepsChartHtml(String jsonData) {
        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\">\n" +
                "<script src=\"https://cdn.jsdelivr.net/npm/chart.js\"></script>\n" +
                "<style>\n" +
                "body { background-color: #FFFFFF; color: #212121; font-family: -apple-system, BlinkMacSystemFont, sans-serif; margin: 0; padding: 16px; }\n" +
                ".segmented-control { background: #F0F0F0; border-radius: 10px; display: flex; padding: 4px; margin-bottom: 20px; }\n" +
                ".seg-btn { flex: 1; text-align: center; padding: 8px; border-radius: 8px; color: #757575; font-size: 14px; font-weight: bold; transition: 0.2s; }\n" +
                ".seg-btn.active { background: #FFFFFF; color: #212121; box-shadow: 0px 2px 4px rgba(0,0,0,0.1); }\n" +
                ".label-title { color: #757575; font-size: 12px; font-weight: bold; letter-spacing: 1px; text-transform: uppercase; }\n" +
                ".avg-value { color: #4285F4; font-size: 34px; font-weight: bold; margin-top: 4px; }\n" +
                ".sub-header { display: flex; align-items: center; margin-top: 4px; margin-bottom: 16px; }\n" +
                ".date-range { font-size: 16px; font-weight: bold; color: #212121; margin-right: 12px; }\n" +
                ".year-btn { background: none; border: none; color: #4285F4; font-size: 20px; font-weight: bold; padding: 0 10px; cursor: pointer; }\n" +
                "</style>\n" +
                "</head>\n" +
                "<body>\n" +
                "<div class=\"segmented-control\">\n" +
                "<div id=\"btnW\" class=\"seg-btn active\" onclick=\"setMode('W')\">W</div>\n" +
                "<div id=\"btnM\" class=\"seg-btn\" onclick=\"setMode('M')\">M</div>\n" +
                "<div id=\"btnY\" class=\"seg-btn\" onclick=\"setMode('Y')\">Y</div>\n" +
                "</div>\n" +
                "<div class=\"label-title\">Daily Average</div>\n" +
                "<div id=\"avgText\" class=\"avg-value\">0</div>\n" +
                "<div class=\"sub-header\">\n" +
                "<div id=\"dateRange\" class=\"date-range\">This Week</div>\n" +
                "<div id=\"yearControls\" style=\"display: none;\">\n" +
                "<button class=\"year-btn\" onclick=\"changeYear(-1)\">&#8592;</button>\n" +
                "<button class=\"year-btn\" onclick=\"changeYear(1)\">&#8594;</button>\n" +
                "</div>\n" +
                "</div>\n" +
                "<div style=\"position: relative; height: 260px; width: 100%;\"><canvas id=\"stepChart\"></canvas></div>\n" +
                "<script>\n" +
                "const rawData = " + jsonData + ";\n" +
                "const stepMap = {}; rawData.forEach(d => stepMap[d.date] = d.steps);\n" +
                "let mode = 'W'; let targetYear = new Date().getFullYear();\n" +
                "let myChart = null;\n" +
                "function initChart() {\n" +
                "  const ctx = document.getElementById('stepChart').getContext('2d');\n" +
                "  myChart = new Chart(ctx, { type: 'bar', data: { labels: [], datasets: [{ data: [], backgroundColor: '#4285F4', borderRadius: {topLeft: 4, topRight: 4, bottomLeft: 0, bottomRight: 0}, borderSkipped: false }] },\n" +
                "  options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false }, tooltip: { enabled: false } }, scales: {\n" +
                "  x: { grid: { display: false }, ticks: { color: '#757575', font: { size: 12 } } },\n" +
                "  y: { position: 'right', grid: { color: '#E0E0E0', drawBorder: false, borderDash: [4, 4] }, ticks: { color: '#757575', maxTicksLimit: 5, callback: function(v){ return v >= 1000 ? (v/1000)+'k' : v; } } }\n" +
                "  } } }); updateView();\n" +
                "}\n" +
                "function setMode(newMode) { mode = newMode;\n" +
                "  document.querySelectorAll('.seg-btn').forEach(b => b.classList.remove('active'));\n" +
                "  document.getElementById('btn' + mode).classList.add('active');\n" +
                "  document.getElementById('yearControls').style.display = (mode === 'Y') ? 'block' : 'none';\n" +
                "  updateView(); }\n" +
                "function changeYear(delta) { targetYear += delta; updateView(); }\n" +
                "function updateView() {\n" +
                "  let labels = []; let data = []; let total = 0; let count = 0; let subtitle = '';\n" +
                "  let curr = new Date();\n" +
                "  if(mode === 'W') {\n" +
                "    let first = curr.getDate() - curr.getDay() + (curr.getDay() === 0 ? -6 : 1);\n" +
                "    labels = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];\n" +
                "    for(let i=0; i<7; i++) { let d = new Date(curr.setDate(first + i)); let v = stepMap[d.toISOString().split('T')[0]] || 0; data.push(v); if(v>0){total+=v; count++;} }\n" +
                "    subtitle = 'This Week';\n" +
                "  } else if(mode === 'M') {\n" +
                "    let y = curr.getFullYear(), m = curr.getMonth(); let days = new Date(y, m+1, 0).getDate();\n" +
                "    for(let i=1; i<=days; i++) { labels.push((i===1 || i===8 || i===15 || i===22 || i===29) ? i.toString() : ''); let dStr = y+'-'+String(m+1).padStart(2,'0')+'-'+String(i).padStart(2,'0'); let v = stepMap[dStr] || 0; data.push(v); if(v>0){total+=v; count++;} }\n" +
                "    let mNames = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']; subtitle = mNames[m] + ' ' + y;\n" +
                "  } else if(mode === 'Y') {\n" +
                "    labels = ['J', 'F', 'M', 'A', 'M', 'J', 'J', 'A', 'S', 'O', 'N', 'D'];\n" +
                "    for(let m=1; m<=12; m++) { let mTotal = 0; let dCount = 0; let days = new Date(targetYear, m, 0).getDate();\n" +
                "      for(let d=1; d<=days; d++) { let dStr = targetYear+'-'+String(m).padStart(2,'0')+'-'+String(d).padStart(2,'0'); let v = stepMap[dStr] || 0; if(v>0){mTotal+=v; dCount++;} }\n" +
                "      let mAvg = dCount>0 ? Math.round(mTotal/dCount) : 0; data.push(mAvg); if(mAvg>0){total+=mAvg; count++;} }\n" +
                "    subtitle = targetYear.toString();\n" +
                "  }\n" +
                "  document.getElementById('avgText').innerText = count > 0 ? Math.round(total/count).toLocaleString('de-DE') : '0';\n" +
                "  document.getElementById('dateRange').innerText = subtitle;\n" +
                "  myChart.data.labels = labels; myChart.data.datasets[0].data = data; myChart.update();\n" +
                "}\n" +
                "window.onload = initChart;\n" +
                "</script>\n" +
                "</body>\n" +
                "</html>";
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupMoneySavedWidget(WebView webView) {
        int safeMoneyGoal = (biometricData != null && biometricData.getMonthlyMoneyGoal() > 0)
                ? biometricData.getMonthlyMoneyGoal() : 50;

        firebaseHelper.getAllDailySteps(currentUserId, new FirebaseHelper.FirestoreCallback<List<Map<String, Object>>>() {
            @Override
            public void onCallback(List<Map<String, Object>> stepsList) {
                StringBuilder jsonBuilder = new StringBuilder("{");
                for (int i = 0; i < stepsList.size(); i++) {
                    Map<String, Object> day = stepsList.get(i);
                    String dateStr = (String) day.get("date");
                    Long steps = (Long) day.get("steps");

                    if (dateStr != null && steps != null) {
                        jsonBuilder.append("\"").append(dateStr).append("\":").append(steps);
                        if (i < stepsList.size() - 1) {
                            jsonBuilder.append(",");
                        }
                    }
                }
                if (jsonBuilder.length() > 1 && jsonBuilder.charAt(jsonBuilder.length() - 1) == ',') {
                    jsonBuilder.deleteCharAt(jsonBuilder.length() - 1);
                }
                jsonBuilder.append("}");

                String html = getMoneySavedWidgetHtml(jsonBuilder.toString(), safeMoneyGoal);

                runOnUiThread(() -> {
                    webView.getSettings().setJavaScriptEnabled(true);
                    webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
                });
            }
            @Override public void onFailure(Exception e) {}
        });
    }

    private String getMoneySavedWidgetHtml(String stepsJsonData, int monthlyGoal) {
        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n" +
                "<style>\n" +
                "  @import url('https://fonts.googleapis.com/css2?family=Poppins:wght@400;500;600;700;800&display=swap');\n" +
                "  body { font-family: 'Poppins', sans-serif; margin: 0; padding: 20px 15px; display: flex; flex-direction: column; align-items: center; background: #ffffff; user-select: none; }\n" +
                "  .widget-container { width: 100%; max-width: 350px; }\n" +
                "  .header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }\n" +
                "  .header button { background: none; border: none; font-size: 1.2em; color: #2196F3; font-weight: bold; cursor: pointer; padding: 10px; }\n" +
                "  .header h2 { margin: 0; font-size: 1.1em; font-weight: 700; color: #333; text-transform: uppercase; letter-spacing: 1px; }\n" +
                "  \n" +
                "  .savings-text { text-align: center; font-size: 2.5em; font-weight: 800; color: #111; margin-bottom: 0px; line-height: 1; }\n" +
                "  .savings-sub { text-align: center; font-size: 0.85em; color: #777; font-weight: 500; margin-bottom: 15px; }\n" +
                "  \n" +
                "  .details-box { display: flex; justify-content: center; gap: 15px; background: #f8f9fa; padding: 12px; border-radius: 12px; margin-bottom: 30px; border: 1px solid #eee; }\n" +
                "  .detail-item { font-size: 0.75em; color: #555; font-weight: 600; display: flex; align-items: center; gap: 4px; }\n" +
                "  \n" +
                "  .road-container { position: relative; width: 100%; margin-bottom: 15px; padding-top: 15px; }\n" +
                "  .road-bg { height: 10px; background: #E0E0E0; border-radius: 5px; position: relative; width: 100%; }\n" +
                "  .road-progress { height: 100%; background: linear-gradient(90deg, #4CAF50, #8BC34A); border-radius: 5px; width: 0%; transition: width 0.8s ease-out; }\n" +
                "  \n" +
                "  .avatar { position: absolute; top: -20px; left: 0%; transform: translateX(-50%); font-size: 28px; transition: left 0.8s ease-out; }\n" +
                "  \n" +
                "  .labels { display: flex; justify-content: space-between; font-size: 0.8em; font-weight: 600; color: #777; margin-top: 8px; }\n" +
                "  \n" +
                "  .goal-reached { color: #4CAF50; font-weight: 700; text-align: center; font-size: 0.9em; margin-top: 15px; opacity: 0; transition: opacity 0.5s; }\n" +
                "</style>\n" +
                "</head>\n" +
                "<body>\n" +
                "<div class=\"widget-container\">\n" +
                "  <div class=\"header\">\n" +
                "    <button id=\"prevBtn\">&#10094;</button>\n" +
                "    <h2 id=\"monthYear\">Month Year</h2>\n" +
                "    <button id=\"nextBtn\">&#10095;</button>\n" +
                "  </div>\n" +
                "  \n" +
                "  <div class=\"savings-text\">€<span id=\"amountDisplay\">0.00</span></div>\n" +
                "  <div class=\"savings-sub\">Saved this month</div>\n" +
                "  \n" +
                "  <div class=\"details-box\">\n" +
                "    <div class=\"detail-item\">⛽ <span id=\"fuelDisplay\">0.0</span> L saved</div>\n" +
                "    <div class=\"detail-item\">🛣️ <span id=\"kmDisplay\">0.0</span> km</div>\n" +
                "    <div class=\"detail-item\">💶 1.50 €/L</div>\n" +
                "  </div>\n" +
                "\n" +
                "  <div class=\"road-container\">\n" +
                "    <div class=\"avatar\" id=\"avatarIcon\">🚶</div>\n" +
                "    <div class=\"road-bg\">\n" +
                "      <div class=\"road-progress\" id=\"progressBar\"></div>\n" +
                "    </div>\n" +
                "    <div class=\"labels\">\n" +
                "      <span>€0</span>\n" +
                "      <span>Goal: €" + monthlyGoal + "</span>\n" +
                "    </div>\n" +
                "  </div>\n" +
                "  <div class=\"goal-reached\" id=\"successMsg\">Goal Reached! Amazing job! 🎉</div>\n" +
                "</div>\n" +
                "\n" +
                "<script>\n" +
                "  const stepData = " + stepsJsonData + ";\n" +
                "  const monthlyGoal = " + monthlyGoal + ";\n" +
                "  const fuelPricePerLiter = 1.50;\n" +
                "  const litersPerKm = 0.08;\n" +
                "  \n" +
                "  let currentDate = new Date();\n" +
                "  \n" +
                "  function updateWidget(date) {\n" +
                "    const year = date.getFullYear();\n" +
                "    const month = date.getMonth();\n" +
                "    const monthNames = [\"Jan\", \"Feb\", \"Mar\", \"Apr\", \"May\", \"Jun\", \"Jul\", \"Aug\", \"Sep\", \"Oct\", \"Nov\", \"Dec\"];\n" +
                "    document.getElementById('monthYear').innerText = monthNames[month] + \" \" + year;\n" +
                "    \n" +
                "    let totalSteps = 0;\n" +
                "    const monthPrefix = `${year}-${String(month + 1).padStart(2, '0')}`;\n" +
                "    for (let dateKey in stepData) {\n" +
                "      if (dateKey.startsWith(monthPrefix)) {\n" +
                "        totalSteps += stepData[dateKey];\n" +
                "      }\n" +
                "    }\n" +
                "    \n" +
                "    let distanceKm = totalSteps * 0.000762;\n" +
                "    let fuelSavedLiters = distanceKm * litersPerKm;\n" +
                "    let totalSavedEuro = fuelSavedLiters * fuelPricePerLiter;\n" +
                "    \n" +
                "    document.getElementById('amountDisplay').innerText = totalSavedEuro.toFixed(2);\n" +
                "    document.getElementById('fuelDisplay').innerText = fuelSavedLiters.toFixed(1);\n" +
                "    document.getElementById('kmDisplay').innerText = distanceKm.toFixed(1);\n" +
                "    \n" +
                "    let percentage = monthlyGoal > 0 ? (totalSavedEuro / monthlyGoal) * 100 : 0;\n" +
                "    let displayPercent = Math.min(100, Math.max(0, percentage));\n" +
                "    \n" +
                "    document.getElementById('progressBar').style.width = displayPercent + '%';\n" +
                "    document.getElementById('avatarIcon').style.left = displayPercent + '%';\n" +
                "    \n" +
                "    const successMsg = document.getElementById('successMsg');\n" +
                "    if (percentage >= 100 && monthlyGoal > 0) {\n" +
                "       document.getElementById('avatarIcon').innerText = '🏃💨';\n" +
                "       successMsg.style.opacity = 1;\n" +
                "    } else {\n" +
                "       document.getElementById('avatarIcon').innerText = '🚶';\n" +
                "       successMsg.style.opacity = 0;\n" +
                "    }\n" +
                "  }\n" +
                "  \n" +
                "  document.getElementById('prevBtn').addEventListener('click', () => {\n" +
                "    currentDate.setMonth(currentDate.getMonth() - 1);\n" +
                "    updateWidget(currentDate);\n" +
                "  });\n" +
                "  document.getElementById('nextBtn').addEventListener('click', () => {\n" +
                "    currentDate.setMonth(currentDate.getMonth() + 1);\n" +
                "    updateWidget(currentDate);\n" +
                "  });\n" +
                "  \n" +
                "  updateWidget(currentDate);\n" +
                "</script>\n" +
                "</body>\n" +
                "</html>";
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupVirtualForestWidget(WebView webView) {
        firebaseHelper.getAllDailySteps(currentUserId, new FirebaseHelper.FirestoreCallback<List<Map<String, Object>>>() {
            @Override
            public void onCallback(List<Map<String, Object>> stepsList) {
                StringBuilder jsonBuilder = new StringBuilder("{");
                for (int i = 0; i < stepsList.size(); i++) {
                    Map<String, Object> day = stepsList.get(i);
                    String dateStr = (String) day.get("date");
                    Long steps = (Long) day.get("steps");

                    if (dateStr != null && steps != null) {
                        jsonBuilder.append("\"").append(dateStr).append("\":").append(steps);
                        if (i < stepsList.size() - 1) {
                            jsonBuilder.append(",");
                        }
                    }
                }
                if (jsonBuilder.length() > 1 && jsonBuilder.charAt(jsonBuilder.length() - 1) == ',') {
                    jsonBuilder.deleteCharAt(jsonBuilder.length() - 1);
                }
                jsonBuilder.append("}");

                String html = getVirtualForestHtml(jsonBuilder.toString());
                runOnUiThread(() -> {
                    webView.getSettings().setJavaScriptEnabled(true);
                    webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
                });
            }
            @Override
            public void onFailure(Exception e) {
                String html = getVirtualForestHtml("{}");
                runOnUiThread(() -> {
                    webView.getSettings().setJavaScriptEnabled(true);
                    webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
                });
            }
        });
    }

    private String getVirtualForestHtml(String stepsJsonData) {
        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n" +
                "<style>\n" +
                "  @import url('https://fonts.googleapis.com/css2?family=Poppins:wght@400;600;700;800&display=swap');\n" +
                "  body { font-family: 'Poppins', sans-serif; display: flex; justify-content: center; align-items: center; margin: 0; padding: 15px 0; background-color: transparent; user-select: none; }\n" +
                "  .widget-container { width: 90%; max-width: 350px; background: #ffffff; border-radius: 24px; padding: 25px 20px; box-shadow: 0 12px 30px rgba(0,0,0,0.06); text-align: center; position: relative; overflow: hidden; border: 1px solid rgba(0,0,0,0.05); }\n" +
                "  .bg-glow { position: absolute; top: -50%; left: -50%; width: 200%; height: 200%; background: radial-gradient(circle, #28a745 0%, transparent 60%); opacity: 0.12; z-index: 0; }\n" +
                "  .content { position: relative; z-index: 1; }\n" +
                "  \n" +
                "  /* Header pentru selectarea anului */\n" +
                "  .header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px; }\n" +
                "  .header button { background: none; border: none; font-size: 1.2em; color: #28a745; font-weight: bold; cursor: pointer; padding: 10px; }\n" +
                "  .header h2 { margin: 0; font-size: 1.2em; font-weight: 700; color: #333; letter-spacing: 1px; }\n" +
                "  \n" +
                "  .icon-box { font-size: 3em; margin: 0 auto 5px; text-shadow: 0 10px 20px rgba(40, 167, 69, 0.4); }\n" +
                "  .title { font-size: 0.85em; font-weight: 600; color: #888; text-transform: uppercase; letter-spacing: 1.5px; margin-bottom: 15px; }\n" +
                "  .stat-box { margin-bottom: 15px; }\n" +
                "  .amount { font-size: 3.5em; font-weight: 800; color: #111; line-height: 1; }\n" +
                "  .stat-label { font-size: 1em; font-weight: 600; color: #28a745; }\n" +
                "  .stat-desc { font-size: 0.75em; color: #777; margin-top: 2px; }\n" +
                "  .amount-small { font-size: 2em; font-weight: 700; color: #444; line-height: 1; margin-top: 10px; }\n" +
                "  .message { font-size: 0.85em; color: #555; font-weight: 500; margin-top: 10px; border-top: 1px solid #eee; padding-top: 10px; }\n" +
                "</style>\n" +
                "</head>\n" +
                "<body>\n" +
                "<div class=\"widget-container\">\n" +
                "  <div class=\"bg-glow\"></div>\n" +
                "  <div class=\"content\">\n" +
                "    <div class=\"header\">\n" +
                "      <button id=\"prevBtn\">&#10094;</button>\n" +
                "      <h2 id=\"yearDisplay\">2026</h2>\n" +
                "      <button id=\"nextBtn\">&#10095;</button>\n" +
                "    </div>\n" +
                "    \n" +
                "    <div class=\"icon-box\">🌳</div>\n" +
                "    <div class=\"title\">Virtual Forest</div>\n" +
                "    \n" +
                "    <div class=\"stat-box\">\n" +
                "      <div class=\"amount\" id=\"treeCounter\">0</div>\n" +
                "      <div class=\"stat-label\">Trees Equivalent</div>\n" +
                "      <div class=\"stat-desc\">(A tree absorbs ~22kg of CO₂/year)</div>\n" +
                "    </div>\n" +
                "    \n" +
                "    <div class=\"stat-box\">\n" +
                "      <div class=\"amount-small\"><span id=\"co2Saved\">0</span> kg</div>\n" +
                "      <div class=\"stat-label\" style=\"font-size: 0.85em; color: #666;\">CO₂ Emissions Prevented</div>\n" +
                "    </div>\n" +
                "    \n" +
                "    <div class=\"message\">By walking instead of driving this year, you are effectively doing the work of these trees!</div>\n" +
                "  </div>\n" +
                "</div>\n" +
                "<script>\n" +
                "  const stepData = " + stepsJsonData + ";\n" +
                "  let currentYear = new Date().getFullYear();\n" +
                "  \n" +
                "  function animateValue(obj, start, end, duration, isFloat) {\n" +
                "    let startTimestamp = null;\n" +
                "    const step = (timestamp) => {\n" +
                "      if (!startTimestamp) startTimestamp = timestamp;\n" +
                "      const progress = Math.min((timestamp - startTimestamp) / duration, 1);\n" +
                "      const val = start + (1 - Math.pow(1 - progress, 4)) * (end - start);\n" +
                "      obj.innerHTML = isFloat ? val.toFixed(1) : Math.floor(val);\n" +
                "      if (progress < 1) { window.requestAnimationFrame(step); }\n" +
                "    };\n" +
                "    window.requestAnimationFrame(step);\n" +
                "  }\n" +
                "  \n" +
                "  function updateWidget(year) {\n" +
                "    document.getElementById('yearDisplay').innerText = year;\n" +
                "    \n" +
                "    // 1. Calculam pasii pe intreg anul selectat\n" +
                "    let totalSteps = 0;\n" +
                "    const yearPrefix = `${year}-`;\n" +
                "    for (let dateKey in stepData) {\n" +
                "      if (dateKey.startsWith(yearPrefix)) {\n" +
                "        totalSteps += stepData[dateKey];\n" +
                "      }\n" +
                "    }\n" +
                "    \n" +
                "    // 2. Facem calculele de mediu\n" +
                "    let distanceKm = totalSteps * 0.000762;\n" +
                "    let co2SavedKg = distanceKm * 0.15;\n" +
                "    let treesEquivalent = co2SavedKg / 22.0;\n" +
                "    \n" +
                "    // 3. Animam numerele de la 0 la noua valoare\n" +
                "    animateValue(document.getElementById('treeCounter'), 0, treesEquivalent, 1500, true);\n" +
                "    animateValue(document.getElementById('co2Saved'), 0, co2SavedKg, 1500, true);\n" +
                "  }\n" +
                "  \n" +
                "  document.getElementById('prevBtn').addEventListener('click', () => {\n" +
                "    currentYear--;\n" +
                "    updateWidget(currentYear);\n" +
                "  });\n" +
                "  document.getElementById('nextBtn').addEventListener('click', () => {\n" +
                "    currentYear++;\n" +
                "    updateWidget(currentYear);\n" +
                "  });\n" +
                "  \n" +
                "  // Init widget\n" +
                "  updateWidget(currentYear);\n" +
                "</script>\n" +
                "</body>\n" +
                "</html>";
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupHeatmapWidget(WebView webView) {
        firebaseHelper.getAllDailySteps(currentUserId, new FirebaseHelper.FirestoreCallback<List<Map<String, Object>>>() {
            @Override
            public void onCallback(List<Map<String, Object>> stepsList) {
                StringBuilder jsonBuilder = new StringBuilder("{");

                for (int i = 0; i < stepsList.size(); i++) {
                    Map<String, Object> day = stepsList.get(i);
                    String dateStr = (String) day.get("date");
                    Long steps = (Long) day.get("steps");

                    if (dateStr != null && steps != null) {
                        jsonBuilder.append("\"").append(dateStr).append("\":").append(steps);
                        if (i < stepsList.size() - 1) {
                            jsonBuilder.append(",");
                        }
                    }
                }

                if (jsonBuilder.length() > 1 && jsonBuilder.charAt(jsonBuilder.length() - 1) == ',') {
                    jsonBuilder.deleteCharAt(jsonBuilder.length() - 1);
                }
                jsonBuilder.append("}");

                int safeGoal = (dailyStepGoal > 0) ? dailyStepGoal : 10000;
                String html = getAppleFitnessCalendarHtml(jsonBuilder.toString(), safeGoal);

                runOnUiThread(() -> {
                    webView.getSettings().setJavaScriptEnabled(true);
                    webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
                });
            }
            @Override public void onFailure(Exception e) {}
        });
    }

    private String getAppleFitnessCalendarHtml(String stepsJsonData, int dailyGoal) {
        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n" +
                "<style>\n" +
                "  @import url('https://fonts.googleapis.com/css2?family=Poppins:wght@400;500;600;700&display=swap');\n" +
                "  body { font-family: 'Poppins', sans-serif; margin: 0; padding: 15px; background: #ffffff; display: flex; flex-direction: column; align-items: center; user-select: none; -webkit-tap-highlight-color: transparent; }\n" +
                "  .calendar-container { width: 100%; max-width: 350px; }\n" +
                "  .main-title { text-align: center; font-size: 1.1em; font-weight: 700; color: #111; text-transform: uppercase; letter-spacing: 1.2px; margin-bottom: 20px; }\n" +
                "  .header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }\n" +
                "  .header button { background: none; border: none; font-size: 1.2em; color: #34C759; font-weight: bold; cursor: pointer; padding: 10px; }\n" +
                "  .header h2 { margin: 0; font-size: 1.1em; font-weight: 600; color: #555; }\n" +
                "  .weekdays { display: grid; grid-template-columns: repeat(7, 1fr); text-align: center; font-size: 0.8em; font-weight: 600; color: #8E8E93; margin-bottom: 10px; }\n" +
                "  .days-grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 8px; text-align: center; }\n" +
                "  .day-wrapper { aspect-ratio: 1; display: flex; justify-content: center; align-items: center; position: relative; }\n" +
                "  .day-ring { width: 100%; height: 100%; border-radius: 50%; display: flex; justify-content: center; align-items: center; }\n" +
                "  .day-inner { width: 75%; height: 75%; background: white; border-radius: 50%; display: flex; justify-content: center; align-items: center; font-size: 0.85em; font-weight: 600; color: #333; }\n" +
                "  .day.empty { visibility: hidden; }\n" +
                "  .day-inner.full { background: #34C759; color: white; }\n" +
                "</style>\n" +
                "</head>\n" +
                "<body>\n" +
                "<div class=\"calendar-container\">\n" +
                "  <div class=\"main-title\">Activity Rings</div> \n" +
                "  <div class=\"header\">\n" +
                "    <button id=\"prevBtn\">&#10094;</button>\n" +
                "    <h2 id=\"monthYear\">Month Year</h2>\n" +
                "    <button id=\"nextBtn\">&#10095;</button>\n" +
                "  </div>\n" +
                "  <div class=\"weekdays\">\n" +
                "    <div>M</div><div>T</div><div>W</div><div>T</div><div>F</div><div>S</div><div>S</div>\n" +
                "  </div>\n" +
                "  <div class=\"days-grid\" id=\"daysGrid\"></div>\n" +
                "</div>\n" +
                "<script>\n" +
                "  const stepData = " + stepsJsonData + ";\n" +
                "  const dailyGoal = " + dailyGoal + ";\n" +
                "  let currentDate = new Date();\n" +
                "  function renderCalendar(date) {\n" +
                "    const year = date.getFullYear();\n" +
                "    const month = date.getMonth();\n" +
                "    const monthNames = [\"January\", \"February\", \"March\", \"April\", \"May\", \"June\", \"July\", \"August\", \"September\", \"October\", \"November\", \"December\"];\n" +
                "    document.getElementById('monthYear').innerText = monthNames[month] + \" \" + year;\n" +
                "    const firstDayIndex = new Date(year, month, 1).getDay();\n" +
                "    const startDay = firstDayIndex === 0 ? 6 : firstDayIndex - 1;\n" +
                "    const daysInMonth = new Date(year, month + 1, 0).getDate();\n" +
                "    const grid = document.getElementById('daysGrid');\n" +
                "    grid.innerHTML = '';\n" +
                "    for(let i = 0; i < startDay; i++) { grid.innerHTML += `<div class=\"day-wrapper empty\"></div>`; }\n" +
                "    for(let i = 1; i <= daysInMonth; i++) {\n" +
                "      const mStr = String(month + 1).padStart(2, '0');\n" +
                "      const dStr = String(i).padStart(2, '0');\n" +
                "      const dateKey = `${year}-${mStr}-${dStr}`;\n" +
                "      const steps = stepData[dateKey] || 0;\n" +
                "      \n" +
                "      let percentage = Math.min(100, Math.max(0, (steps / dailyGoal) * 100));\n" +
                "      let ringGradient = `conic-gradient(#34C759 ${percentage}%, #E5E5EA ${percentage}%)`;\n" +
                "      let innerClass = percentage >= 100 ? 'day-inner full' : 'day-inner';\n" +
                "      \n" +
                "      grid.innerHTML += `\n" +
                "        <div class=\"day-wrapper\">\n" +
                "          <div class=\"day-ring\" style=\"background: ${ringGradient};\">\n" +
                "            <div class=\"${innerClass}\">${i}</div>\n" +
                "          </div>\n" +
                "        </div>`;\n" +
                "    }\n" +
                "  }\n" +
                "  document.getElementById('prevBtn').addEventListener('click', () => { currentDate.setMonth(currentDate.getMonth() - 1); renderCalendar(currentDate); });\n" +
                "  document.getElementById('nextBtn').addEventListener('click', () => { currentDate.setMonth(currentDate.getMonth() + 1); renderCalendar(currentDate); });\n" +
                "  renderCalendar(currentDate);\n" +
                "</script>\n" +
                "</body>\n" +
                "</html>";
    }
}