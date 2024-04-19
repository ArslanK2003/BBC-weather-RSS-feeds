
// Name                 Syed Mohammed Arslan Kazmi
// Student ID           S2128998
// Programme of Study   BSc/BSc (Hons) Computing

package com.example.kazmi_arslan_s2128998;

import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class WeatherInfo {
    private String title;
    private String description;
    private String pubDate; // This holds the entire date string from the feed
    private Date date; // This will hold the parsed Date object

    // Getters and setters for existing fields
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPubDate() {
        return pubDate;
    }

    public void setPubDate(String pubDate) {
        this.pubDate = pubDate;
        Log.d("WeatherInfo", "Received pubDate to parse: " + pubDate); // Confirming the received date string

        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT")); // Explicitly set the timezone to GMT

        try {
            Date parsedDate = format.parse(pubDate);
            Log.d("WeatherInfo", "Parsed date: " + parsedDate); // Logging the parsed date
            if (parsedDate != null) {
                this.date = parsedDate;
            } else {
                Log.e("WeatherInfo", "Parsed date is null, using current date as fallback");
                this.date = new Date(); // Use current date as a fallback
            }
        } catch (ParseException e) {
            Log.e("WeatherInfo", "Error parsing pubDate: " + pubDate, e);
            this.date = new Date(); // Use current date as a fallback
        }
    }


    // Method to get the day of the week from pubDate
    public String getDayOfWeek() {
        if (this.date == null) {
            return "";
        }
        SimpleDateFormat outputFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
        return outputFormat.format(this.date);
    }

    // Method to get a nicely formatted date string
    public String getFormattedDate() {
        if (this.date == null) {
            return "";
        }
        SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        return outputFormat.format(this.date);
    }

    // Method to increment the date by a certain number of days
    public Date incrementDay(int days) {
        if (this.date == null) {
            // Handle the null case, perhaps log an error or use a default date
            Log.e("WeatherInfo", "Date is null. Can't increment.");
            return new Date(); // Return the current date as a fallback
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(this.date);
        cal.add(Calendar.DATE, days);
        return cal.getTime();
    }

    public Date getDate() {
        if (this.date == null) {
            // Handle the null case, perhaps log an error or return null
            Log.e("WeatherInfo", "Date is null. Returning current date as fallback.");
            return new Date(); // Return the current date as a fallback
        }
        return this.date;
    }

}