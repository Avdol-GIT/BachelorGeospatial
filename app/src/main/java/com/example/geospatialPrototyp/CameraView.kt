package com.example.geospatialPrototyp

import android.app.AlertDialog
import android.opengl.GLSurfaceView
import android.view.View
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.geospatialPrototyp.R
import com.example.geospatialPrototyp.helpers.SnackbarHelper
import com.example.geospatialPrototyp.helpers.TapHelper
import com.google.ar.core.Config
//View-Klasse aller UI Elemente für den Upload-Modus im Prototyp
/**
 * Bestandteile der Klasse wurden aus dem Programm hello-ar-kotlin genommen
 * Weitere Informationen finden Sie hier:
 * https://github.com/google-ar/arcore-android-sdk/tree/master/samples/hello_ar_kotlin
 * Englische Kommentare vor einem Codeblock sind Bestandteil von codelab-geospatial und markieren
 * den Code als diesen
 */
class CameraView(val activity: CameraViewActivity): DefaultLifecycleObserver {
    val root = View.inflate(activity, R.layout.activity_cameraview, null)
    val surfaceView = root.findViewById<GLSurfaceView>(R.id.surfaceview)

    //Kann aufgerufen werden nachdem der SessionHelper in der Activity aktiviert wurde

    val session get() = activity.SessionHelper.session
    val snackbarHelper = SnackbarHelper()
    val tapHelper = TapHelper(activity).also { /*surfaceView.setOnTouchListener(it)*/ }

    override fun onResume(owner: LifecycleOwner) {
        surfaceView.onResume()
    }

    override fun onPause(owner: LifecycleOwner) {
        surfaceView.onPause()
    }
    fun showOcclusionDialogIfNeeded() {
        val session = session ?: return
        val isDepthSupported = session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)
        if (!activity.depthSettings.shouldShowDepthEnableDialog() || !isDepthSupported) {
            return // Don't need to show dialog.
        }

        // Asks the user whether they want to use depth-based occlusion.
        AlertDialog.Builder(activity)
            .setTitle(R.string.options_title_with_depth)
            .setMessage(R.string.depth_use_explanation)
            .setPositiveButton(R.string.button_text_enable_depth) { _, _ ->
                activity.depthSettings.setUseDepthForOcclusion(true)
            }
            .setNegativeButton(R.string.button_text_disable_depth) { _, _ ->
                activity.depthSettings.setUseDepthForOcclusion(false)
            }
            .show()
    }

}