var express = require('express'); // Express web server framework
var request = require('request'); // "Request" library
var querystring = require('querystring');
var cookieParser = require('cookie-parser');
var bodyParser = require('body-parser');
var wait = require('wait.for');
var Promise = require('bluebird');


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

// parse application/x-www-form-urlencoded
//app.use(bodyParser.urlencoded({ extended: false }));
// parse application/json
app.use(bodyParser.json());

app.param('x_type', function(req, res, next, type){
  req.x_type = type;
  return next();
});

app.param('y_type', function(req, res, next, type){
  req.y_type = type;
  return next();
});

app.param('id', function(req, res, next, id){
  req.id = id;
  return next();
});

app.param('name', function(req, res, next, name){
  req.name = name;
  return next();
});

app.post('/api/cluster/:x_type/:y_type', function(req, res, next) {
  console.log("got request");
  var accessToken = req.body.accessToken;
  console.log(accessToken);
  getData(req.x_type, req.y_type, accessToken, res, next);
  console.log("end");
});

console.log('Listening on 8888');
app.listen(8888);

//return a promise
function getData(x_type, y_type, access_token, res, next) {
 
  console.log("getting data");
  console.log("x_type: " + x_type + ", y_type: " + y_type);
  var tracks = {
    url: 'https://api.spotify.com/v1/me/tracks?offset=0&limit=50',
    headers: { 'Authorization': 'Bearer ' + access_token },
    json: true,
    limit: 50
  };

  request.get(tracks, function(error, response, body) {
    //res.json(body);
    extractFeatures(body, x_type, y_type, access_token, res, next);
  });
}

function extractFeatures(body, x_type, y_type, access_token, res, next) {

  console.log("extracting features");

  const track_features_url = 'https://api.spotify.com/v1/audio-features/';
  var track_features = {
    url: 'https://api.spotify.com/v1/audio-features/',
    headers: { 'Authorization': 'Bearer ' + access_token },
    json: true
  };

  var axis_values = ['danceability', 'energy', 'loudness', 
                     'speechiness', 'acousticness', 'instrumentalness', 
                     'liveness', 'valence', 'tempo'];
  var user_tracks = [];
  var count = 0;

      var items_length = body.items.length;

      for(var i=0; i<body.items.length; i++) {
        user_tracks.push({});
      }
      
      for(var i=0; i<body.items.length; i++) {
        user_tracks[i] = {"name": body.items[i].track.name, 
                          "id": body.items[i].track.id,
                          "x" : 0,
                          "y" : 0};
        //console.log(user_tracks[i]);        
        track_features.url = track_features_url.concat(user_tracks[i].id);
       
        (function(user_track){
          request.get(track_features, function(error, response, body) {
            //console.log(body);
            user_track['x'] = body[x_type];
            user_track['y'] = body[y_type];
            //console.log(user_track);
            count++;
            if(count == items_length) {
              //console.log("hello");
              //console.log(user_tracks);
              //returnData(user_tracks, res);
              console.log("returning");
              console.log(user_tracks);
              clusterData(user_tracks, res);
              /*
              res.json(
              {
                result : user_tracks
              }
              );
              */
              //res.json(user_tracks);
              
              //callback(user_tracks);
            }
          });
        })(user_tracks[i]); 
        
      }   
}

var KMEANS = require('kmeanie');

function clusterData(user_tracks, res) {

  var points = [];
  for(var i=0; i<user_tracks.length; i++) {
    if(user_tracks[i].x && user_tracks[i].y) {
      points.push([user_tracks[i].x, user_tracks[i].y]);
    }
  }

  var kmeans = new KMEANS();

  kmeans.compile(points, 2, function(err, data){
 
    if(err){ console.error(err); return; }
 
    console.log('converged!');
    console.log(data);

    res.json({
      tracks : user_tracks,
      centroids : data.centers
    });
  });

}


app.post('/api/playlist/create/:name', function(req, res, next) {
  console.log("trying to create playlist");
  var access_token = req.body.accessToken;
  var name = req.name;
  var tracks = req.body.tracks;
  console.log("============== tracks ==========");
  console.log(tracks);
  var options = {
    url: 'https://api.spotify.com/v1/me' ,
    headers: { 'Authorization': 'Bearer ' + access_token },
    json: true
  };

  request.get(options, function(error, response, body) {
    var id = body.id;
    console.log("id = " + id);
    makePlaylist(access_token, id, name, tracks, res);
  });
  console.log("end");
  res.json({
    message : "hello"
  });
});

function makePlaylist(access_token, id, name, tracks, res) {
  var options = {
    url: 'https://api.spotify.com/v1/users/' + id + '/playlists' ,
    headers: { 'Authorization': 'Bearer ' + access_token },
    json: true,
    body: {
      name: name,
      public: true
    }
  };

  request.post(options, function(error, response, body) {
    if(!error){
      console.log(body);
      var playlist_id = body.id;
      console.log("playlist_id = " + playlist_id);
      console.log("success");
      addTracks(access_token, id, playlist_id, name, tracks, res);
    }
  });
}

function addTracks(access_token, user_id, playlist_id, name, tracks, res) {
  var url = 'https://api.spotify.com/v1/users/' + user_id + '/playlists/' + playlist_id + '/tracks';
  console.log(url);
  var options = {
    url: url ,
    headers: { 'Authorization': 'Bearer ' + access_token },
    json: true,
    body: {
      uris : tracks
    }
  };

  request.post(options, function(error, response, body) {
    console.log(error);
    if(!error){
      console.log(body);
      console.log("Tracs added");
    }
  });
}

