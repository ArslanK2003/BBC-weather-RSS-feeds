
// Name                 Syed Mohammed Arslan Kazmi
// Student ID           S2128998
// Programme of Study   BSc/BSc (Hons) Computing

package com.example.kazmi_arslan_s2128998;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WeatherRepository {
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    public interface DataFetchedCallback<T> {
        void onDataFetched(T data);
    }

    public void getForecast(String locationId, DataFetchedCallback<List<WeatherInfo>> callback) {
        executorService.execute(() -> {
            List<WeatherInfo> forecastData = fetchAndParseForecast(locationId);
            callback.onDataFetched(forecastData);
        });
    }

    public void getCurrentWeather(String locationId, DataFetchedCallback<WeatherInfo> callback) {
        executorService.execute(() -> {
            WeatherInfo currentWeatherData = fetchAndParseCurrentWeather(locationId);
            callback.onDataFetched(currentWeatherData);
        });
    }

    private List<WeatherInfo> fetchAndParseForecast(String locationId) { return null; }
    private WeatherInfo fetchAndParseCurrentWeather(String locationId) { return null; }

    public void shutdown() {
        executorService.shutdown();
    }
}
