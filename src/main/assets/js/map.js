var map = L.map('map');

L.tileLayer("http://otile{s}.mqcdn.com/tiles/1.0.0/osm/{z}/{x}/{y}.png", {
    subdomains: "1234",
    attribution: "&copy; <a href='http://www.openstreetmap.org/'>OpenStreetMap</a>. Tiles: <a href='http://www.mapquest.com/'>MapQuest</a>"
 }).addTo(map);

// We acknowledge this in the license screen.
map.attributionControl.setPrefix('');

//map.setView([-33.865, 151.209444], 13);

var bounds = L.latLngBounds([]);

for (var i=0; i<TripMapShim.getMarkerCount(); i++) {
    var m = TripMapShim.getMarker(i);
    var ll = [m.getLat(), m.getLong()];
    bounds.extend(ll);
    L.marker(ll).bindPopup(m.getHTML()).addTo(map);
}

map.setMaxBounds(bounds.pad(1)).fitBounds(bounds.pad(0.1));


