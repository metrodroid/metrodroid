//
// MapViewController.swift
//
// Copyright 2019 Google
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.
//

import Foundation
import UIKit
import MapKit
import metrolib

class MapViewController : UIViewController {
    @IBOutlet weak var mapView: MKMapView!
    var annot: [MKPointAnnotation] = []
    var region: MKCoordinateRegion? = nil
    
    override func viewDidLoad() {
        super.viewDidLoad()
        mapView?.addAnnotations(annot)
        mapView?.setRegion(region!, animated: false)
    }
    
    class func stationToCoordinate(station: Station) -> CLLocationCoordinate2D? {
        let lat = CLLocationDegrees(truncating: station.latitude ?? 0)
        let lon = CLLocationDegrees(truncating: station.longitude ?? 0)
        if (abs(lat) < 1e-5 && abs(lon) < 1e-5) {
            return nil
        }
        return CLLocationCoordinate2D(
            latitude: lat,
            longitude: lon
        )
    }
    
    class func stationToPoint(station: Station) -> MKPointAnnotation? {
        let point = MKPointAnnotation()
        guard let coord = stationToCoordinate(station: station) else {
            return nil
        }
        point.coordinate = coord
        point.title = station.getStationName(isShort: false).unformatted
        return point
    }
    
    class func mapCenter(coords: [CLLocationCoordinate2D]) -> CLLocationCoordinate2D {
        if coords.count < 2 {
            return coords[0]
        }
        return CLLocationCoordinate2D(
            latitude: (coords[0].latitude + coords[1].latitude) / 2,
            longitude: (coords[0].longitude + coords[1].longitude) / 2
        )
    }
    
    class func mapSize(coords: [CLLocationCoordinate2D]) -> CLLocationDistance {
        if coords.count < 2 {
            return 1000
        }
        let point1 = MKMapPoint(coords[0])
        let point2 = MKMapPoint(coords[1])
        let dist = point1.distance(to: point2)
        if (dist < 700) {
            return 1000
        }
        return dist * 1.5
    }
    
    class func create(trip: Trip) -> MapViewController? {
        let stations : [Station] = [trip.startStation, trip.endStation].compactMap { $0 }
        let coords = stations.compactMap { MapViewController.stationToCoordinate(station: $0) }
        let annot = stations.compactMap { MapViewController.stationToPoint(station: $0) }
        if (annot.isEmpty) {
            return nil
        }
        let storyboard = UIStoryboard(name: "Main", bundle: nil)
        let cr = storyboard.instantiateViewController(withIdentifier: "MapViewController") as! MapViewController
        cr.annot = annot
        cr.title = Trip.Companion.init().formatStationNames(trip: trip)?.unformatted
        let sz = mapSize(coords: coords)
        cr.region = MKCoordinateRegion(center: mapCenter(coords: coords), latitudinalMeters: sz, longitudinalMeters: sz)
        return cr
    }
}
