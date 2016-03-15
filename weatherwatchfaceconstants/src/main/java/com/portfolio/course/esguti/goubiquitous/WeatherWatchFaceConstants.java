package com.portfolio.course.esguti.goubiquitous;

public class WeatherWatchFaceConstants {
    public static final String KEY_WEATHER_NA = "N/A";
    public static final String KEY_WEATHER_CLEAR = "CLEAR";
    public static final String KEY_WEATHER_CLOUDY = "CLOUDY";
    public static final String KEY_WEATHER_FOG = "FOG";
    public static final String KEY_WEATHER_LIGHT_CLOUDS = "LIGHT_CLOUDS";
    public static final String KEY_WEATHER_LIGHT_RAIN = "RAIN";
    public static final String KEY_WEATHER_RAIN = "RAIN";
    public static final String KEY_WEATHER_SNOW = "SNOW";
    public static final String KEY_WEATHER_STORM = "STORM";

    /**
     * Helper method to provide the icon resource id according to the weather condition id returned
     * by the OpenWeatherMap call.
     * @param weatherId from OpenWeatherMap API response
     * @return wearable resource id for the corresponding icon.
     */
    public static String getWearableIconResourceForWeatherCondition(int weatherId) {
        // Based on weather code data found at:
        // http://bugs.openweathermap.org/projects/api/wiki/Weather_Condition_Codes
        if (weatherId >= 200 && weatherId <= 232) {
            return WeatherWatchFaceConstants.KEY_WEATHER_STORM;
        } else if (weatherId >= 300 && weatherId <= 321) {
            return WeatherWatchFaceConstants.KEY_WEATHER_LIGHT_RAIN;
        } else if (weatherId >= 500 && weatherId <= 504) {
            return WeatherWatchFaceConstants.KEY_WEATHER_RAIN;
        } else if (weatherId == 511) {
            return WeatherWatchFaceConstants.KEY_WEATHER_SNOW;
        } else if (weatherId >= 520 && weatherId <= 531) {
            return WeatherWatchFaceConstants.KEY_WEATHER_RAIN;
        } else if (weatherId >= 600 && weatherId <= 622) {
            return WeatherWatchFaceConstants.KEY_WEATHER_SNOW;
        } else if (weatherId >= 701 && weatherId <= 761) {
            return WeatherWatchFaceConstants.KEY_WEATHER_FOG;
        } else if (weatherId == 761 || weatherId == 781) {
            return WeatherWatchFaceConstants.KEY_WEATHER_STORM;
        } else if (weatherId == 800) {
            return WeatherWatchFaceConstants.KEY_WEATHER_CLEAR;
        } else if (weatherId == 801) {
            return WeatherWatchFaceConstants.KEY_WEATHER_LIGHT_CLOUDS;
        } else if (weatherId >= 802 && weatherId <= 804) {
            return WeatherWatchFaceConstants.KEY_WEATHER_CLOUDY;
        }
        return WeatherWatchFaceConstants.KEY_WEATHER_NA;
    }
}

