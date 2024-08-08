package com.example.geospatialPrototyp

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.geospatialPrototyp.helpers.CameraPermissionHelper
import com.example.geospatialPrototyp.helpers.DepthSettings
import com.example.geospatialPrototyp.helpers.FullScreenHelper
import com.example.geospatialPrototyp.helpers.InstantPlacementSettings
import com.example.geospatialPrototyp.samplerender.SampleRender
import com.example.geospatialPrototyp.CameraRenderer
import com.example.geospatialPrototyp.CameraView
import com.example.geospatialPrototyp.MainActivity
import com.example.geospatialPrototyp.R
import com.example.geospatialPrototyp.SessionHelper
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableApkTooOldException
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import com.google.ar.core.exceptions.UnavailableSdkTooOldException
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class CameraViewActivity : MainActivity(){
    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 200
    }
    lateinit var SessionHelper: SessionHelper
    lateinit var view: CameraView
    lateinit var renderer: CameraRenderer
    private val db = Firebase.firestore
    /**Liste an manuell erstellten Datenmengen
     * Ankerdaten werden in Firebase Cloud Firestore hochgeladen beim Aufrufen dieser Funktion.
     * Die vorliegenden Daten sind Beispieldaten, welche zur Hochschule Mannheim führen
     * Die Klasse testet den Upload der Datenmengen an die Datenbank.
     */
    var anchorlist: MutableList<GeoPoint> = mutableListOf(GeoPoint(49.46940686781182, 8.483169049299951),
        GeoPoint(49.46933932478744, 8.482900157795122),
        GeoPoint(49.46927178166994, 8.482973248004663),
        GeoPoint(49.46943998564781, 8.482687592690306),
        GeoPoint(49.53855491787648, 8.473705922274378),
        GeoPoint(49.46949358420335, 8.48261919634743),
        GeoPoint(49.46946482401012, 8.483083218595167),
        GeoPoint(49.469374621476305, 8.483207941338055),
        GeoPoint(49.4705264012502, 8.481686702069437),
        GeoPoint(49.47051954105033, 8.481817605959188),
        GeoPoint(49.47095653386861, 8.481830274082558),
        GeoPoint(49.47085911979594, 8.48192422929375),
        GeoPoint(49.470577761300966, 8.481697276551879),
        GeoPoint(49.47099520862583, 8.481714040359078)
    )

    val instantPlacementSettings = InstantPlacementSettings()
    val depthSettings = DepthSettings()
    /**Erstellung eines Standard Quaternion für die Anker der Datenbank */
    private suspend fun uploadData(anchorlist: List<GeoPoint>, db: FirebaseFirestore){
        var namenszaehler:Int = 1
        for(points in anchorlist){
            val anchor1 = hashMapOf(
                "coordinates" to points,
                "q1" to 0F,
                "q2" to 0F,
                "q3" to 0F,
                "q4" to 1F,
            )
            try {
                db.collection("PrototypDatabase").document("Anchor$namenszaehler").set(anchor1).await()
                namenszaehler++
                Log.d("TestenUpload", "DocumentSnapshot erstellt!")
            } catch (e: Exception){
                Log.w("TestenUpload", "Fehler im Dokumentenindex $namenszaehler", e)
                break  // Beende die Schleife bei einem Fehler

            }



        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cameraview)
        /** Der vorliegende Code startet den Upload der Daten, nach dem ersten Aufruf der Aktivität
        * Außerhalb des Testens ist diese Funktion nicht nötig und wird nicht genutzt */

            /**
            CoroutineScope(Dispatchers.Main).launch {
                uploadData(anchorlist, db)
            }
            */



        // Setup ARCore session lifecycle helper and configuration.
        Log.i("ViewActivity","OnCreate 2 ")
        SessionHelper = SessionHelper(this)
        Log.i("ViewActivity","SessionHelper start")
        SessionHelper.exceptionCallback =
            { exception ->
                val message =
                    when (exception) {
                        is UnavailableUserDeclinedInstallationException ->
                            "Please install Google Play Services for AR"
                        is UnavailableApkTooOldException -> "Please update ARCore"
                        is UnavailableSdkTooOldException -> "Please update this app"
                        is UnavailableDeviceNotCompatibleException -> "This device does not support AR"
                        is CameraNotAvailableException -> "Camera not available. Try restarting the app."
                        else -> "Failed to create AR session: $exception"
                    }
                Log.e("CheckUp", "ARCore threw an exception", exception)
            }
        // Configure session features, including: Lighting Estimation, Depth mode, Instant Placement.
        SessionHelper.beforeSessionResume = ::configureSession
        lifecycle.addObserver(SessionHelper)

        // Set up the Hello AR renderer.
        renderer = CameraRenderer(this)
        lifecycle.addObserver(renderer)

        // Set up Hello AR UI.
        view = CameraView(this)
        lifecycle.addObserver(view)
        setContentView(view.root)

        // Sets up an example renderer using our HelloARRenderer.
        SampleRender(view.surfaceView, renderer, assets)

        depthSettings.onCreate(this)
        instantPlacementSettings.onCreate(this)


    }
    private fun configureSession(session: Session) {
        session.configure(
            session.config.apply {
                // Einstellung des Lichtmodus. Erkennung der Beleuchtung des AR-Objektes
                lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR

                // Abfrage ob die Depth API vom Endgerät unterstützt wird
                depthMode =
                    if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                        Config.DepthMode.AUTOMATIC
                    } else {
                        Config.DepthMode.DISABLED
                    }

                // Abfrage ob Instantplacement aktiviert werden kann.
                // Instantplacement erlaubt die direkte Position eines AR-Gebäudes bei Berührung
                instantPlacementMode =
                    if (instantPlacementSettings.isInstantPlacementEnabled) {
                        Config.InstantPlacementMode.LOCAL_Y_UP
                    } else {
                        Config.InstantPlacementMode.DISABLED
                    }
            }
        )
    }
    // Von Google Developers freigegebener Code
    // Testet die vergebenen Zugangsberechtigungen der App
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        results: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, results)
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            // Use toast instead of snackbar here since the activity will exit.
            Toast.makeText(this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
                .show()
            if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                CameraPermissionHelper.launchPermissionSettings(this)
            }
            finish()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus)
    }




}