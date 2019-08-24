//
//  TripsViewController.swift
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
//

import Foundation
import UIKit
import metrolib

class TripsViewController : UITableViewController, CardViewProtocol {
    var trips: [TripSection?] = []
    
    func setTransitData(card: Card, transitData: TransitData?) -> Bool {
        guard let t1 = transitData?.trips else {
            return false
        }
        trips = TripSection.Companion.init().sectionize(trips: t1)
        if (trips.count == 0) {
            let cell = UITableViewCell.init(style: .default, reuseIdentifier: "EmptyTrip")
            cell.textLabel?.text = Utils.localizeString(RKt.R.string.no_trip_data)
            cell.textLabel?.numberOfLines = 0
            cell.textLabel?.textAlignment = .center
            tableView.backgroundView = cell
            tableView.separatorStyle = .none
        } else {
            tableView.backgroundView = nil
            tableView.separatorStyle = .singleLine
        }
        return true
    }
    
    // Return the number of rows for the table.
    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return trips[section]?.trips.count ?? 0
    }
    
    override func tableView(_ tableView: UITableView, titleForHeaderInSection section: Int) -> String? {
        if let nd = trips[section]?.date {
            return TimestampFormatter.init().longDateFormat(
                ts: nd).unformatted
        } else {
            return Utils.localizeString(RKt.R.string.unknown_date_title)
        }
    }
    
    override func numberOfSections(in tableView: UITableView) -> Int {
        return trips.count
    }
    
    // Provide a cell object for each row.
    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let trip = trips[indexPath.section]?.trips[indexPath.item]
        
        let cell = tableView.dequeueReusableCell(withIdentifier: "TripCell", for: indexPath) as! TripCell
        
        // Configure the cell’s contents.
        cell.renderTrip(trip: trip!)
        return cell
    }
    
    override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        guard let trip = trips[indexPath.section]?.trips[indexPath.item] else {
            return
        }
        guard let m = MapViewController.create(trip: trip) else {
            return
        }
        navigationController?.pushViewController(m, animated: true)
    }
}
