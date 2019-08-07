package com.coolweather.android.gson;

import com.google.gson.annotations.SerializedName;

public class Forecast {
    public String date;

    @SerializedName("cond_txt_d")
    public String dayCondition;

    @SerializedName("cond_txt_n")
    public String nightCondition;

    @SerializedName("tmp_max")
    public String maxTemperature;

    @SerializedName("tmp_min")
    public String minTemperature;
}
