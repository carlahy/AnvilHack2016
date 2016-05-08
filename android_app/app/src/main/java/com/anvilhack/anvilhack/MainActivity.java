package com.anvilhack.anvilhack;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.PointsGraphSeries;
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


public class MainActivity extends Activity implements ConnectionStateCallback {

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
                //makeRequest();
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

        final String URL = "http://10.100.196.75:8888/api/cluster/danceability/loudness";
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.POST, URL, info,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d("hello", response.toString());
                        ArrayList<Point> points = new ArrayList<>();
                        ArrayList<Point> centroids = new ArrayList<>();
                        try {
                            JSONArray arr = response.getJSONArray("tracks");
                            for(int i=0; i<arr.length(); i++) {
                                String name = arr.getJSONObject(i).getString("name");
                                String id = arr.getJSONObject(i).getString("id");
                                if(arr.getJSONObject(i).isNull("x") || arr.getJSONObject(i).isNull("y")) {
                                    continue;
                                }
                                double x = arr.getJSONObject(i).getDouble("x");
                                double y = arr.getJSONObject(i).getDouble("y");

                                points.add(new Point(id, name, x, y));
                                Log.d("track", id);
                            }

                            Log.d("hello", "here");

                            arr = response.getJSONArray("centroids");
                            Log.d("hello", arr.toString());
                            for(int i=0; i<arr.length(); i++) {

                                double x = arr.getJSONArray(i).getDouble(0);
                                double y = arr.getJSONArray(i).getDouble(1);

                                Log.d("point", x + " " + y);

                                centroids.add(new Point(x, y));
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        plotPoints(points, Color.BLUE);
                        plotPoints(centroids, Color.RED);

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.e("Error: ", error.getMessage());
            }
        });

        queue.add(req);
    }


    private void plotPoints(ArrayList<Point> points, int color) {

        GraphView graph = (GraphView) findViewById(R.id.graph);

        ArrayList<DataPoint> dataPoints = new ArrayList<>();

        for(Point p : points) {
            if(p.getX() != null && p.getY() != null) {
                dataPoints.add(new DataPoint(p.getX(), p.getY()));
            }
        }

        DataPoint[] dataArr = new DataPoint[dataPoints.size()];
        for(int i=0; i<dataArr.length; i++){
            dataArr[i] = dataPoints.get(i);
        }

        PointsGraphSeries<DataPoint> series = new PointsGraphSeries<DataPoint>(dataArr);
        series.setColor(color);
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