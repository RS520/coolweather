package com.coolweather.android.gson;

import com.google.gson.annotations.SerializedName;

public class LifeStyle {

    @SerializedName("type")
    public String lifeStyleType;

    @SerializedName("brf")
    public String brfIntroduction;

    @SerializedName("txt")
    public String suggestion;
}
