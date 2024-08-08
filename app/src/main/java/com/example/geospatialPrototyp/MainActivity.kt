package com.example.geospatialPrototyp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.geospatialPrototyp.geospatial.helpers.GeoPermissionsHelper
import com.example.geospatialPrototyp.helpers.CameraPermissionHelper.hasCameraPermission
import com.example.geospatialPrototyp.helpers.CameraPermissionHelper.requestCameraPermission
import com.google.ar.core.ArCoreApk
import com.google.ar.core.exceptions.UnavailableException


//Erste Aktivität. Start der App
open class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        /** Erster Aufruf der Standort Nutzung. Verlankt die Erlaubnis auf den Standort des Nutzers
         * zuzugreifen */
        GeoPermissionsHelper.requestPermissions(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!isARCoreSupportedAndUpToDate()) {
            //Check nach dem Status von ARCore im Endgerät
            Log.e("ARCoreSupport", "ARCore ist nicht unterstützt oder benötigt ein Update");
            finish(); // Stop the activity
        } else {
            // ARCore ist nicht unterstützt. Die weiteren Fehlermeldung geben mehr zum Status aus.
            Log.i("ARCoreSupport", "ARCore ist unterstützt und kann genutzt werden.");
        }

        //Abfrage nach der Kamera Erlaubnis
        if (!hasCameraPermission(this)) {
            requestCameraPermission(this)
        }else{
            Log.i("CheckUp", "hasCameraPermission()" + hasCameraPermission(this).toString())
        }


        // Erstellter Button, welcher zur Cameraview führt. Diese analysiert die Gegend nach geraden Oberflächen
        Log.i("Button","Vor der Erstellung")
        val fButton = findViewById<Button>(R.id.Fbutton)
        Log.i("Button","Nach der Erstellung" + fButton.isActivated.toString())

        Log.i("ARCoresupport","StartActivity")
        fButton.setOnClickListener{

            // Führt zur CameraView. Eine Test-Activity zur Erkennung von Oberflächen
            val intent = Intent(this, CameraViewActivity::class.java)
            Log.i("CheckUp","OnClick. StartActivity")
            startActivity(intent)
        }

        Log.i("Button","Vor der Erstellung")
        val geoButton = findViewById<Button>(R.id.Geobutton)
        Log.i("Button","Nach der Erstellung" + fButton.isActivated.toString())

        geoButton.setOnClickListener{
            Log.i("ARCoresupport","Geobutton gecklickt")
            // Führt zum öffentlichen Modus, um AR-Objekte darzustellen
            val intent = Intent( this, com.example.geospatialPrototyp.geospatial.HelloGeoActivity::class.java)
            Log.i("CheckUp","OnClick. StartActivity GeoActivity" )
            startActivity(intent)
        }
    }


    /**Funktion zum Prüfen nach dem Status von ARCore auf dem Gerät des Nutzers
     * Gibt true aus, wenn das Gerät AR-Funktionen unterstützt */
    private fun isARCoreSupportedAndUpToDate(): Boolean {
        return when (ArCoreApk.getInstance().checkAvailability(this)) {
            ArCoreApk.Availability.SUPPORTED_INSTALLED -> true
            ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD, ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED -> {
                try {
                    Log.i("ARCoreSupport", "ARCore installation requested im try")
                    // Request ARCore installation or update if needed.
                    when (ArCoreApk.getInstance().requestInstall(this, true)) {
                        ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                            Log.i("ARCoreSupport", "ARCore installation requested.")
                            false
                        }

                        ArCoreApk.InstallStatus.INSTALLED -> true
                    }
                } catch (e: UnavailableException) {
                    Log.e("ARCoreSupport", "ARCore ist nicht installiert", e)
                    false
                }
            }
            ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE ->
                // Gerät ünterstützt nicht AR.
                false
            ArCoreApk.Availability.UNKNOWN_ERROR -> {Log.e("ARCoreSupport","Es ist ein unkownError")
            false }
            ArCoreApk.Availability.UNKNOWN_CHECKING -> {Log.e("ARCoreSupport","Es ist ein Unknown_Checking")
                false }
            ArCoreApk.Availability.UNKNOWN_TIMED_OUT -> {Log.e("ARCoreSupport","Es ist ein Unkown_Timed_Out")
                false }
        }
    }
}