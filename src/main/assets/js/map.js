var map = L.map('map');

var tileUrl = TripMapShim.getTileUrl();
var subdomains = TripMapShim.getSubdomains();

if (subdomains.indexOf(',') >= 0) {
    subdomains = subdomains.split(',');
}

var attribution = 'Custom Map Tiles';
if (tileUrl.indexOf('stackcdn.com') >= 0) {
    attribution = "&copy; <a href='http://www.openstreetmap.org/'>OpenStreetMap</a>. Tiles: <a href='https://stackptr.com/'>StackPtr</a>";
}
L.tileLayer(tileUrl, {
    subdomains: subdomains,
    attribution: attribution
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


