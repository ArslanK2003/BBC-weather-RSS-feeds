
// Name                 Syed Mohammed Arslan Kazmi
// Student ID           S2128998
// Programme of Study   BSc/BSc (Hons) Computing

package com.example.kazmi_arslan_s2128998;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import java.io.StringReader;
import java.util.ArrayList;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;


import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.util.HashMap;

import android.app.AlertDialog;
import android.widget.ListView;
import java.util.List;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Calendar;

import java.util.function.Consumer;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Date;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import android.content.pm.PackageManager;
import androidx.annotation.NonNull;

import androidx.lifecycle.ViewModelProvider;
import java.util.stream.Collectors;

import androidx.lifecycle.Observer;

public class MainActivity extends AppCompatActivity implements OnClickListener, OnMapReadyCallback {
    private TextView rawDataDisplay;
    private String urlSource = "https://weather-broker-cdn.api.bbci.co.uk/en/forecast/rss/3day/";
    private ArrayList<WeatherInfo> weatherList = new ArrayList<>();
    private StringBuilder result = new StringBuilder();
    private Spinner locationSpinner;
    private HashMap<String, String> locationIds = new HashMap<>();
    private WeatherInfo currentWeatherInfo;
    ArrayAdapter<String> forecastAdapter;
    ListView forecastListView;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    private String currentLocationId = "";
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationClient;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private HashMap<String, LatLng> locationCoordinates;
    private static final float DEFAULT_ZOOM = 21.0f;
    private WeatherViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeLocationCoordinates();
        initializeViews();
        initializeLocationIds();
        setupSpinner();
        // Set up the raw links to the graphical components
        rawDataDisplay = (TextView) findViewById(R.id.rawDataDisplay);
        result = new StringBuilder();
        Spinner locationSpinner = findViewById(R.id.location_spinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>(locationIds.keySet()));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        locationSpinner.setAdapter(adapter);

        forecastAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        forecastListView = findViewById(R.id.forecastListView);
        forecastListView.setAdapter(forecastAdapter);
        forecastListView.setOnItemClickListener((parent, view, position, id) -> showForecastDetailDialog(weatherList.get(position)));

        long initialDelay = calculateInitialDelay(8, 0); // For 8 AM update
        long interval = TimeUnit.HOURS.toMillis(12); // 12 hours interval for 8 PM update

        scheduledExecutorService.scheduleAtFixedRate(() -> {
            //'currentLocationId' holds the last selected location ID
            fetchWeatherData(currentLocationId);
        }, initialDelay, interval, TimeUnit.MILLISECONDS);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        // Set up ViewModel and LiveData observation
        viewModel = new ViewModelProvider(this).get(WeatherViewModel.class);
        viewModel.getWeatherInfoList().observe(this, new Observer<List<WeatherInfo>>() {
            @Override
            public void onChanged(List<WeatherInfo> weatherInfoList) {
                updateForecastList(weatherInfoList);
            }
        });

        viewModel.getCurrentWeather().observe(this, new Observer<WeatherInfo>() {
            @Override
            public void onChanged(WeatherInfo weather) {
                updateCurrentWeather(weather);
            }
        });

