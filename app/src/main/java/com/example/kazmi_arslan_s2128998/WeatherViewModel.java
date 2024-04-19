
// Name                 Syed Mohammed Arslan Kazmi
// Student ID           S2128998
// Programme of Study   BSc/BSc (Hons) Computing

package com.example.kazmi_arslan_s2128998;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import java.util.List;

public class WeatherViewModel extends ViewModel {
    private final MutableLiveData<List<WeatherInfo>> weatherInfoList = new MutableLiveData<>();
    private final MutableLiveData<WeatherInfo> currentWeather = new MutableLiveData<>();
    private final WeatherRepository weatherRepository;

    public WeatherViewModel() {
        weatherRepository = new WeatherRepository();
    }

    public LiveData<List<WeatherInfo>> getWeatherInfoList() {
        return weatherInfoList;
    }

    public LiveData<WeatherInfo> getCurrentWeather() {
        return currentWeather;
    }

    // Fetch weather data and update LiveData
    public void fetchWeatherData(String locationId) {
        weatherRepository.getForecast(locationId, new WeatherRepository.DataFetchedCallback<List<WeatherInfo>>() {
            @Override
            public void onDataFetched(List<WeatherInfo> data) {
                weatherInfoList.postValue(data);
            }
        });

        weatherRepository.getCurrentWeather(locationId, new WeatherRepository.DataFetchedCallback<WeatherInfo>() {
            @Override
            public void onDataFetched(WeatherInfo data) {
                currentWeather.postValue(data);
            }
        });
    }
}