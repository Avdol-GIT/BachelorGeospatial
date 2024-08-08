package com.example.geospatialPrototyp.geospatial;

import com.google.firebase.firestore.GeoPoint;
//Klasse zum Verwalten der erhaltenen Datensätze der Datenbank
public class AnchorData {
    private final GeoPoint coordinates;
    private final float q1;
    private final float q2;
    private final float q3;
    private final float q4;
    public AnchorData(GeoPoint coordinates,float q1, float q2, float q3, float q4){
        this.coordinates = coordinates;
        this.q1 = q1;
        this.q2 = q2;
        this.q3 = q3;
        this.q4 = q4;
    }
    public GeoPoint getCoordinates(){ return coordinates;}

    public float getQ1(){ return q1;}
    public float getQ2(){ return q2;}
    public float getQ3(){ return q3;}
    public float getQ4(){ return q4;}

}