        viewModel.fetchWeatherData("2648579"); // Fetch initial data

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        // Initialize TextViews
        locationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedLocation = parent.getItemAtPosition(position).toString();
                LatLng coordinates = locationCoordinates.get(selectedLocation);                currentLocationId = locationIds.get(selectedLocation); // Save current location ID
                fetchWeatherData(currentLocationId); // Fetch weather data for selected location
                updateMap(); // Update map markers and camera position
                if (coordinates != null && mMap != null) {
                    mMap.clear(); // Clears all markers
                    mMap.addMarker(new MarkerOptions().position(coordinates).title(selectedLocation)); // Add new marker
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(coordinates, 10)); // Move camera to new position
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //Handle the case where nothing is selected
            }
        });
    }

    private long calculateInitialDelay(int targetHour, int targetMinute) {
        Calendar nextUpdate = Calendar.getInstance();
        nextUpdate.set(Calendar.HOUR_OF_DAY, targetHour);
        nextUpdate.set(Calendar.MINUTE, targetMinute);
        nextUpdate.set(Calendar.SECOND, 0);
        nextUpdate.set(Calendar.MILLISECOND, 0);

        if (nextUpdate.getTimeInMillis() < System.currentTimeMillis()) {
            nextUpdate.add(Calendar.DAY_OF_MONTH, 1);
        }

        return nextUpdate.getTimeInMillis() - System.currentTimeMillis();
    }

    private void initializeViews() {
        rawDataDisplay = findViewById(R.id.rawDataDisplay);
        locationSpinner = findViewById(R.id.location_spinner);
        forecastListView = findViewById(R.id.forecastListView);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
        scheduledExecutorService.shutdown();
    }

    // Update the ListView with the forecast data
    private void updateForecastList(List<WeatherInfo> weatherInfoList) {
        if (weatherInfoList != null) {
            ArrayAdapter<String> forecastAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_list_item_1,
                    weatherInfoList.stream().map(WeatherInfo::getTitle).collect(Collectors.toList()));
            forecastListView.setAdapter(forecastAdapter);
        } else {
            // Handle the case where weatherInfoList is null
            forecastAdapter.clear();
            forecastAdapter.notifyDataSetChanged();
            // Log the error
            Log.e("MainActivity", "Weather info list is null");
        }
    }


    // Update the TextView with the current weather data
    private void updateCurrentWeather(WeatherInfo weather) {
        if (weather != null) {
            rawDataDisplay.setText(String.format("%s\n%s", weather.getTitle(), weather.getDescription()));
        } else {
            rawDataDisplay.setText(getString(R.string.no_weather_data));
        }
    }
    private void initializeLocationIds() {
        locationIds.put("Glasgow", "2648579");
        locationIds.put("London", "2643743");
        locationIds.put("New York", "5128581");
        locationIds.put("Oman", "287286");
        locationIds.put("Mauritius", "934154");
        locationIds.put("Bangladesh", "1185241");
    }

    private void initializeLocationCoordinates() {
        locationCoordinates = new HashMap<>();
        locationCoordinates.put("Glasgow", new LatLng(55.86683929366823, -4.249948118712544));
        locationCoordinates.put("London", new LatLng(51.51843851349916, -0.07223486798482338));
        locationCoordinates.put("New York", new LatLng(40.723501654257596, -74.00167535937399));
        locationCoordinates.put("Bangladesh", new LatLng(23.872652457514505, 90.36880496612018));
        locationCoordinates.put("Mauritius", new LatLng(-20.297092632019723, 57.5834311936153));
        locationCoordinates.put("Oman", new LatLng(23.640950675998013, 58.218872582222566));
    }

    private void setupSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>(locationIds.keySet()));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        locationSpinner.setAdapter(adapter);
        locationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedLocation = parent.getItemAtPosition(position).toString();
                fetchWeatherData(locationIds.get(selectedLocation)); // Fetch weather data for selected location

                LatLng coordinates = locationCoordinates.get(selectedLocation);
                if (coordinates != null) {
                    moveCameraToLocation(coordinates, 10);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //Handle the case where nothing is selected
            }
        });
    }

    private void moveCameraToLocation(LatLng coordinates, float zoomLevel) {
        if (mMap != null && coordinates != null) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(coordinates, zoomLevel));
        }
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        // Ensure the initial location is Glasgow
        LatLng initialLocationCoordinates = locationCoordinates.get("Glasgow");
        if (initialLocationCoordinates != null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(initialLocationCoordinates, DEFAULT_ZOOM));
        }
        updateMap(); // This will clear and re-add markers based on updated logic.
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                updateMap();
            } else {
                // Handle the case where the user denies the permissions.
            }
        }
    }

    private void updateMap() {
        if (mMap != null) {
            mMap.clear(); // Clear old markers

            for (String locationName : locationCoordinates.keySet()) {
                LatLng coordinates = locationCoordinates.get(locationName);
                if (coordinates != null) {
                    mMap.addMarker(new MarkerOptions().position(coordinates).title(locationName));
                }
            }

            // Re-center to the current selected location with specified zoom
            if (currentLocationId != null && !currentLocationId.isEmpty()) {
                LatLng currentCoordinates = locationCoordinates.get(currentLocationId);
                if (currentCoordinates != null) {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentCoordinates, DEFAULT_ZOOM));
                }
            }
        }
    }



    private void fetchWeatherData(String locationId) {
        // Clear the old data to ensure UI is clean before updating
        weatherList.clear();
        currentWeatherInfo = null;

        String forecastUrl = urlSource + locationId;
        String observationsUrl = "https://weather-broker-cdn.api.bbci.co.uk/en/observation/rss/" + locationId;

        // Fetch and parse forecast data
        fetchAndParseData(forecastUrl, this::parseForecastData);

        // Fetch and parse current weather data
        fetchAndParseData(observationsUrl, this::parseObservationsData);
    }

    private void fetchAndParseData(String urlString, Consumer<String> parser) {
        executorService.submit(() -> {
            try {
                URL url = new URL(urlString);
                URLConnection yc = url.openConnection();
                BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                // Use the parser passed as a parameter
                parser.accept(response.toString());
            } catch (IOException e) {
                Log.e("MyTag", "IO Exception", e);
            }
        });
    }

    private void parseForecastData(String data) {
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();

            xpp.setInput(new StringReader(data));
            int eventType = xpp.getEventType();
            WeatherInfo weatherInfo = null;

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if ("item".equalsIgnoreCase(xpp.getName())) {
                        weatherInfo = new WeatherInfo();
                    } else if (weatherInfo != null) {
                        switch (xpp.getName().toLowerCase()) {
                            case "title":
                                weatherInfo.setTitle(xpp.nextText().trim());
                                break;
                            case "description":
                                weatherInfo.setDescription(xpp.nextText().trim());
                                break;
                            case "pubdate":
                                weatherInfo.setPubDate(xpp.nextText().trim());
                                break;
                        }
                    }
                } else if (eventType == XmlPullParser.END_TAG && "item".equalsIgnoreCase(xpp.getName())) {
                    if (weatherInfo != null) {
                        weatherList.add(weatherInfo);
                    }
                }
                eventType = xpp.next();
            }

            // After parsing, update UI on the main thread
            runOnUiThread(this::updateUI);

        } catch (XmlPullParserException | IOException e) {
            Log.e("MyTag", "Parsing error", e);
        }
    }

    private void showForecastDetailDialog(WeatherInfo weatherInfo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(weatherInfo.getTitle());
        builder.setMessage(weatherInfo.getDescription());
        builder.setPositiveButton("OK", null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }


    public void onClick(View v) {
        startProgress();
    }

    public void startProgress() {
        // Run network access on a separate thread;
        new Thread(new Task(urlSource, true)).start();
    }

    private class Task implements Runnable {
        private String url;
        private boolean isForecast;

        public Task(String url, boolean isForecast) {
            this.url = url;
            this.isForecast = isForecast;
        }

        @Override
        public void run() {
            StringBuilder result = new StringBuilder();
            try {
                URL aurl = new URL(url);
                URLConnection yc = aurl.openConnection();
                BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    result.append(inputLine);
                }
                in.close();
            } catch (IOException ae) {
                Log.e("MyTag", "IOException occurred", ae);
            }

            // Clear the weatherList and parse forecast data
            if (isForecast) {
                // Clear existing data to ensure the list contains only the most recent forecast
                weatherList.clear();
                parseForecastData(result.toString());
            } else {
                parseObservationsData(result.toString());
            }

            // After parsing the data (forecast or current observations), trigger the UI update
            MainActivity.this.runOnUiThread(() -> updateUI());
        }

    }

    private void parseObservationsData(String data) {
        WeatherInfo currentWeather = new WeatherInfo();
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();

            xpp.setInput(new StringReader(data));
            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && "item".equalsIgnoreCase(xpp.getName())) {
                    // Parsing within the <item> tag
                    while (!(eventType == XmlPullParser.END_TAG && "item".equalsIgnoreCase(xpp.getName()))) {
                        if (eventType == XmlPullParser.START_TAG) {
                            String tagName = xpp.getName();
                            if ("title".equalsIgnoreCase(tagName)) {
                                currentWeather.setTitle(xpp.nextText().trim());
                            } else if ("description".equalsIgnoreCase(tagName)) {
                                currentWeather.setDescription(xpp.nextText().trim());
                            }
                        }
                        eventType = xpp.next();
                    }
                }
                eventType = xpp.next();
            }
        } catch (XmlPullParserException | IOException e) {
            Log.e("MyTag", "Parsing error", e);
        }

        // Set the currentWeatherInfo to the parsed data
        this.currentWeatherInfo = currentWeather;

        // Since this is a network operation, trigger the UI update on the UI thread
        runOnUiThread(this::updateUI);
    }


    private void updateUI() {
        runOnUiThread(() -> {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            SimpleDateFormat dayOfWeekFormat = new SimpleDateFormat("EEEE", Locale.getDefault()); // Added this line

            if (currentWeatherInfo != null && currentWeatherInfo.getTitle() != null && !currentWeatherInfo.getTitle().isEmpty()) {
                String dayOfWeek = dayOfWeekFormat.format(currentWeatherInfo.getDate());
                String date = dateFormat.format(currentWeatherInfo.getDate());

                String currentWeatherDetails = currentWeatherInfo.getTitle() + "\n" +
                        currentWeatherInfo.getDescription() + "\n" +
                        dayOfWeek + " - " + date;
                rawDataDisplay.setText(currentWeatherDetails);
            } else {
                rawDataDisplay.setText("No current weather data available.");
            }

            forecastAdapter.clear();
            List<String> titles = new ArrayList<>();
            for (int i = 0; i < weatherList.size(); i++) {
                WeatherInfo info = weatherList.get(i);
                if (info.getDate() != null) { // Check for null before formatting
                    Date forecastDate = info.incrementDay(i);
                    String formattedDate = dateFormat.format(forecastDate);
                    String titleWithDate = info.getTitle() + " - " + formattedDate;
                    titles.add(titleWithDate);
                }
            }

            forecastAdapter.addAll(titles);
            forecastAdapter.notifyDataSetChanged();
        });
    }
} 