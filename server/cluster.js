var KMEANS = require('kmeanie');
 
  //utility function to get random points
 
  var getPoints = function(count, dimensions, min, max){
 
    var points = [];
 
    for(var i = 0 ; i < count; i++){
      var arr = [];
      for(var j = 0 ; j < dimensions; j++){
        arr.push(min + Math.random()*(max - min));
      }
      points.push(arr);
    }
 
    return points;
 
  };
 
  var kmeans = new KMEANS();
 
  //1,000 2D points
 
  var randomPoints = getPoints(1000, 2, -10000, 10000);

  randomPoints = [[1,1], [1,2], [2,1], [10, 10], [9, 10], [10, 9]];
 
  //time how long it takes for centers to converge
  console.time('kmeans2D-1K');
 
  //listen for centers positions update
  //this is handy for animations
  kmeans.onCentersUpdated = function(newCenters, iteration){
    console.log('iteration: ' + iteration);
    console.dir(newCenters);
  };
 
  //compile the kmeans algorithm with the point cloud, the number of desired clusters and a cb
  kmeans.compile(randomPoints, 2, function(err, data){
 
    if(err){ console.error(err); return; }
 
    console.log('converged!');
 
    console.log(data);
 
    console.timeEnd('kmeans2D-1K');
  });
