package com.example.tampereenbussit;


import android.app.ProgressDialog;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import com.google.maps.android.ui.IconGenerator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


public class DisplayOnMap  extends FragmentActivity implements GoogleMap.OnMarkerClickListener {
    private ProgressDialog pDialog;

    // URL to get contacts JSON
    private static String url = "http://data.itsfactory.fi/siriaccess/vm/json";

    // JSON Node names
    private static final String TAG_BEARING = "Bearing";
    private static final String TAG_LINEREF = "LineRef";
    private static final String TAG_VEHICLEREF = "VehicleRef";
    private static final String TAG_LATITUDE = "Latitude";
    private static final String TAG_LONGITUDE = "Longitude";
    private static final String TAG_DESTINATION = "DestinationName";
    private static final String TAG_ORIGIN = "OriginName";
    private static final String TAG_DEPARTURETIME = "FramedVehicleJourneyRef";

    private boolean onStop = false;

    String[] accessibleBuses;
    List<String> accessibleBusArray;
    int timerDelay;
    boolean allLines = true;

    String[] hervantaArray;
    List<String> hervannanLinjat;

    private ArrayList <Marker> markerList = new ArrayList<Marker>();
    private final LatLng TAMPERE = new LatLng( 61.498056,  23.760833);
    private Handler m_handler;
    private ArrayList<HashMap<String, String>> busArrayHashMap;
    private GoogleMap googleMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);

        accessibleBuses = getResources().getStringArray(R.array.accessible_buses);
        accessibleBusArray = Arrays.asList(accessibleBuses);

        hervantaArray = getResources().getStringArray(R.array.hervanta);
        hervannanLinjat = Arrays.asList(hervantaArray);
        try {
            MapsInitializer.initialize(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        setContentView(R.layout.map_view);
        busArrayHashMap = new ArrayList<HashMap<String, String>>();
        if (googleMap == null){
            googleMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.mapView))
                    .getMap();
        }

        m_handler = new Handler();

        startHandler();


        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(TAMPERE, 13));
        //googleMap.setBuildingsEnabled(true);
        googleMap.setMyLocationEnabled(true);


        googleMap.setOnMarkerClickListener(this);

    }

        /*
        // Setting a custom info window adapter for the google map
        googleMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

            // Use default InfoWindow frame
            @Override
            public View getInfoWindow(Marker arg0) {
                return null;
            }

            // Defines the contents of the InfoWindow
            @Override
            public View getInfoContents(Marker arg0) {

                // Getting view from the layout file info_window_layout
                View v = getLayoutInflater().inflate(R.layout.info_window_layout, null);


                // Getting reference to the TextView to set latitude
                TextView tvLat = (TextView) v.findViewById(R.id.info_text);


                // Setting the latitude
                tvLat.setText("Latitude:" + arg0.getTitle());


                // Returning the view containing InfoWindow contents
                return v;

            }
        });


    */

    protected void onPause (){
        super.onPause();
        onStop = true;
        googleMap.clear();
        busArrayHashMap.clear();
        markerList.clear();

    }

    @Override
    protected void onStop () {
        super.onStop();
        googleMap.clear();
        busArrayHashMap.clear();
        markerList.clear();
        onStop = true;

    }

    @Override
    protected void onResume (){
        super.onResume();
        onStop = false;

    }

    public  boolean onMarkerClick(Marker marker){
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), googleMap.getCameraPosition().zoom), 200, null);

        if(!marker.isInfoWindowShown())
            marker.showInfoWindow();
        else
            marker.hideInfoWindow();
        //Toast.makeText(this, marker.getSnippet() + " " + marker.getTitle(), Toast.LENGTH_LONG).show();

        return true;
    }


    private void initializeMap() {
        if (googleMap == null){


            if (((MapFragment) getFragmentManager().findFragmentById(R.id.mapView)) != null) {
                googleMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.mapView)).getMap();
            }
            if (googleMap == null) {
                Toast.makeText(getApplicationContext(),
                        "Sorry! unable to create maps", Toast.LENGTH_SHORT)
                        .show();
            }

        }
    }

    public void startHandler()
    {
        m_handler.postDelayed(new Runnable() {
            @Override
            public void run()  {
               if (!onStop){
                   if (markerList.size() == 0){
                       timerDelay = 500;
                   }
                   else if (!allLines){
                       timerDelay = 2500;
                   }
                   else
                      timerDelay = 4000;

                   busArrayHashMap.clear();
                   new GetBuses().execute();
               }
               startHandler();
            }
        }, timerDelay);
    }



    public  class  GetBuses extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... arg0) {
            // Creating service handler class instance
            ServiceHandler sh = new ServiceHandler();

            // Making a request to url and getting response
            String busJSONString;
            busJSONString = sh.makeServiceCall(url, ServiceHandler.GET);


            Log.i("JSON", "> " + busJSONString);
            parseJSON(busJSONString);

            return null;
        }
        private void parseJSON(String busJSONString){

            if (busJSONString != null) {
                try {
                    Log.i("JSON", "Testi");

                    JSONObject jsonObj = new JSONObject(busJSONString);
                    // Getting JSON Array node
                    JSONObject siri = jsonObj.getJSONObject("Siri");
                    JSONObject serviceDelivery = siri.getJSONObject("ServiceDelivery");
                    JSONArray vehicleMonitor = serviceDelivery.getJSONArray("VehicleMonitoringDelivery");
                    JSONObject vehicleObject = vehicleMonitor.getJSONObject(0);
                    JSONArray vehicles = vehicleObject.getJSONArray("VehicleActivity");

                    Log.i("JSON", "Testi " + vehicles.length());



                    // looping through All Vehicles
                    for (int i = 0; i < vehicles.length(); i++) {
                        // Parse to
                        HashMap<String, String> singleBus = new HashMap<String, String>();

                        JSONObject vehicleActivity = vehicles.getJSONObject(i);
                        JSONObject monitoredVehicle = vehicleActivity.getJSONObject("MonitoredVehicleJourney");

                        //Get line
                        JSONObject lineRef = monitoredVehicle.getJSONObject(TAG_LINEREF);
                        String line = lineRef.getString("value");

                        //Get vehicle reference
                        JSONObject vehicleRef = monitoredVehicle.getJSONObject(TAG_VEHICLEREF);
                        String vehicle = vehicleRef.getString("value");

                        // Get vehicle location
                        JSONObject vehicleLocation = monitoredVehicle.getJSONObject("VehicleLocation");
                        String vehicleLocationLatitude = vehicleLocation.getString(TAG_LATITUDE);
                        String vehicleLocationLongitude = vehicleLocation.getString(TAG_LONGITUDE);
                        //Log.i("JSON", "Testi " + vehicleLocationLongitude);
                        //Log.i("JSON", "Testi " + vehicleLocationLatitude);

                        // Get origin
                        JSONObject vehicleOrigin = monitoredVehicle.getJSONObject(TAG_ORIGIN);
                        String vehicleOriginString = vehicleOrigin.getString("value");

                        //Get destination
                        JSONObject vehicleDestination = monitoredVehicle.getJSONObject(TAG_DESTINATION);
                        String vehicleDestinationString = vehicleDestination.getString("value");

                        //Get bearing
                        String bearingValue = monitoredVehicle.getString(TAG_BEARING);

                        //Get departure time
                        JSONObject vehicleTimeOfDeparture = monitoredVehicle.getJSONObject(TAG_DEPARTURETIME);
                        String vehicleDepartureTime = vehicleTimeOfDeparture.getString("DatedVehicleJourneyRef");

                        singleBus.put(TAG_VEHICLEREF, vehicle);
                        singleBus.put(TAG_LINEREF, line);
                        singleBus.put(TAG_LATITUDE, vehicleLocationLatitude);
                        singleBus.put(TAG_LONGITUDE, vehicleLocationLongitude);
                        singleBus.put(TAG_DESTINATION, vehicleDestinationString);
                        singleBus.put(TAG_ORIGIN, vehicleOriginString);
                        singleBus.put(TAG_BEARING, bearingValue);
                        singleBus.put(TAG_DEPARTURETIME, vehicleDepartureTime);

                        if (allLines == true)
                            busArrayHashMap.add(singleBus);
                        else if (allLines == false && hervannanLinjat.contains(line))
                             busArrayHashMap.add(singleBus);


                        //Log.i("JSON", "Testi " + busArrayHashMap.get(0).get(TAG_VEHICLEREF));
                        //Log.i("Map: ", "Size Json " + busArrayHashMap.size());

                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            } else {
                Log.e("ServiceHandler", "Couldn't get any data from the url");
            }
        }
        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);

            // Clear the map

            //googleMap.clear();

            if (busArrayHashMap != null){


                for(int i = 0; i < busArrayHashMap.size(); i++){



                    double lattDouble = Double.parseDouble(busArrayHashMap.get(i).get(TAG_LATITUDE));
                    double longDouble = Double.parseDouble(busArrayHashMap.get(i).get(TAG_LONGITUDE));

                    LatLng positionLL = new LatLng(lattDouble, longDouble);
                    String title = busArrayHashMap.get(i).get(TAG_VEHICLEREF);
                    String lineNumber = busArrayHashMap.get(i).get(TAG_LINEREF);
                    String departureTime = busArrayHashMap.get(i).get(TAG_DEPARTURETIME);
                    String markerInfo = busArrayHashMap.get(i).get(TAG_LINEREF);
                    float bearingF = Float.parseFloat(busArrayHashMap.get(i).get(TAG_BEARING));
                    boolean found = false;

                    // päivitetään markkeri
                    for(int j = 0; j< markerList.size();j++){
                        String[] snip = markerList.get(j).getSnippet().split("[ ]+");
                        //Log.i("Map: ", "markerlist " + markerList.get(j).getSnippet());

                        if (markerList.get(j).getTitle().equals(title) && snip[0].equals(lineNumber)){
                            //Log.i("Map: ", "markerlist " + markerList.get(j).getSnippet());

                            markerList.get(j).setPosition(positionLL);
                            markerList.get(j).setRotation(bearingF);
                            markerList.get(j).setSnippet(lineNumber + "     " + departureTime);
                            found = true;
                            break;
                        }

                    }

                    // uusi markkeri
                    if (!found){
                        IconGenerator iG = new IconGenerator(DisplayOnMap.this);

                        iG.setTextAppearance(R.style.iconGenText);
                        Resources res = getResources();

                        if  (accessibleBusArray.contains(title)
                                && hervannanLinjat.contains(lineNumber)){

                            Drawable drawable = res.getDrawable(R.drawable.arrow_herv);
                            iG.setBackground(drawable);

                        }

                        else if (accessibleBusArray.contains(title)){
                            Drawable drawable = res.getDrawable(R.drawable.arrow_handi);
                            iG.setBackground(drawable);
                        }
                        else {
                            Drawable drawable = res.getDrawable(R.drawable.arrow_small);
                            iG.setBackground(drawable);
                        }

                        iG.setTextAppearance(R.style.iconGenText);

                        MarkerOptions markerOptions = new MarkerOptions()
                                .position(positionLL)
                                .rotation(bearingF)
                                .flat(true)
                                .title(title)
                                .snippet(lineNumber + "     " + departureTime)
                                .icon(BitmapDescriptorFactory.fromBitmap(iG.makeIcon(busArrayHashMap.get(i).get(TAG_LINEREF))));
                        Marker marker = googleMap.addMarker(markerOptions);



                        //marker.showInfoWindow(); fromResource(R.drawable.arrow_small)

                        markerList.add(marker);

                    }



                }
            }


        }

    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_search:
                openSearch();
                return true;
            case R.id.action_settings:
                openSettings();
                return true;
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void openSettings() {
        Toast.makeText(this, "Search button pressed", Toast.LENGTH_SHORT).show();
    }

    private void openSearch() {
        if (allLines) {
            allLines = false;
            markerList.clear();
            googleMap.clear();
            Toast.makeText(this, "Hervannan linjat valittu", Toast.LENGTH_SHORT).show();
        }else{
            allLines = true;
            markerList.clear();
            googleMap.clear();
            Toast.makeText(this, "Kaikki linjat valittu", Toast.LENGTH_SHORT).show();
        }

    }
}
