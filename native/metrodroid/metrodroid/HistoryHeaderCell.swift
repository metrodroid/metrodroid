//
//  HistoryHeaderCell.swift
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

class HistoryHeaderCell: UITableViewHeaderFooterView {
    @IBOutlet private weak var title: UILabel!
    @IBOutlet private weak var arrow: UIImageView!
    private var delegate: HistoryViewController? = nil
    private var section: Int = 0
    
    func setExpansion(expanded: Bool) {
        self.arrow.image = UIImage(named: expanded ? "expanded_header" : "collapsed_header")
    }
    
    func setState(title: String, delegate: HistoryViewController, section: Int,
                  expanded: Bool) {
        self.title.text = title
        self.delegate = delegate
        self.section = section
        setExpansion(expanded: expanded)
    }
    
    func additionalElements() {
        addGestureRecognizer(UITapGestureRecognizer(target: self,
                                                    action: #selector(tapHeader)))
        backgroundView = UIView(frame: bounds)
        backgroundView?.backgroundColor = UIColor.opaqueSeparator
    }
    
    override init(reuseIdentifier: String?) {
        super.init(reuseIdentifier: reuseIdentifier)
        additionalElements()
    }
    
    required init?(coder: NSCoder) {
        super.init(coder: coder)
        additionalElements()
    }
    
    @objc func tapHeader() {
        setExpansion(expanded: delegate?.toggleSection(sectionNumber: section) ?? false)
    }
}
