/*
 * Copyright 2022 Google L LC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.geospatialPrototyp.geospatial

import android.annotation.SuppressLint
//import android.graphics.Shader
import android.opengl.Matrix
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.geospatialPrototyp.helpers.DisplayRotationHelper
import com.example.geospatialPrototyp.helpers.TrackingStateHelper
import com.example.geospatialPrototyp.samplerender.Framebuffer
import com.example.geospatialPrototyp.samplerender.Mesh
import com.example.geospatialPrototyp.samplerender.SampleRender
import com.example.geospatialPrototyp.samplerender.Shader
import com.example.geospatialPrototyp.samplerender.Texture
import com.example.geospatialPrototyp.samplerender.arcore.BackgroundRenderer
import com.google.android.gms.maps.model.LatLng
import com.google.ar.core.Anchor
import com.google.ar.core.FutureState
import com.google.ar.core.Pose
import com.google.ar.core.ResolveAnchorOnRooftopFuture
import com.google.ar.core.ResolveAnchorOnTerrainFuture
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
//import com.google.ar.sceneform.rendering.Texture
import java.io.IOException
import java.util.Timer
import kotlin.concurrent.schedule


/**
 * Bestandteile der Klasse wurden aus dem Programm codelab-geospatial genommen
 * Weitere Informationen finden Sie hier: https://github.com/google-ar/codelab-geospatial
 * Englische Kommentare vor einem Codeblock sind Bestandteil von codelab-geospatial und markieren
 * den Code als diesen
 */
