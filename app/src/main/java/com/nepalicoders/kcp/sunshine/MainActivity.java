package com.nepalicoders.kcp.sunshine;

import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.Intent;
import android.content.Loader;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.nepalicoders.kcp.sunshine.data.SunshinePreferences;
import com.nepalicoders.kcp.sunshine.utilities.NetworkUtils;
import com.nepalicoders.kcp.sunshine.utilities.OpenWeatherJsonUtils;

import java.net.URL;

public class MainActivity extends AppCompatActivity implements ForecastAdapter.ForecastAdapterOnClickHandler, LoaderManager.LoaderCallbacks<String[]> {
    private final static int FORECAST_LOADER_ID = 0;
    private RecyclerView mRecyclerView;
    private ForecastAdapter mForecastAdapter;
    private TextView mErrorMessageTextView;
    private ProgressBar mLoadingProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forecast);
        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerview_forecast);
        mErrorMessageTextView = (TextView) findViewById(R.id.tv_error_message_display);

        //setting layout manager for recycle view
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(layoutManager);
        //Using setHasFixedSize(true) on mRecyclerView to designate that all items in the list will have the same size
        mRecyclerView.setHasFixedSize(true);

        //connecting adapter
        mForecastAdapter = new ForecastAdapter(this);
        mRecyclerView.setAdapter(mForecastAdapter);

        mLoadingProgressBar = (ProgressBar) findViewById(R.id.pb_loading_indicator);

        int loaderId = FORECAST_LOADER_ID;
        Bundle bundleForLoader = null;
        LoaderManager.LoaderCallbacks<String[]> callback = MainActivity.this;
        //start the loader
        getLoaderManager().initLoader(loaderId, bundleForLoader, callback);

    }

//    private void loadWeatherData() {
//        String location = SunshinePreferences.getPreferredWeatherLocation(this);
//
//        //adding location in a bundle to access it onResume
//        Bundle queryBundle = new Bundle();
//        queryBundle.putString(FORECAST_LOCATION_EXTRA, location);
//
//        //loading weather of given location
//        LoaderManager loaderManager = getLoaderManager();
//        Loader<Object> weatherSearchLoader = loaderManager.getLoader(FORECAST_LOADER_ID);
//        if (weatherSearchLoader == null) {
//            loaderManager.initLoader(FORECAST_LOADER_ID, queryBundle, MainActivity.this);
//        } else {
//            loaderManager.restartLoader(FORECAST_LOADER_ID, queryBundle, MainActivity.this);
//        }
//    }

    @Override
    public Loader<String[]> onCreateLoader(int id, final Bundle loaderArgs) {
        return new AsyncTaskLoader<String[]>(this) {
            String[] mWeatherForecast=null;

            @Override
            protected void onStartLoading() {

                if (mWeatherForecast != null) {
                    deliverResult(mWeatherForecast);

                } else {
                    mLoadingProgressBar.setVisibility(View.VISIBLE);
                    forceLoad();
                }
            }

            @Override
            public String[] loadInBackground() {
                String location = SunshinePreferences.getPreferredWeatherLocation(MainActivity.this);
                if (location == null || TextUtils.isEmpty(location)) {
                    return null;
                }
                URL weatherRequestUrl = NetworkUtils.buildUrl(location);

                try {
                    String jsonWeatherResponse = NetworkUtils
                            .getResponseFromHttpUrl(weatherRequestUrl);

                    String[] simpleJsonWeatherData = OpenWeatherJsonUtils
                            .getSimpleWeatherStringsFromJson(MainActivity.this, jsonWeatherResponse);

                    return simpleJsonWeatherData;

                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            public void deliverResult(String[] data) {
                mWeatherForecast = data;
                super.deliverResult(data);
            }
        };
    }

    @Override
    public void onLoadFinished(Loader<String[]> loader, String[] weatherData) {
        mLoadingProgressBar.setVisibility(View.INVISIBLE);
        //passing weather data to the adapter, adapter will handle the data
        mForecastAdapter.setWeatherData(weatherData);
        if (weatherData != null) {
            showWeatherDataView();
        } else {
            showErrorMessage();
        }
    }

    //on click - individual item of the list
    @Override
    public void onClick(String weatherForDay) {
        Intent intentToStartDetailActivity = new Intent(this, DetailActivity.class);
        intentToStartDetailActivity.putExtra(Intent.EXTRA_TEXT, weatherForDay);
        startActivity(intentToStartDetailActivity);
    }

    @Override
    public void onLoaderReset(Loader<String[]> loader) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.forecast, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int selectedMenu = item.getItemId();
        if (selectedMenu == R.id.action_refresh) {
            mForecastAdapter.setWeatherData(null);
            getLoaderManager().restartLoader(FORECAST_LOADER_ID,null,MainActivity.this);
            return true;
        }
        if (selectedMenu == R.id.action_map) {
            openLocationInMap();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void showWeatherDataView() {
        mErrorMessageTextView.setVisibility(View.INVISIBLE);
        mRecyclerView.setVisibility(View.VISIBLE);
    }

    public void showErrorMessage() {
        mErrorMessageTextView.setVisibility(View.VISIBLE);
        mRecyclerView.setVisibility(View.INVISIBLE);
    }

    private void openLocationInMap() {
        String addressString = "1600 Ampitheatre Parkway, CA";
        Uri geoLocation = Uri.parse("geo:0,0?q=" + addressString);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(geoLocation);

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Log.d("MainActivity", "Couldn't call " + geoLocation.toString()
                    + ", no receiving apps installed!");
        }
    }

}
