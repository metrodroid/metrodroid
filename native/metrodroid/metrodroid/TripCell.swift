//
//  TripCell.swift
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

class TripCell : UITableViewCell {
    @IBOutlet weak var modeImage: UIImageView!
    @IBOutlet weak var routeLabel: UILabel!
    @IBOutlet weak var stationsLabel: UILabel!
    @IBOutlet weak var machineLabel: UILabel!
    @IBOutlet weak var fareLabel: UILabel!
    @IBOutlet weak var timeLabel: UILabel!
    @IBOutlet weak var transferImage: UIImageView!
    @IBOutlet weak var rejectedImage: UIImageView!
    @IBOutlet weak var paxLabel: UILabel!
    @IBOutlet weak var paxImage: UIImageView!
    
    func boldAttr(bold: NSAttributedString?, normal: NSAttributedString?) -> NSAttributedString? {
        if (bold == nil && normal == nil) {
            return nil
        }
        var boldRange: NSRange? = nil
        let result = NSMutableAttributedString()
        if (bold != nil) {
            result.append(bold!)
            boldRange = NSMakeRange(0, bold!.string.count)
        }
        if (bold != nil && normal != nil) {
            result.append(NSAttributedString(string: " "))
        }
        if (normal != nil) {
            result.append(normal!)
        }
        let fontSize = UIFont.systemFontSize
        let normalAttribute = [
            NSAttributedString.Key.font: UIFont.systemFont(ofSize: fontSize),
        ]
        let fullRange = NSMakeRange(0, result.string.count)
        result.addAttributes(normalAttribute, range: fullRange)
        let boldAttribute = [
            NSAttributedString.Key.font: UIFont.boldSystemFont(ofSize: fontSize),
        ]
        if let range = boldRange {
            result.addAttributes(boldAttribute, range: range)
        }
        return result
    }
    
    let modeMap = [
        "bus",
        "train",
        "tram",
        "metro",
        "ferry",
        "tvm",
        "vending_machine",
        "cashier_yen",
        "unknown",
        "banned",
        "trolleybus",
        "car", // TOLL_ROAD
        "monorail"
    ]
    
    func renderTrip(trip: Trip) {
        var routeName : FormattedString? = trip.routeName
        if (Preferences.init().rawLevel != TransitData.RawLevel.none) {
            if let raw = trip.getRawFields(level: Preferences.init().rawLevel) {
                routeName = (routeName ?? FormattedString(input: "")).plus(b: FormattedString(input: " <" + raw + ">"))
            }
        }
        routeLabel?.attributedText = boldAttr(bold: trip.getAgencyName(isShort: true)?.attributed,
                                              normal: routeName?.attributed)
        timeLabel.attributedText = Trip.Companion.init().formatTimes(trip: trip)?.attributed
        stationsLabel?.attributedText = Trip.Companion.init().formatStationNames(trip: trip)?.attributed
        fareLabel.attributedText = trip.fare?.formatCurrencyString(isBalance: false).attributed
        if let machine = trip.vehicleID {
            machineLabel.text = Utils.localizeString(RKt.R.string.vehicle_number, machine)
        } else if let machine = trip.machineID {
            machineLabel.text = Utils.localizeString(RKt.R.string.machine_id_format, machine)
        } else {
            machineLabel.text = nil
        }
        
        var idx = Int(trip.mode.idx)
        if (idx < 0 || idx >= modeMap.count) {
            idx = Int(Trip.Mode.other.idx)
        }
        modeImage.image = UIImage(named: modeMap[idx])
        modeImage.accessibilityLabel = Utils.localizeString(
            trip.mode.contentDescription)
        Utils.setImageVisibility(image: transferImage, visible: trip.isTransfer)
        Utils.setImageVisibility(image: rejectedImage, visible: trip.isRejected)
        Utils.renderPax(paxLabel: paxLabel, paxImage: paxImage, pax: Int(trip.passengerCount))
    }
}
