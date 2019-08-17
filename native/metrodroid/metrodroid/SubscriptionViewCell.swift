//
//  SubscriptionViewCell.swift
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

class SubscriptionViewCell : UICollectionViewCell {
    @IBOutlet weak var agencyLabel: UILabel!
    @IBOutlet weak var nameLabel: UILabel!
    @IBOutlet weak var validityLabel: UILabel!
    @IBOutlet weak var tripsLabel: UILabel!
    @IBOutlet weak var extraInfoTable: UITableView!
    @IBOutlet weak var paxLabel: UILabel!
    @IBOutlet weak var paxImage: UIImageView!
    @IBOutlet weak var usedLabel: UILabel!
    @IBOutlet weak var daysLabel: UILabel!
    var extraInfoDataSource: ListViewDataSource?
    func setSubscription(_ formatted: Subscription.Formatted) {
        agencyLabel?.attributedText = formatted.shortAgencyLabel?.attributed
        nameLabel?.text = formatted.subscriptionName
        validityLabel?.attributedText = formatted.validity?.attributed
        tripsLabel.text = formatted.remainingTrips
        
        if let subInfo = formatted.info {
            extraInfoTable.isHidden = false
            print("subinfo.size = \(subInfo.count)")
            extraInfoDataSource = ListViewDataSource(items: subInfo)
            extraInfoTable.delegate = extraInfoDataSource
            extraInfoTable.dataSource = extraInfoDataSource
            extraInfoTable.reloadData()
        } else {
            print("no subinfo")
            extraInfoTable.isHidden = true
            extraInfoTable.delegate = nil
            extraInfoTable.dataSource = nil
        }
        Utils.renderPax(paxLabel: paxLabel, paxImage: paxImage, pax: Int(formatted.passengerCount))
        let subState = formatted.subscriptionState
        if (subState == Subscription.SubscriptionState.unknown) {
            usedLabel?.text = nil
        } else {
            usedLabel?.text = Utils.localizeString(subState.descriptionRes)
        }
        if let remainingDays = formatted.remainingDayCount {
            daysLabel?.text = Utils.localizePlural(RKt.R.plurals.remaining_day_count,
                                                   Int(truncating: remainingDays), remainingDays)
        } else {
            daysLabel?.text = nil
        }
    }
}