class HelloGeoRenderer(val activity: HelloGeoActivity) :
    SampleRender.Renderer, DefaultLifecycleObserver {
    companion object {
        val TAG = "HelloGeoRenderer"

        private val Z_NEAR = 0.1f
        private val Z_FAR = 1000f
    }

    lateinit var backgroundRenderer: BackgroundRenderer
    lateinit var virtualSceneFramebuffer: Framebuffer
    var hasSetTextureNames = false

    // Virtual object (ARCore pawn)
    lateinit var virtualObjectMesh: Mesh
    lateinit var virtualObjectShader: Shader
    lateinit var virtualObjectShaderDach: Shader
    lateinit var virtualObjectTexture: Texture
    lateinit var virtualObjectTextureDach: Texture


    // Temporary matrix allocated here to reduce number of allocations for each frame.
    val modelMatrix = FloatArray(16)
    val viewMatrix = FloatArray(16)
    val projectionMatrix = FloatArray(16)
    val modelViewMatrix = FloatArray(16) // view x model

    val modelViewProjectionMatrix = FloatArray(16) // projection x view x model

    val session
        get() = activity.SessionHelper.session

    val displayRotationHelper = DisplayRotationHelper(activity)
    val trackingStateHelper = TrackingStateHelper(activity)


    var qTest: ResolveAnchorOnTerrainFuture? = null
    var qTest2: ResolveAnchorOnTerrainFuture? = null
    var qTest3: ResolveAnchorOnTerrainFuture? = null
    var qTest4: ResolveAnchorOnTerrainFuture? = null
    var onlyanchor: MutableList<Anchor>? = mutableListOf()
    var dachAnchor: ResolveAnchorOnRooftopFuture? = null
    val timer = Timer()
    var wartendatabase = true
    var Screentimer: Long? = null
    var wartendraw = true
    var ondrawtimer: Long = 0
    val Database = FireDatabase()
    var databaseCheck = true

    /**Wird aufgerufen, falls die Klasse aufgerufen wird. Es exisitert keine onStart Funktion,
    *  onResume wird beim ersten Aufruf der Klasse genutzt
    */
    @SuppressLint("SuspiciousIndentation")
    override fun onResume(owner: LifecycleOwner) {
        Screentimer = System.currentTimeMillis()
        displayRotationHelper.onResume()
        hasSetTextureNames = false
        // Earth wird erstellt, um die Anker zu erstellen
        val earth = session?.earth

        //Aufruf der Ankerdaten in Firebase Cloud Firestore
        Database.getAllAnchor {
            //Erstellung eines Geländeankers mit den erhaltenen Daten
            earth?.resolveAnchorOnTerrainAsync(
                it.coordinates.latitude,   //Angabe des Breitengrads
                it.coordinates.longitude,  //Angabe des Längengrads
                0.0,      //Angabe der Höhe über dem Gelände
                it.q1,                     //Wert des Quaternion für X
                it.q2,                     //Wert des Quaternion für Y
                it.q3,                     //Wert des Quaternion für Z
                it.q4                      //Wert des Quaternion für W
            ) { anchor, state ->
                //Nachdem der Anker erfolgreich erstellt wurde, wird dieser Code ausgeführt
                if (state == Anchor.TerrainAnchorState.SUCCESS) {
                    //Speichern des reinen Ankers innerhalb einer MutableList / ArrayList
                    onlyanchor?.add(anchor)
                    Log.i(
                        "Test",
                        "Wie groß ist gerade onlyanchor? :   " + onlyanchor?.size.toString()
                    )
                } else {
                    // Der Anker wurde nicht erfolgreich erstellt
                    Log.i(
                        "AnchorState",
                        "Nicht Trackable! Keine erfolgreiche Erstellung des DatanbankAnkers"
                    )
                }
            }

        }

        /**Erstellung eines Geländeankers mit Daten innerhalb der App, zum Testen der Performance
         * und dem Verhalten der Modelle bei unterschiedlichen Quaternions */

        qTest =
            earth?.resolveAnchorOnTerrainAsync(
                /** Genutzte Koordinaten für Abbildung 4.2 in der Thesis */
                49.46945512282578, 8.48340970181298,
                0.0,
                0f,
                0f,
                0f,
                1f
            ) { anchor, state ->
                if (state == Anchor.TerrainAnchorState.SUCCESS) {
                    // Nach erfolgreicher Erstellung des Ankers wird dieser Code ausgeführt
                    Log.i("Funktion", "Der Anker qTest funktioniert und kann dargestellt werden")
                    Log.i("PerformanceTest","Es dauerte ungefähr für q1: " + (Screentimer!!.minus(System.currentTimeMillis())) + " Milli für die Darstellung in onResume")
                } else {
                    // Falls die Erstellung des Ankers fehlschlägt, wird dieser Code ausgeführt
                    Log.i(
                        "Funktion",
                        "Der Anker qTest funktioniert nicht und kann noch nicht dargestellt werden"
                    )
                }
            }
        qTest2 =
            earth?.resolveAnchorOnTerrainAsync(
                /** Genutzte Koordinaten für Abbildung 4.2 in der Thesis */
                49.469489834974624, 8.483412328798634,
                0.0,
                0f,
                1f,
                2f,
                0.5f
            ) { anchor, state ->
                if (state == Anchor.TerrainAnchorState.SUCCESS) {
                    // do something with the anchor here
                    Log.i("Funktion", "Der Anker future funktioniert und kann dargestellt werden")
                } else {
                    // the anchor failed to resolve
                    Log.i(
                        "Funktion",
                        "Der Anker future funktioniert nicht und kann noch nicht dargestellt werden"
                    )
                }
            }
        qTest3 =
            earth?.resolveAnchorOnTerrainAsync(
                /** Genutzte Koordinaten für Abbildung 4.2 in der Thesis */
                49.46955186154001, 8.483397442546586,
                /** Genutzte Koordinaten für Abbildung 6.1 in der Thesis:
                 * 4.9.469489834974624, 8.483412328798634, */
                0.0,
                0.2f,
                0f,
                0f,
                0.7f
            ) { anchor, state ->
                if (state == Anchor.TerrainAnchorState.SUCCESS) {
                    // do something with the anchor here
                    Log.i("Funktion", "Der Anker future funktioniert und kann dargestellt werden")
                } else {
                    // the anchor failed to resolve
                    Log.i(
                        "Funktion",
                        "Der Anker future funktioniert nicht und kann noch nicht dargestellt werden"
                    )
                }
            }
        qTest4 =
            earth?.resolveAnchorOnTerrainAsync(
                /** Genutzte Koordinaten für Abbildung 4.2 in der Thesis */
                49.46959852367506, 8.483395691222817,
                /** Genutzte Koordinaten für Abbildung 6.1 in der Thesis:
                 * 4.9.469489834974624, 8.483412328798634, */
                0.0,
                5.4f,
                3f,
                2.4f,
                0.6f
            ) { anchor, state ->
                if (state == Anchor.TerrainAnchorState.SUCCESS) {
                    // do something with the anchor here
                    Log.i("Funktion", "Der Anker future funktioniert und kann dargestellt werden")
                } else {
                    // the anchor failed to resolve
                    Log.i(
                        "Funktion",
                        "Der Anker future funktioniert nicht und kann noch nicht dargestellt werden"
                    )
                }
            }

        //Erstellung eines Dachankers zum Testen dessens Verhalten
        dachAnchor = earth?.resolveAnchorOnRooftopAsync(
            /** Genutzte Koordinaten für Abbildung 4.3 in der Thesis */
            49.4688259, 8.4822948,
            0.0,
            0f,
            0f,
            0f,
            1f
        ) { anchor, state ->
            if (state == Anchor.RooftopAnchorState.SUCCESS) {
                // do something with the anchor here
                Log.i(
                    "AnchorState",
                    "TRACKABLE! Erfolgreiche Erstellung eines Dach Ankers"
                )
            } else {
                // the anchor failed to resolve
                Log.i(
                    "AnchorState",
                    "Nicht Trackable! Fehlgeschlagene Erstellung eines Dach Ankers!"
                )
            }
        }
}

    override fun onPause(owner: LifecycleOwner) {
        displayRotationHelper.onPause()
    }

    //Methode wurde ohne Veränderungen vom Projekt codelab_geospatial übernommen
    override fun onSurfaceCreated(render: SampleRender) {
        // Prepare the rendering objects.
        // This involves reading shaders and 3D model files, so may throw an IOException.
        try {
            backgroundRenderer = BackgroundRenderer(render)
            virtualSceneFramebuffer = Framebuffer(render, /*width=*/ 1, /*height=*/ 1)

            // Virtual object to render (Geospatial Marker)
            // Implementierung des 3D-Modells
            virtualObjectTexture =
                Texture.createFromAsset(
                    render,
                    "models/cat/Cat_diffuse.jpg",
                    Texture.WrapMode.CLAMP_TO_EDGE,
                    Texture.ColorFormat.SRGB
                )
            virtualObjectTextureDach =
                Texture.createFromAsset(
                    render,
                    "models/cat/Cat_bump.jpg",
                    Texture.WrapMode.CLAMP_TO_EDGE,
                    Texture.ColorFormat.SRGB
                )
            //virtualObjectMesh = Mesh.createFromAsset(render, "models/geospatial_marker.obj")
            virtualObjectMesh = Mesh.createFromAsset(render, "models/cat/Cat_model.obj");
            virtualObjectShader =
                Shader.createFromAssets(
                    render,
                    "shaders/ar_unlit_object.vert",
                    "shaders/ar_unlit_object.frag",
                    /*defines=*/ null
                )
                    .setTexture("u_Texture", virtualObjectTexture)
            virtualObjectShaderDach =
                Shader.createFromAssets(
                    render,
                    "shaders/ar_unlit_object.vert",
                    "shaders/ar_unlit_object.frag",
                    /*defines=*/ null
                )
                    .setTexture("u_Texture", virtualObjectTextureDach)

            backgroundRenderer.setUseDepthVisualization(render, false)
            backgroundRenderer.setUseOcclusion(render, false)
        } catch (e: IOException) {
            Log.e(TAG, "Failed to read a required asset file", e)
            showError("Failed to read a required asset file: $e")
        }
    }

    //Methode wurde ohne Veränderungen vom Projekt codelab_geospatial übernommen
    override fun onSurfaceChanged(render: SampleRender, width: Int, height: Int) {
        displayRotationHelper.onSurfaceChanged(width, height)
        virtualSceneFramebuffer.resize(width, height)
    }
    //</editor-fold>





    @SuppressLint("SuspiciousIndentation")
    override fun onDrawFrame(render: SampleRender) {
        val session = session ?: return
        if (wartendraw) {
            ondrawtimer = System.currentTimeMillis()
            wartendraw = false
        }

        //<editor-fold desc="ARCore frame boilerplate" defaultstate="collapsed">
        // Texture names should only be set once on a GL thread unless they change. This is done during
        // onDrawFrame rather than onSurfaceCreated since the session is not guaranteed to have been
        // initialized during the execution of onSurfaceCreated.
        if (!hasSetTextureNames) {
            session.setCameraTextureNames(intArrayOf(backgroundRenderer.cameraColorTexture.textureId))
            hasSetTextureNames = true
        }

        // -- Update per-frame state

        // Notify ARCore session that the view size changed so that the perspective matrix and
        // the video background can be properly adjusted.
        displayRotationHelper.updateSessionIfNeeded(session)

        // Obtain the current frame from ARSession. When the configuration is set to
        // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
        // camera framerate.
        val frame =
            try {
                session.update()
            } catch (e: CameraNotAvailableException) {
                Log.e(TAG, "Camera not available during onDrawFrame", e)
                showError("Camera not available. Try restarting the app.")
                return
            }

        val camera = frame.camera

        // BackgroundRenderer.updateDisplayGeometry must be called every frame to update the coordinates
        // used to draw the background camera image.
        backgroundRenderer.updateDisplayGeometry(frame)

        // Keep the screen unlocked while tracking, but allow it to lock when tracking stops.
        trackingStateHelper.updateKeepScreenOnFlag(camera.trackingState)

        // -- Draw background
        if (frame.timestamp != 0L) {
            // Suppress rendering if the camera did not produce the first frame yet. This is to avoid
            // drawing possible leftover data from previous sessions if the texture is reused.
            backgroundRenderer.drawBackground(render)
        }

        // If not tracking, don't draw 3D objects.
        if (camera.trackingState == TrackingState.PAUSED) {
            return
        }

        // Get projection matrix.
        camera.getProjectionMatrix(projectionMatrix, 0, Z_NEAR, Z_FAR)

        // Get camera matrix and draw.
        camera.getViewMatrix(viewMatrix, 0)

        render.clear(virtualSceneFramebuffer, 0f, 0f, 0f, 0f)
        //</editor-fold>

        // Funktion zur Darstellung der Position des Nutzers, basierend auf das Projekt codelab_geospatial
        val earth = session.earth
        if (earth?.trackingState == TrackingState.TRACKING) {
            val cameraGeospatialPose =
            //Aktuelle Informationen zur Position des Nutzers
                earth.cameraGeospatialPose
            /**Aktualisiert die MapView die unter der Kamera angezeigt wird
            * mit den neuen Informationen
             */
            activity.view.mapView?.updateMapPosition(
                latitude = cameraGeospatialPose.latitude,
                longitude = cameraGeospatialPose.longitude,
                heading = cameraGeospatialPose.heading
            )
        }
        if (earth != null) {
            activity.view.updateStatusText(earth, earth.cameraGeospatialPose)
        }


                var currentAnchor: Anchor? = null
                /**Der Code im try Block stellt die Modelle an der Position der Anker dar.
                 * Bei einem Fehler wird dieser ausgegeben.*/
                try {
                        if (earth?.trackingState == TrackingState.TRACKING) {

                                onlyanchor?.forEach { anchor ->
                                    currentAnchor = anchor
                                    //Stellt den ein 3D-Modell an der Position des Ankers dar.
                                    render.renderCompassAtTerrainAnchor(anchor)

                            }
                        }
                        /**Der folgende Code wird nur einmal ausgegeben, um die Performance der App
                         * zu messen. Es gibt an, wie lange es dauerte, bis die Anker aus den Daten
                         * der Datenbank dargestellt wurden */

                        if (wartendatabase) {
                            Log.i(
                                "performancetest",
                                "Es dauerte ungefähr: " + (Screentimer!!.minus(System.currentTimeMillis())) + " Milli für die Darstellung in onResume"
                            )
                            Log.i(
                                "performancetest",
                                "Es dauerte ungefähr: " + (ondrawtimer.minus(System.currentTimeMillis())) + " Milli für die Darstellung in onDrawframe"
                            )
                            wartendatabase = false
                        }
                } catch (e: NullPointerException) {
                    Log.e(
                        "Fehler",
                        "Der Fehler ist: $e"
                    )
                } catch (e: IndexOutOfBoundsException) {
                    Log.e("Fehler",
                        "IndexOutOfBoundsException: $e"
                    )
                }catch (e: ConcurrentModificationException){
                    Log.e(
                        "Fehler",
                        "Es ist ein ConcurrentModificationException: $e und der Fehler ist bei $currentAnchor"
                    )
                }


        //Darstellung der qTestAnker
        try {
            Log.i("QTest", "Sind jetzt vor 1")
            if (qTest?.state == FutureState.DONE) {
                if (earth?.trackingState == TrackingState.TRACKING) {
                    qTest?.resultAnchor.let { render.renderCompassAtTerrainAnchor(it) }
                }
            }

            Log.i("QTest", "Sind jetzt vor 2")
            if (qTest2?.state == FutureState.DONE) {
                if (earth?.trackingState == TrackingState.TRACKING) {
                    qTest2?.resultAnchor.let { render.renderCompassAtTerrainAnchor(it) }
                }
            }
            Log.i("QTest", "Sind jetzt vor 3")
            if (qTest3?.state == FutureState.DONE) {

                if (earth?.trackingState == TrackingState.TRACKING) {


                    qTest3?.resultAnchor.let { render.renderCompassAtTerrainAnchor(it) }
                }
            }

            Log.i("QTest", "Sind jetzt vor 4")
            if (qTest4?.state == FutureState.DONE) {
                if (earth?.trackingState == TrackingState.TRACKING) {
                    qTest4?.resultAnchor.let { render.renderCompassAtTerrainAnchor(it) }
                }
            }
        }catch (e: ConcurrentModificationException){
            Log.e("Fehler", "Es ist ein ConcurrentModificationException: $e")
            Log.i("Fehler","Am besten der Code wird später wieder gestartet")
        }
        //Darstellung eines 3D-Modells dank dem Dchanker
        //TODO Bestimme ob du den Code löschen möchtest
        if (dachAnchor?.state == FutureState.DONE) {
            if (earth?.trackingState == TrackingState.TRACKING) {
                    //Zur Makierung eines Dachankers, nutzt die folgende Funktion ein anderes 3D-Modell
                    dachAnchor?.resultAnchor.let { render.renderCompassAtRoofAnchor(it) }
            }
        }
        // Compose the virtual scene with the background.
        backgroundRenderer.drawVirtualScene(render, virtualSceneFramebuffer, Z_NEAR, Z_FAR)

    }

    fun onMapClick(latLng: LatLng) {
        // TODO: place an anchor at the given position.
        val earth = session?.earth ?: return
        if (earth.trackingState != TrackingState.TRACKING) {
            return
        }


    }



    /**Funktion aus dem Code des Projektes codelab_geospatial unter renderCompassAtAnchor()
    * Stellt ein 3D-Modell an der Position eines Geländeankers dar
     */
    private fun SampleRender.renderCompassAtTerrainAnchor(anchor: Anchor?) {
        // Get the current pose of the Anchor in world space. The Anchor pose is updated
        // during calls to session.update() as ARCore refines its estimate of the world.
        anchor?.pose?.toMatrix(modelMatrix, 0)

        // Calculate model/view/projection matrices
        Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0)

        // Update shader properties and draw
        virtualObjectShader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix)

        draw(virtualObjectMesh, virtualObjectShader, virtualSceneFramebuffer)

    }
    /**Funktion aus dem Code des Projektes codelab_geospatial unter renderCompassAtAnchor()
     * Stellt ein 3D-Modell an der Position eines Dachankers dar */
    private fun SampleRender.renderCompassAtRoofAnchor(anchor: Anchor?) {
        // Get the current pose of the Anchor in world space. The Anchor pose is updated
        // during calls to session.update() as ARCore refines its estimate of the world.
        anchor?.pose?.toMatrix(modelMatrix, 0)

        // Calculate model/view/projection matrices
        Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0)

        // Update shader properties and draw
        virtualObjectShaderDach.setMat4("u_ModelViewProjection", modelViewProjectionMatrix)
        draw(virtualObjectMesh, virtualObjectShaderDach, virtualSceneFramebuffer)
    }

    private fun showError(errorMessage: String) =
        activity.view.snackbarHelper.showError(activity, errorMessage)

    /** Funktion um die Sicht eines Ankers zu testen über einen Hit-Test.
     *  Diese Funktion ist noch nicht beendet und wird nicht implementiert */
    fun getLatLngFromAnchor(anchor: ResolveAnchorOnRooftopFuture?): LatLng? {
        // Überprüfen Sie, ob Earth im Tracking-Zustand ist
        val earth = session?.earth
        if (earth!!.trackingState != TrackingState.TRACKING) {
            return null
        }
        // Holen Sie sich die Pose des Ankers
        val pose: Pose = anchor?.resultAnchor!!.pose
        // Holen Sie sich die GeospatialPose von der Erdinstanz
        val geospatialPose = earth.getGeospatialPose(pose.extractTranslation())
        // Erstellen Sie ein LatLng-Objekt mit den Breiten- und Längengraden aus der GeospatialPose
        return LatLng(geospatialPose.latitude, geospatialPose.longitude)
    }

    /** Funktion um die Sicht eines Ankers zu testen über einen Hit-Test.
     *  Diese Funktion ist noch nicht beendet und wird nicht implementiert */
    private fun checkVisible(anchor: ResolveAnchorOnRooftopFuture?): Boolean? {
        val earth = session?.earth
        if (earth!!.trackingState != TrackingState.TRACKING) {
            return null
        }
        val frame = session?.update()
        var results = frame?.hitTest(
            getLatLngFromAnchor(anchor)?.latitude!!.toFloat(),
            getLatLngFromAnchor(anchor)?.longitude!!.toFloat()
        );

        if (results!!.isEmpty()) {
            return true
        }
        return false


    }


    /** Funktionen um die Entfernung eines Ankers mit dem Nutzer zu testen.
     *  Funktion noch nicht beendet und wird nicht implementiert.
     *  Abstand von LNG und LTD sind manchmal zu klein und zu ungenau */
    @SuppressLint("SuspiciousIndentation")
    private fun checkRange(anchor: Anchor?, boolean: Boolean): Boolean {

        val earth = session?.earth
        if (earth!!.trackingState != TrackingState.TRACKING) {
            Log.i("Anchorstate", "Sind wir ausversehen nicht in Tracking?")
            return false
        } else
            Log.i("Anchorstate", "Rechnung beginnen")
        timer.schedule(5000) {
            Log.i("Anchorstate", "Timer abgewartet")
        }
        if (session?.earth != null) {
            Log.i("Anchorstate", "Abstand wird berechnet")
            val direction = earth.getGeospatialPose(anchor?.pose)
            Log.i("anchorstate", "latitude " + direction.latitude.toString())
            Log.i("anchorstate", "longtitude " + direction.longitude.toString())

            var abstandLtd = direction!!.latitude - earth.cameraGeospatialPose.latitude
            if (abstandLtd < 0.0) {
                abstandLtd *= -1
            }
            Log.i(
                "Anchorstate",
                "Terrain ist: LATITUDE $abstandLtd indem wir:  " + earth.cameraGeospatialPose.latitude + "  -  " + direction.latitude
            )
            var abstandLng = direction!!.longitude - earth.cameraGeospatialPose.longitude
            if (abstandLng < 0.0) {
                abstandLng *= -1
            }
            Log.i(
                "Anchorstate",
                "Terrain ist: LONGTITUDE $abstandLng indem wir:  " + earth.cameraGeospatialPose.longitude + "   -  " + direction.longitude
            )
            Log.i("Anchorstate", "Abstand berechnet")
            if (abstandLtd < 0.00015) {
                Log.i("Anchorstate", "LTD passt")
                !boolean
                return true
            } else if (abstandLng < 0.00015) {
                Log.i("Anchorstate", "LNG passt")
                !boolean
                return true
            }
            Log.i(
                "Anchorstate",
                "Abstand zu groß für LTD " + (abstandLtd - 0.00002) + "  und LNG: " + (abstandLng - 0.00002)
            )
            !boolean
            return false
        }
        !boolean
        return false
    }
}
