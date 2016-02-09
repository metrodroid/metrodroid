var map = L.map('map');

// This tile server gives "retina" tiles which are much nicer on modern
// smartphones. Permission to use the tile server has been granted by
// the tile server operator.
//
// The tile stylesheets are available under an open source license at:
// https://github.com/stackunderflow-stackptr/stackptr_tools/tree/master/osm-bright
L.tileLayer('https://tile{s}.stackcdn.com/osm_tiles' + (L.Browser.retina ? '_2x' : '') + '/{z}/{x}/{y}.png', {
    subdomains: '123456',
    attribution: "&copy; <a href='http://www.openstreetmap.org/'>OpenStreetMap</a>. Tiles: <a href='https://stackptr.com/'>StackPtr</a>"
 }).addTo(map);

// We acknowledge this in the license screen.
map.attributionControl.setPrefix('');

var bounds = L.latLngBounds([]);

for (var i=0; i<TripMapShim.getMarkerCount(); i++) {
    var m = TripMapShim.getMarker(i);
    var ll = [m.getLat(), m.getLong()];
    bounds.extend(ll);
    var icon = L.icon({
        iconUrl: 'img/' + m.getIcon() + '.png',
        iconRetinaUrl: 'img/' + m.getIcon() + '-2x.png',
        iconSize:    [25, 41],
		iconAnchor:  [12, 41],
		popupAnchor: [1, -34],
		shadowSize:  [41, 41]
    });

    L.marker(ll, {icon: icon}).bindPopup(m.getHTML()).addTo(map);
}

map.setMaxBounds(bounds.pad(1)).fitBounds(bounds.pad(0.1));


