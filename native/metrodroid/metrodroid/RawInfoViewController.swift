//
//  RawInfoViewController.swift
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

class RawInfoViewController : UITableViewController, CardViewProtocol, ErrorLabelProtocol {
    var delegate: ListViewDataSource?
    @IBOutlet weak var errorView: UILabel!
    
    func setErrorLabel(msg: String?, bgColor: UIColor?) {
        errorView?.text = msg
        errorView?.backgroundColor = bgColor
        if msg == nil {
            tableView.tableHeaderView = nil
        } else {
            tableView.tableHeaderView = errorView
        }
    }
    
    func setTransitData(card: Card, transitData: TransitData?) -> Bool {
        let info = card.rawData ?? []
        let scanTime = card.scannedAt
        let scannedAt = Utils.localizeFormatted(RKt.R.string.scanned_at_format,
                                                TimestampFormatter.init().timeFormat(ts: scanTime),
                                                TimestampFormatter.init().dateFormat(ts: scanTime))
        let infoCompleted = [ListItem(name_: scannedAt)] + info
        delegate = ListViewDataSource(items: infoCompleted)
        tableView.delegate = delegate
        tableView.dataSource = delegate
        return true
    }
}
