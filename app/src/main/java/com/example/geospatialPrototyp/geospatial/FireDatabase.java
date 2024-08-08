package com.example.geospatialPrototyp.geospatial;

import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;


public class FireDatabase {



    public GeoPoint Location;
    public AnchorData leerAnker;
    public Double q1;
    public Double q2;
    public Double q3;
    public Double q4;
    public String databaseName = "PrototypDatabase"; //Name der zu nutzenden Dokumenten-Collection

    public interface fireStoreCallbackAnker {
        void onCallback(AnchorData currentAnker);
    }
    private final FirebaseFirestore fullDB = FirebaseFirestore.getInstance();
    public void getAllAnchor(fireStoreCallbackAnker callback) {

        Log.i("Testen", "Start von getAnchor");
        /** Wähle die genutzte Collection der Firebase Cloud FireStore.
          * Aktuell nutzt es eine Datenbank,
          * welche den öffentlichen Modus darstellt */
        fullDB.collection(databaseName)
                //Erhalte die Collection
                .get()
                //Der folgende Code wird ausgeführt, nachdem die App die Daten erhalten hat.
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        //QueryDocumentSnapshot ist eine Query, welche die Daten der Collection besitzt
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Log.d("Database", "Aktuell wird: " + document.getId() + " genutzt.");
                            //Speichern der Daten anhand deren Feldnamen
                            Location = document.getGeoPoint("coordinates");
                            q1 = document.getDouble("q1");
                            q2 = document.getDouble("q2");
                            q3 = document.getDouble("q3");
                            q4 = document.getDouble("q4");
                            Log.d("Database", "Die gespeicherten Daten sind: " + Location.toString() + " - " + q1 + " - " + q2 + " - " + q3 + " - " + q4);
                            /** Ein Objekt der Klasse AnchorData wird mit den zuvor gespeicherten Daten erstellt.
                             * Mit diesem Objekt wird ein raumbezogener Anker erstellt
                             * leerAnker ist ein Objekt der Klasse AnchorData, welche zum Verwalten der Daten aus der Datenbank erstellt wurde */
                            leerAnker = new AnchorData(document.getGeoPoint("coordinates"),
                                    q1.floatValue(), q2.floatValue(), q3.floatValue(), q4.floatValue());
                            try {
                                //Dieser Callback ist in HelloGeoRenderer definiert.
                                callback.onCallback(leerAnker);
                            } catch (NullPointerException e) {
                                Log.e("Fehler", "NullPointerException aufgetreten beim Aufrufen des callbacks mit dem Anker: " + leerAnker.toString(), e);
                                System.exit(1);
                            }
                        }
                    } else {
                        Log.d("Datenbank-Fehler", "Error getting documents: ", task.getException());
                    }
                });
    }
    }

