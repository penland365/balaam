var express        = require('express');
var app            = express();

const port = 4000; //process.env.PORT;

// static files
app.use(express.static('public'));

app.get('/ping', function(req, res) {
  res.send("PONG").end();
})

app.listen(port, function () {
  console.log('balaam-web listening on port ' + port);
});
