/*
 * Copyright 2022 Google LLC
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

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.geospatialPrototyp.MainActivity
import com.example.geospatialPrototyp.SessionHelper
import com.example.geospatialPrototyp.geospatial.helpers.GeoPermissionsHelper
import com.example.geospatialPrototyp.geospatial.helpers.HelloGeoView
import com.example.geospatialPrototyp.helpers.FullScreenHelper
import com.example.geospatialPrototyp.samplerender.SampleRender
import com.google.ar.core.Config
import com.google.ar.core.Config.DepthMode
import com.google.ar.core.Session
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableApkTooOldException
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import com.google.ar.core.exceptions.UnavailableSdkTooOldException
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
/**
 * Bestandteile der Klasse wurden aus dem Programm codelab-geospatial genommen
 * Weitere Informationen finden Sie hier: https://github.com/google-ar/codelab-geospatial
 * Englische Kommentare vor einem Codeblock sind Bestandteil von codelab-geospatial und markieren
 * den Code als diesen
 * Außerhalb der Konfiguration der Sitzung und der Nutzung der selbst erstellten Klasse SessionHelper,
 * wurde die Klasse nicht angepasst.
 * Die Klasse dient als Aktivität des Ansichtsmodus im Prototyp
 */
class HelloGeoActivity : MainActivity() {
  companion object {
    const val TAG = "HelloGeoActivity"
  }

  //Erstellter Helfer für die zu nutzende Sitzung
  lateinit var SessionHelper: SessionHelper //Änderung von SessionHelper aus SessionHelper
  //Aufrug der View-Objekte
  lateinit var view: HelloGeoView
  //Aufruf des Renderers, welcher die 3D-Objekte rendert
  lateinit var renderer: HelloGeoRenderer

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Aktivierung von ARCore SessionHelper
    SessionHelper = SessionHelper(this)
    // If Session creation or Session.resume() fails, display a message and log detailed
    // information.
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
        Log.e(TAG, "ARCore threw an exception", exception)
        view.snackbarHelper.showError(this, message)
      }

    // Configure session features.
    SessionHelper.beforeSessionResume = ::configureSession
    lifecycle.addObserver(SessionHelper)

    // Set up the Hello AR renderer.
    renderer = HelloGeoRenderer(this)
    lifecycle.addObserver(renderer)

    // Set up Hello AR UI.
    view = HelloGeoView(this)
    lifecycle.addObserver(view)
    setContentView(view.root)

    // Sets up an example renderer using our HelloGeoRenderer.
    SampleRender(view.surfaceView, renderer, assets)
    Log.i("Config","Text der Session Config: "+ SessionHelper.session?.config?.depthMode.toString())
  }


  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<String>,
    results: IntArray
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, results)
    if (!GeoPermissionsHelper.hasGeoPermissions(this)) {
      // Use toast instead of snackbar here since the activity will exit.
      Toast.makeText(this, "Camera and location permissions are needed to run this application", Toast.LENGTH_LONG)
        .show()
      if (!GeoPermissionsHelper.shouldShowRequestPermissionRationale(this)) {
        // Permission denied with checking "Do not ask again".
        GeoPermissionsHelper.launchPermissionSettings(this)
      }
      finish()
    }
  }

// Konfiguration der Sitzung. Es erlaubt die Nutzung der verschiedenen APIs
private fun configureSession(session: Session) {
    session.configure(
      /**session.config ist die Standard Konfiguration der Sitzung. Es werden weitere Konfigurationen
       * implementiert. */
      session.config.apply {
        // Aktivierung des Geospatial Modus für die Nutzung der Geospatial API
        geospatialMode = Config.GeospatialMode.ENABLED
        // Aktivierung des Streetcape Modus für die Nutzung der Streetcape Geometry API
        streetscapeGeometryMode = Config.StreetscapeGeometryMode.ENABLED
        // Abfrage ob Endgerät den Depth Modus unterstützt
        if(session.isDepthModeSupported(DepthMode.AUTOMATIC)) {
          depthMode = DepthMode.AUTOMATIC
        }else{
          Log.i("Config","Depthmode nicht supported: " + session.config.depthMode.toString())
        }
      }
    )
  }

  override fun onWindowFocusChanged(hasFocus: Boolean) {
    super.onWindowFocusChanged(hasFocus)
    FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus)
  }

}
