//
//  SupportedCardsHeader.swift
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

class SupportedCardsHeader : UICollectionReusableView {
    @IBOutlet private weak var label: UILabel!
    @IBOutlet private weak var arrow: UIImageView!
    @IBOutlet weak var cardCount: UILabel!
    private var delegate: SupportedCardsViewController? = nil
    private var section: Int = 0
    
    func additionalElements() {
        addGestureRecognizer(UITapGestureRecognizer(target: self,
                                                    action: #selector(tapHeader)))
    }

    override init(frame: CGRect) {
        super.init(frame: frame)
        additionalElements()
    }
    
    required init?(coder: NSCoder) {
        super.init(coder: coder)
        additionalElements()
    }
    
    func setExpansion(expanded: Bool) {
        self.arrow.image = UIImage(named: expanded ? "expanded_header" : "collapsed_header")
    }
    
    @objc func tapHeader() {
        setExpansion(expanded: delegate?.toggleSection(sectionNumber: section) ?? false)
    }
    
    func setState(title: String, count: Int, delegate: SupportedCardsViewController, section: Int,
                  expanded: Bool) {
        self.label.text = title
        self.cardCount.text = Utils.localizePlural(
            RKt.R.plurals.supported_cards_format, count, count)
        self.delegate = delegate
        self.section = section
        setExpansion(expanded: expanded)
    }
}
