package com.coolweather.android;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.Image;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.coolweather.android.gson.Air_now_city;
import com.coolweather.android.gson.Forecast;
import com.coolweather.android.gson.LifeStyle;
import com.coolweather.android.gson.Weather;
import com.coolweather.android.gson.WeatherAir;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;

import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Text;

import java.io.IOException;
import java.util.Random;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {

    public static final String KEY = "4b6321fb6a65451e894fae3456ce8a06";

    private ScrollView weatherLayout;
    private TextView titleCity;
    private TextView titleUpdateTime;
    private TextView degreeText;
    private TextView weatherInfoText;
    private LinearLayout forecastLayout;
    private TextView aqiText;
    private TextView pm25Text;
    private LinearLayout suggestionLayout;

    private ImageView bingPicImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT>=21){
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        setContentView(R.layout.activity_weather);
        weatherLayout = (ScrollView)findViewById(R.id.weather_layout);
        titleCity = (TextView)findViewById(R.id.title_city);
        titleUpdateTime = (TextView)findViewById(R.id.title_update_time);
        degreeText = (TextView)findViewById(R.id.degree_text);
        weatherInfoText = (TextView)findViewById(R.id.weather_info_text);
        forecastLayout = (LinearLayout)findViewById(R.id.forecast_layout);
        aqiText = (TextView)findViewById(R.id.aqi_text);
        pm25Text = (TextView)findViewById(R.id.pm25_text);
        suggestionLayout = (LinearLayout)findViewById(R.id.suggestion_layout);
        bingPicImage = (ImageView)findViewById(R.id.bing_pic_img);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = prefs.getString("weather",null);
        String airString = prefs.getString("airNowCity",null);
        if (weatherString != null){
            //有缓存时直接解析天气数据
            Weather weather = Utility.handleWeatherResponse(weatherString);
            showWeatherInfo(weather);
            if (airString != null){
                Air_now_city airNowCity = Utility.handleAirResponse(airString).air_now_city;
                showAirInfo(airNowCity);
            }else {
                Toast.makeText(WeatherActivity.this,"加载空气质量出错",Toast.LENGTH_SHORT);
            }
        }else{
            String weatherId = getIntent().getStringExtra("weather_id");
            Log.d("WeatherActivity", "onCreate: "+weatherId);
            weatherLayout.setVisibility(View.INVISIBLE);
            requestWeather(weatherId);
            requestAir(weatherId);
        }
        String bingPic = prefs.getString("bing_pic",null);
        if (bingPic != null){
            Glide.with(this).load(bingPic).into(bingPicImage);
        }else {
            loadBingPic();
        }
    }

    private void loadBingPic() {
        String requestBingPic = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                final String bingPic = response.body().string();
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                editor.putString("bing_pic",bingPic);
                editor.apply();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(bingPic).into(bingPicImage);
                    }
                });
            }
        });
    }

    /**
     * 根据id获取空气相关信息
     * @param weatherId
     */
    private void requestAir(String weatherId) {
        String airUrl = "https://free-api.heweather.net/s6/air/now?location="+weatherId+"&key="+KEY;
        HttpUtil.sendOkHttpRequest(airUrl, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        weatherLayout.setVisibility(View.VISIBLE);
                        Toast.makeText(WeatherActivity.this,"获取空气质量信息失败",Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                final String responseText = response.body().string();
                final WeatherAir weatherAir = Utility.handleAirResponse(responseText);
                Log.d("WeatherActivity", "onResponse: status "+responseText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (weatherAir != null && "ok".equals(weatherAir.status)){
                            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                            editor.putString("airNowCity",responseText);
                            editor.apply();
                            showAirInfo(weatherAir.air_now_city);
                        }else{
                            weatherLayout.setVisibility(View.VISIBLE);
                            Toast.makeText(WeatherActivity.this,"获取空气质量信息失败",Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }

    private void showAirInfo(Air_now_city airNowCity) {
        if (airNowCity != null) {
            String aqi = airNowCity.aqi;
            String pm25 = airNowCity.pm25;
            aqiText.setText(aqi);
            pm25Text.setText(pm25);
        }
        weatherLayout.setVisibility(View.VISIBLE);
    }

    /**
     * 根据天气id请求城市天气信息
     * @param weatherId
     */
    private void requestWeather(final String weatherId) {
        String weatherUrl = "https://free-api.heweather.net/s6/weather/now?location="+weatherId+"&key="+KEY;
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this,"获取天气信息失败",Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                final String responseText = response.body().string();
                final Weather weather = Utility.handleWeatherResponse(responseText);
                Log.d("WeatherActivity", "status:"+weather.status);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (weather != null && "ok".equals(weather.status)){
                            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                            editor.putString("weather",responseText);
                            editor.apply();
                            showWeatherInfo(weather);
                        }else{
                            Toast.makeText(WeatherActivity.this,"获取天气信息失败",Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
        loadBingPic();
    }

    private void showWeatherInfo(Weather weather) {
        String cityName = weather.basic.cityName;
        String updateTime = weather.update.updateTime;
        String degree = weather.now.temperature+"℃";
        String weatherInfo = weather.now.condition;
        Log.d("WeatherActivity", "showWeatherInfo: "+weatherInfo);
        titleCity.setText(cityName);
        titleUpdateTime.setText(updateTime);
        degreeText.setText(degree);
        weatherInfoText.setText(weatherInfo);
        forecastLayout.removeAllViews();
        if (weather.forecastList!=null) {
            for (Forecast forecast : weather.forecastList) {
                View view = LayoutInflater.from(this).inflate(R.layout.forecast_item, forecastLayout, false);
                TextView dateText = (TextView) view.findViewById(R.id.date_text);
                TextView infoText = (TextView) view.findViewById(R.id.info_text);
                TextView maxText = (TextView) view.findViewById(R.id.max_text);
                TextView minText = (TextView) view.findViewById(R.id.min_text);
                dateText.setText(forecast.date);
                infoText.setText(forecast.dayCondition);
                maxText.setText(forecast.maxTemperature);
                minText.setText(forecast.minTemperature);
                forecastLayout.addView(view);
            }
        }
        suggestionLayout.removeAllViews();
        if (weather.lifeStyleList!=null) {
            for (LifeStyle lifeStyle : weather.lifeStyleList) {
                View view = LayoutInflater.from(this).inflate(R.layout.suggestion_item, suggestionLayout, false);
                TextView typeText = (TextView) view.findViewById(R.id.type_text);
                TextView brfText = (TextView) view.findViewById(R.id.brf_text);
                TextView suggestionText = (TextView) view.findViewById(R.id.suggestion_text);
                typeText.setText(lifeStyle.lifeStyleType);
                brfText.setText(lifeStyle.brfIntroduction);
                suggestionText.setText(lifeStyle.suggestion);
                suggestionLayout.addView(view);
            }
        }
    }
}
