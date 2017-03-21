package com.example.ankur.forecastie;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static android.R.attr.data;
import static android.R.attr.viewportHeight;

/**
 * A placeholder fragment containing a simple view.
 */
public class ForecastFragment extends Fragment {

    private ArrayAdapter<String> mForecastAdapter;
    public ForecastFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment,menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id=item.getItemId();
        if (id ==R.id.action_refresh){
            updateWeather();

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateWeather() {
        FetchWeather fetchWeather =new FetchWeather();

        String location =PreferenceManager.getDefaultSharedPreferences(getActivity())
                .getString(getString(R.string.pref_general_location_key),
                getString(R.string.pref_default_edit_location));
        fetchWeather.execute(location);
    }

    @Override
    public void onStart() {
        super.onStart();
        updateWeather();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View rootView=inflater.inflate(R.layout.fragment_main,container,false);



        mForecastAdapter = new ArrayAdapter<String>(
                getActivity(),R.layout.list_item,R.id.list_item_forecast,new ArrayList<String>()
        );

        ListView listView= (ListView) rootView.findViewById(R.id.listview);
        listView.setAdapter(mForecastAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                String forecast =mForecastAdapter.getItem(position);
                Toast.makeText(getActivity(),forecast,Toast.LENGTH_LONG).show();
                Intent intent=new Intent(getActivity() ,Detail.class)
                        .putExtra(Intent.EXTRA_TEXT,forecast);
                startActivity(intent);
            }
        });
        return rootView;
    }
    public class FetchWeather extends AsyncTask<String,Void,String[]>{

        private final String LOG_TAG= FetchWeather.class.getSimpleName();

        private String dateString(long time){
            //unix time to date

            Date date =new Date(time * 1000);
            SimpleDateFormat format= new SimpleDateFormat("E, MMM d");
            return format.format(date);
        }

        private String formatHighLows(double high,double low) {

            SharedPreferences sharedPreferences=PreferenceManager.getDefaultSharedPreferences(getActivity());
            String unitType= sharedPreferences.getString(getString(R.string.pref_units_key),
                    getString(R.string.pref_units_metric));

            if (unitType.equals(getString(R.string.pref_units_imperial)))
            {high =(high*1.8) +32;
                low =(low * 1.8) +32;}
            else if (!unitType.equals(getString(R.string.pref_units_metric))){
                Log.d(LOG_TAG,"Unit Type Not Found: "+ unitType);
            }
            long roundedHigh = Math.round(high);
            long roundedLow = Math.round(low);

            String highLowStr= roundedHigh +"/" + roundedLow;
            return highLowStr;
        }


        private String[] dataFromJson(String jsonStr,int numDays)throws JSONException{

            final String LIST = "list";
            final String WEATHER = "weather";
            final String TEMPERATURE = "temp";
            final String MAX = "max";
            final String MIN = "min";
            final String DATE = "dt";
            final String DESCRIPTION = "main";

            JSONObject forecastJson= new JSONObject(jsonStr);
            JSONArray weatherArray=  forecastJson.getJSONArray(LIST);

            String[] strings=new String[numDays];
            for (int i=0; i<weatherArray.length();i++) {
                String day;
                String description;
                String highAndLow;

                JSONObject dayforecast = weatherArray.getJSONObject(i);

                long dateTime = dayforecast.getLong(DATE);
                day = dateString(dateTime);

                JSONObject weatherObject = dayforecast.getJSONArray(WEATHER).getJSONObject(0);
                description = weatherObject.getString(DESCRIPTION);

                JSONObject tempObject = dayforecast.getJSONObject(TEMPERATURE);
                double high = tempObject.getDouble(MAX);
                double low = tempObject.getDouble(MIN);

                highAndLow = formatHighLows(high, low);
                strings[i] = day + " - " + description + "-" + highAndLow;
            }

            for (String s : strings) {
                Log.v(LOG_TAG, "forecast entry:" + s);
            }
            return strings;

            }


        @Override
        protected String[] doInBackground(String... params) {


            if (params.length==0){

                return null;
            }


            HttpURLConnection urlConnection = null;
            BufferedReader reader=null;

            String JsonStr=null;

            String format="json";
            String units ="metric";
            int numDays=7;


            try {

                final String FORECAST_URL = "http://api.openweathermap.org/data/2.5/forecast/daily?";

                final String QUERY_PARAM = "q";
                final String FORMAT_PARAM = "mode";
                final String UNIT_PARAM = "units";
                final String DAYS_PARAM = "cnt";
                final String apiKey="&APPID=" +BuildConfig.OPEN_WEATHER_MAP_API_KEY;

                Uri uri = Uri.parse(FORECAST_URL).buildUpon()
                        .appendQueryParameter(QUERY_PARAM, params[0])
                        .appendQueryParameter(FORMAT_PARAM, format)
                        .appendQueryParameter(UNIT_PARAM, units)
                        .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays))
                        .build();

                URL url = new URL(uri.toString().concat(apiKey));

                Log.v(LOG_TAG, "Built URI" + uri.toString());
                //request for open weatger

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                //read input stream into string
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();

                if (inputStream == null) {

                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {

                    return null;
                    //stream is empty
                }
                JsonStr = buffer.toString();

                Log.v(LOG_TAG, "Forcast String: " + JsonStr);
            }catch (IOException e) {

                Log.e(LOG_TAG, "Error", e);

                return null;
            }finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }

            }
            try {
                return dataFromJson(JsonStr,numDays);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String[] result) {
            if (result!=null){
                mForecastAdapter.clear();
                for (String dayForecastStr : result){
                    mForecastAdapter.add(dayForecastStr);
                }
            }
        }
    }
}

