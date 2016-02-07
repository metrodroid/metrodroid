var map = L.map('map');

// This tile server gives "retina" tiles which are much nicer on modern
// smartphones. Permission to use the tile server has been granted by
// the tile server operator.
L.tileLayer('https://tile{s}.stackcdn.com/' + (L.Browser.retina ? 'osm_tiles_2x' : 'osm_tiles') + '/{z}/{x}/{y}.png', {
    subdomains: '123456',
    attribution: "&copy; <a href='http://www.openstreetmap.org/'>OpenStreetMap</a>. Tiles: <a href='https://stackptr.com/'>StackPtr</a>"
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


