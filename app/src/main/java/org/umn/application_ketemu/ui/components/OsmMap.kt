package org.umn.application_ketemu.ui.components

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint as OsmGeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.umn.application_ketemu.R
import org.umn.application_ketemu.domain.model.GeoPoint
import java.io.File

/**
 * Peta OpenStreetMap (osmdroid) untuk memilih / menampilkan titik barang.
 *
 * Alasan memakai osmdroid, bukan Google Maps SDK:
 *  - tidak memerlukan API key berbayar sehingga proyek tetap bisa dibangun dan
 *    didemokan tanpa akun Google Cloud;
 *  - tidak bergantung pada Google Play Services (emulator tanpa Play Store tetap
 *    bisa menampilkan peta).
 *
 * Peta tetap berfungsi saat offline bila ubin (tile) sudah pernah di-cache;
 * laporan sendiri tidak pernah gagal disimpan hanya karena peta tidak termuat.
 *
 * @param marker titik yang ditampilkan (null = belum ada titik).
 * @param interactive `true` untuk form laporan (ketuk peta memindahkan marker),
 *        `false` untuk pratinjau di halaman detail (peta statis, halaman tetap
 *        bisa digulir).
 * @param onMarkerChange dipanggil saat pengguna mengetuk peta (hanya interaktif).
 */
@Composable
fun KetemuMap(
    marker: GeoPoint?,
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
    onMarkerChange: (GeoPoint) -> Unit = {}
) {
    val context = LocalContext.current
    val currentOnMarkerChange by rememberUpdatedState(onMarkerChange)

    val mapView = remember(context, interactive) {
        createMapView(context = context, marker = marker, interactive = interactive)
    }
    val markerOverlay = remember(mapView) {
        Marker(mapView).apply {
            setIcon(ContextCompat.getDrawable(mapView.context, R.drawable.ic_map_pin))
            // Ujung bawah pin menempel tepat di koordinat yang dipilih.
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            // Ketukan pada pin tidak membuka bubble bawaan osmdroid.
            setOnMarkerClickListener { _, _ -> true }
        }
    }
    val eventsOverlay = remember(mapView) {
        MapEventsOverlay(
            object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: OsmGeoPoint?): Boolean {
                    if (p != null) {
                        currentOnMarkerChange(GeoPoint(p.latitude, p.longitude))
                    }
                    return true
                }

                override fun longPressHelper(p: OsmGeoPoint?): Boolean = false
            }
        )
    }

    // Holder biasa (bukan state Compose) agar penulisan di dalam update() tidak
    // memicu recomposition berulang.
    val lastApplied = remember { arrayOfNulls<GeoPoint>(1) }

    DisposableEffect(mapView, interactive) {
        if (interactive) {
            if (!mapView.overlays.contains(eventsOverlay)) {
                // Indeks 0: event tap diproses sebelum overlay lain.
                mapView.overlays.add(0, eventsOverlay)
            }
        } else {
            mapView.overlays.remove(eventsOverlay)
        }
        mapView.setMultiTouchControls(interactive)
        onDispose {
            mapView.overlays.remove(eventsOverlay)
        }
    }

    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { view ->
            val point = marker?.takeIf { it.isValid }
            if (point == null) {
                view.overlays.remove(markerOverlay)
                lastApplied[0] = null
                view.invalidate()
                return@AndroidView
            }

            val osmPoint = OsmGeoPoint(point.latitude, point.longitude)
            markerOverlay.setPosition(osmPoint)
            if (!view.overlays.contains(markerOverlay)) {
                view.overlays.add(markerOverlay)
            }

            val previous = lastApplied[0]
            val outsideVisibleArea = runCatching {
                !view.boundingBox.contains(osmPoint)
            }.getOrDefault(false)

            // GPS bisa melompat jauh: geser peta hanya bila titik berada di luar
            // area yang terlihat. Ketukan pengguna di dalam peta tidak memicu
            // pergeseran yang mengagetkan.
            if (previous == null || outsideVisibleArea) {
                view.controller.animateTo(osmPoint)
            }
            lastApplied[0] = point
            view.invalidate()
        }
    )
}

/**
 * Membuka titik di aplikasi peta perangkat. Bila tidak ada aplikasi yang
 * menangani URI `geo:`, pengguna diarahkan ke OpenStreetMap lewat browser.
 */
fun openInMaps(context: Context, point: GeoPoint, label: String) {
    val query = "${point.latitude},${point.longitude}"
    val geoUri = Uri.parse("geo:$query?q=$query(${Uri.encode(label)})")
    val intent = Intent(Intent.ACTION_VIEW, geoUri)
    if (context !is Activity) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        val webUri = Uri.parse(
            "https://www.openstreetmap.org/?mlat=${point.latitude}" +
                "&mlon=${point.longitude}#map=18/${point.latitude}/${point.longitude}"
        )
        val webIntent = Intent(Intent.ACTION_VIEW, webUri)
        if (context !is Activity) {
            webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(webIntent) }
    }
}

/**
 * Konfigurasi global osmdroid: User-Agent wajib diisi sesuai kebijakan ubin
 * OpenStreetMap, dan cache diarahkan ke cache internal agar tidak meminta izin
 * penyimpanan pada Android modern.
 */
private fun configureOsmdroid(context: Context) {
    val configuration = Configuration.getInstance()
    configuration.load(
        context,
        context.getSharedPreferences(OSM_PREFS_NAME, Context.MODE_PRIVATE)
    )
    configuration.setUserAgentValue(context.packageName)
    val basePath = File(context.cacheDir, "osmdroid")
    configuration.setOsmdroidBasePath(basePath)
    configuration.setOsmdroidTileCache(File(basePath, "tiles"))
}

private fun createMapView(
    context: Context,
    marker: GeoPoint?,
    interactive: Boolean
): MapView {
    configureOsmdroid(context)
    val center = marker?.takeIf { it.isValid } ?: GeoPoint.CAMPUS_CENTER

    // Peta pratinjau memakai subclass yang tidak menangani sentuhan sama sekali.
    // Dengan begitu gestur gulir diteruskan ke halaman induk, bukan "tertelan"
    // oleh MapView (perilaku ini tidak bisa dicapai lewat OnTouchListener karena
    // listener yang mengembalikan true justru mengonsumsi seluruh gestur).
    val view = if (interactive) MapView(context) else PreviewMapView(context)

    return view.apply {
        setTileSource(TileSourceFactory.MAPNIK)
        setMultiTouchControls(interactive)
        zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        controller.setZoom(if (marker != null) MARKER_ZOOM else CAMPUS_ZOOM)
        controller.setCenter(OsmGeoPoint(center.latitude, center.longitude))
    }
}

/** MapView baca-saja: sentuhan diabaikan agar scroll halaman detail tetap mulus. */
private class PreviewMapView(context: Context) : MapView(context) {
    override fun onTouchEvent(event: MotionEvent?): Boolean = false
}

private const val OSM_PREFS_NAME = "osmdroid"
private const val CAMPUS_ZOOM = 15.0
private const val MARKER_ZOOM = 17.0
