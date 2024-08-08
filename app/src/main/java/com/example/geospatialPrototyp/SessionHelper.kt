package com.example.geospatialPrototyp

import android.app.Activity
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.geospatialPrototyp.geospatial.HelloGeoActivity
import com.example.geospatialPrototyp.helpers.CameraPermissionHelper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Session
import com.google.ar.core.VpsAvailability
import com.google.ar.core.exceptions.CameraNotAvailableException

// Klasse erstellt und konfigiert die Sitzung und dem Lebenszyklus der Sitzung
// Dieser kontrolliert anhand von überschriebenen Funktionen die Sitzung
// Vor der Erstellung der Sitzung wird der Status des ARCore Pakets geprüft
class SessionHelper(
    val activity: Activity,
    val features: Set<Session.Feature> = setOf(),



) : DefaultLifecycleObserver {
    // Wird true wenn ARCore Paket installiert ist
    var installRequested = false
    // Erste Sitzung wurde erstellt, ist aber noch leer. Sie ist nicht nutzbar
    var session: Session? = null
        private set

        //Abfrage, ob VPS-Signal erhaltbar ist
    private var fusedLocationClient: FusedLocationProviderClient? = null

    init {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity)
    }

    //DefaultLifecycleObserver überschreibt die Lifecyclesteps die wir unternehmen mit unserer Sitzung
    override fun onPause(owner: LifecycleOwner) {
        Log.i("Session", "Wird pausiert")
    session?.pause()
        Log.i("Session", "Wurde pausiert")
    }


    var exceptionCallback: ((Exception) -> Unit)? = null
    //Leere Funktion, welche in der MainActivity erstellt wird
    var beforeSessionResume:((Session) -> Unit)? = null



    private fun tryCreateSession(): Session? {
        // Erneute Abfrage nach der Kamera-Erlaubnis
        Log.i("ViewActivity","tryCreateSession")
        if (!CameraPermissionHelper.hasCameraPermission(activity)) {
            CameraPermissionHelper.requestCameraPermission(activity)
            return null
        }

        return try {
            // erneute Abfrage nach ARCore Status
            when (ArCoreApk.getInstance().requestInstall(activity, !installRequested)!!) {
                ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                    installRequested = true
                    // tryCreateSession will be called again, so we return null for now.
                    return null
                }

                ArCoreApk.InstallStatus.INSTALLED -> {
                    Log.i("Testen","In SessionHelper, APK ist installiert")
                }
            }
            //Erstellung einer Sitzung anhand der Aktivität und den benötigten Funktionen
            //VPS Check eingebaut
            Session(activity, features).also {
                this.session = it
                getLastLocation()
            }
        } catch (e: Exception) {
            exceptionCallback?.invoke(e)
            Log.i("Testen","In SessionHelper, APK ist nicht installiert und hat einen Fehler")
            null
        }
    }

    //Findet über fusedLocationClient die aktuellen Koordinaten des Endgeräts
    private fun getLastLocation() {
        try {
            fusedLocationClient
                ?.lastLocation
                ?.addOnSuccessListener { location ->
                    var latitude = 0.0
                    var longitude = 0.0
                    if (location != null) {
                        latitude = location.latitude
                        longitude = location.longitude
                    } else {
                        Log.e(
                            HelloGeoActivity.TAG,
                            "Error location is null"
                        )
                    }
                    session.let {
                        it?.let { it1 ->
                            checkVpsAvailability(
                                latitude, longitude,
                                it1
                            )
                        }
                    }
                }
        } catch (e: SecurityException) {
            Log.e(
                HelloGeoActivity.TAG,
                "No location permissions granted by User!"
            )
        }
    }

    //Testet die aktuelle Sitzung nach der Erreichbarkeit des VPS-Signals anhand der Koordinaten
    private fun checkVpsAvailability(latitude: Double, longitude: Double, session: Session) {
        session.checkVpsAvailabilityAsync(
            latitude,
            longitude
        ) { availability: VpsAvailability ->
            if (availability != VpsAvailability.AVAILABLE) {
                //Meldung, dass VPS nicht erreichbar ist.
                Log.i("VPS", "Nicht availabile ")
            }
        }
    }
    /**Sitzung wird wieder gestartet. Falls es keine Oncreate Funktion exisitiert, wird diese beim
     * ersten Aufruf genutzt */
    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)

        val session = this.session ?: tryCreateSession() ?: return
        try {

            beforeSessionResume?.invoke(session)

            session.resume()
            this.session = session
        } catch (e: CameraNotAvailableException) {
            exceptionCallback?.invoke(e)
        }
    }

    //Stoppt die aktuelle Sitzung
    override fun onDestroy(owner: LifecycleOwner) {

        session?.close()
        session = null
    }

}


