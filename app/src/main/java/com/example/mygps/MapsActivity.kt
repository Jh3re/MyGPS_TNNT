package com.example.mygps

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.mygps.Constants.INTERVAL_TIME
import com.example.mygps.Coordenates.multicine
import com.example.mygps.Coordenates.parquePuraPura
import com.example.mygps.Coordenates.plazaSanFrancisco
import com.example.mygps.Coordenates.univalle

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.example.mygps.databinding.ActivityMapsBinding
import com.google.android.gms.location.FusedLocationProviderClient


import com.google.android.gms.location.*
import com.google.android.gms.maps.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnInfoWindowClickListener, GoogleMap.OnMarkerClickListener {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding

    private lateinit var fusedLocation: FusedLocationProviderClient
    private val PERMISSION_ID = 42
    private var isGPSEnabled = false
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var c = 0

    companion object{
        val PERMISSION_GRANTED = arrayOf(
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_NETWORK_STATE
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        fusedLocation = LocationServices.getFusedLocationProviderClient(this)
        manageLocation()


    }

    /**
     * Pidiendo permisos
     */


    @SuppressLint("MissingPermission")
    private fun manageLocation(){
        if (hasGPSEnabled()){
            if (allPermissionsGranted()) {
                fusedLocation = LocationServices.getFusedLocationProviderClient(this)
                fusedLocation.lastLocation.addOnSuccessListener {
                        location -> getCoordinates()
                }
            } else{
                requestPermissionUser()
                manageLocation()
            }

        }else
            enableGPSService()


    }

    private fun requestPermissionUser(){
        // Lanzar la ventana al usuario para solcitar permisos o lo deniegue
        ActivityCompat.requestPermissions(this, PERMISSION_GRANTED,PERMISSION_ID)
    }

    @SuppressLint("MissingPermission")
    private fun getCoordinates() {
        // Para la version de Google gms location 21 y superiores
        var locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,INTERVAL_TIME
        ).setMaxUpdates(300).build() // Tarea 200 300

        fusedLocation.requestLocationUpdates(locationRequest,myLocationCallback, Looper.myLooper())
    }

    private val myLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            var myLastLocation: Location? = locationResult.lastLocation
            if (myLastLocation != null){
                var lastLatitude = myLastLocation.latitude
                var lastLongitude = myLastLocation.longitude

                latitude = lastLatitude//myLastLocation.latitude
                longitude = lastLongitude//myLastLocation.longitude

            }
        }
    }

    private fun allPermissionsGranted(): Boolean = PERMISSION_GRANTED.all {
        ActivityCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }


    /**
     * Verificacion si el GPS esta activado
     */

    private fun enableGPSService() {
        if (!hasGPSEnabled()){
            AlertDialog.Builder(this)
                .setTitle(R.string.dialog_text_title)
                .setMessage(R.string.dialog_text_description)
                .setPositiveButton(
                    R.string.dialog_button_accept,
                    DialogInterface.OnClickListener {
                            dialog, wich -> goToEnableGPS()
                    })
                .setNegativeButton(R.string.dialog_button_deny) {
                        dialog, wich -> isGPSEnabled = false
                }
                .setCancelable(true)
                .show()

        }else
            Toast.makeText(this,"GPS activado", Toast.LENGTH_SHORT).show()

    }

    private fun goToEnableGPS() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        startActivity(intent)
    }

    private fun hasGPSEnabled(): Boolean {

        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap


        mMap.setInfoWindowAdapter(CustomInfoWindowForGoogleMap(this))


        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        mMap.isMyLocationEnabled = true

        mMap.uiSettings.apply {
            isMyLocationButtonEnabled = true
            isZoomControlsEnabled=true
            isCompassEnabled=true
            isMapToolbarEnabled=true
            isRotateGesturesEnabled = false
            isTiltGesturesEnabled = false
            isZoomGesturesEnabled = true
        }

        fusedLocation.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val ubication = LatLng(location.latitude,location.longitude)
                val customCamera = CameraPosition.Builder()
                    .target(ubication)
                    .zoom(20f)
                    .tilt(80f)
                    .bearing(195f)
                    .build()
                mMap.moveCamera(CameraUpdateFactory.newCameraPosition(customCamera))
            }
        }

        val parqueMarker = mMap.addMarker(MarkerOptions().position(parquePuraPura).title("PURA PURA").snippet("Lugar tranquilo"))
        val cineMarker = mMap.addMarker(MarkerOptions().position(multicine).title("MULTICINE").snippet("Un lugar chido"))
        val uniMarker = mMap.addMarker(MarkerOptions().position(univalle).title("UNIVALLE").snippet("Aprendiendo más"))
        val plazaMarker = mMap.addMarker(MarkerOptions().position(plazaSanFrancisco).title("SAN FRANCISCO").snippet("Pasando el rato"))

        parqueMarker?.run {
            setIcon(BitmapDescriptorFactory.fromResource(R.drawable.leonardo))
            showInfoWindow()
        }
        cineMarker?.run {
            setIcon(BitmapDescriptorFactory.fromResource(R.drawable.rafael))
            showInfoWindow()
        }
        uniMarker?.run {
            setIcon(BitmapDescriptorFactory.fromResource(R.drawable.donatello))
            showInfoWindow()
        }
        plazaMarker?.run {
            setIcon(BitmapDescriptorFactory.fromResource(R.drawable.michaelangelo))
            showInfoWindow()
        }

        lifecycleScope.launch {
            delay(9000)
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(parquePuraPura,17f))
            parqueMarker?.run {
                setIcon(BitmapDescriptorFactory.fromResource(R.drawable.leonardo))
                showInfoWindow()
            }
            delay(6000)
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(plazaSanFrancisco,17f))
            plazaMarker?.run {
                setIcon(BitmapDescriptorFactory.fromResource(R.drawable.michaelangelo))
                showInfoWindow()
            }
            delay(6000)
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(univalle,17f))
            uniMarker?.run {
                setIcon(BitmapDescriptorFactory.fromResource(R.drawable.donatello))
                showInfoWindow()
            }
            delay(6000)
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(multicine,17f))
            cineMarker?.run {
                setIcon(BitmapDescriptorFactory.fromResource(R.drawable.rafael))
                showInfoWindow()
            }
        }

        mMap.setOnMapClickListener {

            if(c<2){
                var nuevaUbicacion = mMap.addMarker(MarkerOptions().position(it).title("Mi nueva posicion").snippet("${it.latitude}, ${it.longitude}").draggable(true))
                nuevaUbicacion?.run {
                    setIcon(BitmapDescriptorFactory.fromResource(R.drawable.astilla_tnnt))
                    showInfoWindow()
                }
            }else{
                Toast.makeText(this,"Ya completo el numero de marcas",Toast.LENGTH_SHORT).show()
            }
            c++
        }

        mMap.setOnMarkerClickListener(this)

        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this,R.raw.map_style))




    }

    override fun onInfoWindowClick(marker: Marker) {

    }

    override fun onMarkerClick(marker: Marker): Boolean {
        Toast.makeText(this,"${marker.position.latitude}, ${marker.position.longitude}",Toast.LENGTH_SHORT).show()
        return false
    }


}

class CustomInfoWindowForGoogleMap(context: Context) : GoogleMap.InfoWindowAdapter {

    var mContext = context
    var mWindow = (context as Activity).layoutInflater.inflate(R.layout.info_design, null)

    private fun rendowWindowText(marker: Marker, view: View){

        val tvTitle = view.findViewById<TextView>(R.id.title)
        val tvSnippet = view.findViewById<TextView>(R.id.snippet)

        tvTitle.text = marker.title
        tvSnippet.text = marker.snippet

    }

    override fun getInfoContents(marker: Marker): View {
        rendowWindowText(marker, mWindow)
        return mWindow
    }

    override fun getInfoWindow(marker: Marker): View? {
        rendowWindowText(marker, mWindow)
        return mWindow
    }
}