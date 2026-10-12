package com.example.parktag;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "parking_locations")
public class ParkingLocation {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private double latitude;
    private double longitude;

    @ColumnInfo(name = "timestamp")
    private String timestamp;

    @ColumnInfo(name = "notes")
    private String notes;

    // Constructor
    public ParkingLocation(double latitude, double longitude, String timestamp, String notes) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.notes = notes;
    }

    // Getters
    public int getId() { return id; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getTimestamp() { return timestamp; }
    public String getNotes() { return notes; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public void setNotes(String notes) { this.notes = notes; }
}
