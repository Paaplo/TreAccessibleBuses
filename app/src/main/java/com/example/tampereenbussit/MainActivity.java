package com.example.tampereenbussit;

import android.app.Activity;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends Activity {
    private ProgressDialog pDialog;

    // URL to get contacts JSON
    private static String url = "http://data.itsfactory.fi/siriaccess/vm/json";

    // JSON Node names
    private static final String TAG_SIRI = "Siri";
    private static final String TAG_VEHICLES = "VehicleActivity";
    private static final String TAG_LINEREF = "LineRef";
    private static final String TAG_VEHICLEREF = "VehicleRef";
    private static final String TAG_LATITUDE = "Longitude";
    private static final String TAG_LONGITUDE = "Latitude";
    private static final String TAG_DESTINATION = "DestinationName";
    private static final String TAG_ORIGIN = "OriginName";




    // H
    String busJSONString;


    private ArrayList<HashMap<String, String>> busHashMap;

    ListView listView;
    ArrayAdapter<HashMap<String, String>> arrayAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        listView = (ListView) findViewById(R.id.busList);

        busHashMap = new ArrayList<HashMap<String, String>>();

        arrayAdapter = new ArrayAdapter<HashMap<String, String>>(this, R.layout.list_item, R.id.listItem, busHashMap);
        listView.setAdapter(arrayAdapter);

        Button refresh = (Button) findViewById(R.id.refresh);
        Button viewOnMap = (Button) findViewById(R.id.mapButton);

        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
              new GetBuses().execute();
            }
        });


        viewOnMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(MainActivity.this, DisplayOnMap.class);
                startActivity(i);
            }
        });


        new GetBuses().execute();

    }

    public class GetBuses extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Showing progress dialog
            pDialog = new ProgressDialog(MainActivity.this);
            pDialog.setMessage("Please wait...");
            pDialog.setCancelable(false);
            pDialog.show();

        }

        @Override
        protected Void doInBackground(Void... arg0) {
            // Creating service handler class instance
            ServiceHandler sh = new ServiceHandler();

            // Making a request to url and getting response
            busJSONString = sh.makeServiceCall(url, ServiceHandler.GET);


            Log.i("JSON", "> " + busJSONString);
            parseJSON(busJSONString);

            return null;
        }
        public void parseJSON(String busJSONString){

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
                        HashMap <String, String> singleBus = new HashMap<String, String>();

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
                        Log.i("JSON", "Testi " + vehicleLocationLongitude);
                        Log.i("JSON", "Testi " + vehicleLocationLatitude);

                        // Get origin
                        JSONObject vehicleOrigin = monitoredVehicle.getJSONObject(TAG_ORIGIN);
                        String vehicleOriginString = vehicleOrigin.getString("value");

                        JSONObject vehicleDestination = monitoredVehicle.getJSONObject(TAG_DESTINATION);
                        String vehicleDestinationString = vehicleDestination.getString("value");

                        singleBus.put(TAG_VEHICLEREF, vehicle);
                        singleBus.put(TAG_LINEREF, line);
                        singleBus.put(TAG_LATITUDE, vehicleLocationLatitude);
                        singleBus.put(TAG_LONGITUDE, vehicleLocationLongitude);
                        singleBus.put(TAG_DESTINATION, vehicleDestinationString);
                        singleBus.put(TAG_ORIGIN, vehicleOriginString);

                        busHashMap.add(singleBus);

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
            // Dismiss the progress dialog
            if (pDialog.isShowing())
                pDialog.dismiss();
            /**
             * Updating parsed JSON data into ListView
             * */

            arrayAdapter.notifyDataSetChanged();


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
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void openSearch() {
        Toast.makeText(this, "Search button pressed", Toast.LENGTH_SHORT).show();
    }

    private void openSettings() {
        Toast.makeText(this, "Search button pressed", Toast.LENGTH_SHORT).show();
    }


}
