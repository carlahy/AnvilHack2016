package com.anvilhack.anvilhack;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.DataPointInterface;
import com.jjoe64.graphview.series.OnDataPointTapListener;
import com.jjoe64.graphview.series.PointsGraphSeries;
import com.jjoe64.graphview.series.Series;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.Spotify;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;


public class MainActivity extends Activity implements ConnectionStateCallback, AdapterView.OnItemSelectedListener {

    // TODO: Replace with your client ID
    private static final String CLIENT_ID = "92685aa42f484569b44bbf91733a5bbb";
    // TODO: Replace with your redirect URI
    private static final String REDIRECT_URI = "https://google.com";

    private String accessToken = null;
    RequestQueue queue;

    // Request code that will be passed together with authentication result to the onAuthenticationResult callback
    // Can be any integer
    private static final int REQUEST_CODE = 1337;

    private Player mPlayer;
    private GraphView graph;

    private Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button = (Button)findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createPlaylist();
            }
        });

        queue = Volley.newRequestQueue(this);

        AuthenticationRequest.Builder builder =
                new AuthenticationRequest.Builder(CLIENT_ID, AuthenticationResponse.Type.TOKEN, REDIRECT_URI);
        builder.setScopes(new String[]{"user-read-private user-library-read playlist-modify-public playlist-modify-private"});
        AuthenticationRequest request = builder.build();

        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);
        graph = (GraphView) findViewById(R.id.graph);
        set_axis();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
            if (response.getType() == AuthenticationResponse.Type.TOKEN) {
                Log.d("hello", response.getAccessToken());
                accessToken = response.getAccessToken();
                makeRequest();
                Toast.makeText(this,"got token", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void makeRequest() {

        JSONObject info = new JSONObject();

        try {
            info.put("accessToken", accessToken);
        } catch (JSONException e) {
            e.printStackTrace();
        }

            final String URL = "http://10.100.196.75:8888/api/cluster/danceability/energy";
            JsonObjectRequest req = new JsonObjectRequest(Request.Method.POST, URL, info,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            Log.d("hello", response.toString());
                            ArrayList<Point> points = new ArrayList<>();
                            ArrayList<Point> centroids = new ArrayList<>();
                            try {
                                JSONArray arr = response.getJSONArray("tracks");
                                for (int i = 0; i < arr.length(); i++) {
                                    String name = arr.getJSONObject(i).getString("name");
                                    String id = arr.getJSONObject(i).getString("id");
                                    if (arr.getJSONObject(i).isNull("x") || arr.getJSONObject(i).isNull("y")) {
                                        continue;
                                    }
                                    double x = arr.getJSONObject(i).getDouble("x");
                                    double y = arr.getJSONObject(i).getDouble("y");

                                    points.add(new Point(id, name, x, y));
                                }

                                Log.d("hello", "here");

                                arr = response.getJSONArray("centroids");
                                Log.d("hello", arr.toString());
                                for (int i = 0; i < arr.length(); i++) {

                                    double x = arr.getJSONArray(i).getDouble(0);
                                    double y = arr.getJSONArray(i).getDouble(1);

                                    Log.d("point", x + " " + y);

                                    centroids.add(new Point(x, y));
                                }
                                plotPoints(points, centroids);

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }


                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    VolleyLog.e("Error: ", error.getMessage());
                }
            });

            queue.add(req);

    }


    private void plotPoints(ArrayList<Point> points, ArrayList<Point> centroids) {

        GraphView graph = (GraphView) findViewById(R.id.graph);

        ArrayList<Cluster> clusters = new ArrayList<>();

        for(Point p : centroids) {
            clusters.add(new Cluster(p));
        }

        for(Point p : points) {
            if(p.getX() == null || p.getY() == null) {
                continue;
            }
            Cluster best_c = clusters.get(0);
            double dist = best_c.distFromCentre(p);
            for(Cluster c : clusters) {
                if(c.distFromCentre(p) < dist) {
                    dist = c.distFromCentre(p);
                    best_c = c;
                }
            }

            best_c.addPoint(p);
        }

        int[] colors = {Color.RED, Color.BLUE, Color.GREEN, Color.BLACK};

        for(int i=0; i<clusters.size(); i++) {
            plotCluster(clusters.get(i), graph, colors[i]);
        }

    }

    private void plotCluster(Cluster cluster, GraphView graph, int color) {

        DataPoint[] dataArr = new DataPoint[cluster.size() - 1];

        for(int i=0; i<cluster.getPoints().size(); i++){
            dataArr[i] = new DataPoint(cluster.getPoints().get(i).getX(), cluster.getPoints().get(i).getY());
        }

        PointsGraphSeries<DataPoint> series = new PointsGraphSeries<DataPoint>(dataArr);
        series.setColor(color);
        graph.addSeries(series);

        dataArr = new DataPoint[1];
        dataArr[0] = new DataPoint(cluster.getCentroid().getX(), cluster.getCentroid().getY());
        series = new PointsGraphSeries<DataPoint>(dataArr);
        series.setColor(color);
        series.setShape(PointsGraphSeries.Shape.RECTANGLE);
        series.setOnDataPointTapListener(new OnDataPointTapListener() {
            @Override
            public void onTap(Series series, DataPointInterface dataPointInterface) {
                Log.d("tap", ""+series.getColor());
            }
        });
        graph.addSeries(series);
    }


    private void createPlaylist() {
        String name = "abc";
        String[] tracks = {"spotify:track:2VEZx7NWsZ1D0eJ4uv5Fym", "spotify:track:1pKYYY0dkg23sQQXi0Q5zN", "" +
                "spotify:track:0MyY4WcN7DIfbSmp5yej5z"};

        JSONArray tracks_arr = new JSONArray(Arrays.asList(tracks));


        JSONObject info = new JSONObject();

        try {
            info.put("accessToken", accessToken);
            info.put("tracks", tracks_arr);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        final String URL = "http://10.100.196.75:8888/api/playlist/create/" + name;
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.POST, URL, info,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d("hello", response.toString());
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.e("Error: ", error.getMessage());
            }
        });

        queue.add(req);
    }

    public void set_axis() {
        Spinner x_axis = (Spinner) findViewById(R.id.x_axis);
        Spinner y_axis = (Spinner) findViewById(R.id.y_axis);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.axis_selection, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        x_axis.setAdapter(adapter);
        y_axis.setAdapter(adapter);

        x_axis.setOnItemSelectedListener(this);
        y_axis.setOnItemSelectedListener(this);
    }

    public void onItemSelected(final AdapterView<?> parent, View view,
                               final int pos, long id) {

        if (parent.getId() == R.id.x_axis) {
            graph.getGridLabelRenderer().setHorizontalAxisTitle(parent.getItemAtPosition(pos).toString());
        } else if (parent.getId() == R.id.y_axis) {
            graph.getGridLabelRenderer().setVerticalAxisTitle(parent.getItemAtPosition(pos).toString());
        }
        graph.onDataChanged(true, true); //TODO: CANADA
    }

    public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
    }

    @Override
    public void onLoggedIn() {
        Log.d("MainActivity", "User logged in");
    }

    @Override
    public void onLoggedOut() {
        Log.d("MainActivity", "User logged out");
    }

    @Override
    public void onLoginFailed(Throwable error) {
        Log.d("MainActivity", "Login failed");
    }

    @Override
    public void onTemporaryError() {
        Log.d("MainActivity", "Temporary error occurred");
    }

    @Override
    public void onConnectionMessage(String message) {
        Log.d("MainActivity", "Received connection message: " + message);
    }

    @Override
    protected void onDestroy() {
        // VERY IMPORTANT! This must always be called or else you will leak resources
        Spotify.destroyPlayer(this);
        super.onDestroy();
    }
}