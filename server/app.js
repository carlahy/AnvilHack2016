var express = require('express'); // Express web server framework
var request = require('request'); // "Request" library
var querystring = require('querystring');
var cookieParser = require('cookie-parser');


var client_id = '92685aa42f484569b44bbf91733a5bbb'; // Your client id
var client_secret = 'f38f01e2a2d04d34ba69b62e4bac8197'; // Your client secret
var redirect_uri = 'http://localhost:8888/callback'; // Your redirect uri

/**
 * Generates a random string containing numbers and letters
 * @param  {number} length The length of the string
 * @return {string} The generated string
 */
var generateRandomString = function(length) {
  var text = '';
  var possible = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';

  for (var i = 0; i < length; i++) {
    text += possible.charAt(Math.floor(Math.random() * possible.length));
  }
  return text;
};

var stateKey = 'spotify_auth_state';

var app = express();

//app.use(express.static(__dirname + '/public'))
app.use(cookieParser());

app.get('/login', function(req, res) {

  var state = generateRandomString(16);
  res.cookie(stateKey, state);

  // your application requests authorization
  var scope = 'user-library-read';
  res.redirect('https://accounts.spotify.com/authorize?' +
    querystring.stringify({
      response_type: 'code',
      client_id: client_id,
      scope: scope,
      redirect_uri: redirect_uri,
      state: state
    }));
});

app.get('/callback', function(req, res) {

  // your application requests refresh and access tokens
  // after checking the state parameter

  var code = req.query.code || null;
  var state = req.query.state || null;
  var storedState = req.cookies ? req.cookies[stateKey] : null;

  if (state === null || state !== storedState) {
    res.redirect('/#' +
      querystring.stringify({
        error: 'state_mismatch'
      }));
  } else {
    res.clearCookie(stateKey);
    var authOptions = {
      url: 'https://accounts.spotify.com/api/token',
      form: {
        code: code,
        redirect_uri: redirect_uri,
        grant_type: 'authorization_code'
      },
      headers: {
        'Authorization': 'Basic ' + (new Buffer(client_id + ':' + client_secret).toString('base64'))
      },
      json: true
    };

    request.post(authOptions, function(error, response, body) {
      if (!error && response.statusCode === 200) {

        var access_token = body.access_token,
            refresh_token = body.refresh_token;

        var options = {
          url: 'https://api.spotify.com/v1/me/tracks?offset=0&limit=50',
          headers: { 'Authorization': 'Bearer ' + access_token },
          json: true,
          limit: 50
        };

        var tracks = {
          url: 'https://api.spotify.com/v1/me/tracks?offset=0&limit=50',
          headers: { 'Authorization': 'Bearer ' + access_token },
          json: true,
          limit: 50
        };

        const track_features_url = 'https://api.spotify.com/v1/audio-features/';
        var track_features = {
          url: 'https://api.spotify.com/v1/audio-features/',
          headers: { 'Authorization': 'Bearer ' + access_token },
          json: true
        };

        // use the access token to access the Spotify Web API
        request.get(options, function(error, response, body) {
          //console.log(body);
        });

        // WE ARE GOING TO DO STUFF HERE
        var axis_values = ['danceability', 'energy', 'loudness', 'speechiness', 'acousticness', 'instrumentalness', 'liveness', 'valence', 'tempo'];
        var user_tracks = [];
        request.get(tracks, function(error, response, body) {
            console.log("=== Tracks ===");
            
            for(var i=0; i<body.items.length; i++) {
              user_tracks.push({"name": body.items[i].track.name, "id": body.items[i].track.id });
              track_features.url = track_features_url.concat(user_tracks[i].id);
              (function(user_track){
                request.get(track_features, function(error, response, body) {
                    user_track.x = body['danceability'];
                    user_track.y = body['speechiness'];
                    console.log(user_track);
                });
              })(user_tracks[i]);
            }    
        });

        res.json({
          message: "hello"
        });

        // we can also pass the token to the browser to make requests from there
        /*
        res.redirect('/#' +
          querystring.stringify({
            access_token: access_token,
            refresh_token: refresh_token
          }));
        */
      } else {
        res.redirect('/#' +
          querystring.stringify({
            error: 'invalid_token'
          }));
      }
    });
  }
});

app.param('x', function(req, res, next, x){
    req.x = x;
    next();
});

app.param('y', function(req, res, next, y){
    req.y = y;
    next();
});

app.get('/api/cluster/:x/:y', function(request, response, body) {
      var axis_values = ['danceability', 'energy', 'loudness', 'speechiness', 'acousticness', 'instrumentalness', 'liveness', 'valence', 'tempo'];
        var user_tracks = [];
        var x_axis = request.x;
        var y_axis = request.y;
        request.get(tracks, function(error, response, body) {
            console.log("=== Tracks ===");
            
            for(var i=0; i<body.items.length; i++) {
              user_tracks.push({"name": body.items[i].track.name, "id": body.items[i].track.id });
              track_features.url = track_features_url.concat(user_tracks[i].id);
              (function(user_track){
                request.get(track_features, function(error, response, body) {
                    user_track.x = body[x_axis];
                    user_track.y = body[y_axis];
                    console.log(user_track);
                });
              })(user_tracks[i]);
            }    
        });
});

app.get('/refresh_token', function(req, res) {

  // requesting access token from refresh token
  var refresh_token = req.query.refresh_token;
  var authOptions = {
    url: 'https://accounts.spotify.com/api/token',
    headers: { 'Authorization': 'Basic ' + (new Buffer(client_id + ':' + client_secret).toString('base64')) },
    form: {
      grant_type: 'refresh_token',
      refresh_token: refresh_token
    },
    json: true
  };

  request.post(authOptions, function(error, response, body) {
    if (!error && response.statusCode === 200) {
      var access_token = body.access_token;
      res.send({
        'access_token': access_token
      });
    }
  });
});

console.log('Listening on 8888');
app.listen(8888);
