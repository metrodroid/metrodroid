//
//  SupportedCardsViewController.swift
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

class SupportedCardsViewController: UICollectionViewController {
    
    let reuseIdentifier = "cell" // also enter this string as the cell identifier in the storyboard
    // Classic is not supported currently
    static let supportedProtocols: [CardType] = [
        .felica, .iso7816, .mifaredesfire, .mifareultralight, .mifareplus, .vicinity]
    class func isSupported(cardInfo: CardInfo) -> Bool {
        return cardInfo.iOSSupported as? Bool ?? supportedProtocols.contains(cardInfo.cardType)
    }
    
    class Section {
        let region: TransitRegion
        let cards: [CardInfo]
        var expanded: Bool
        init(region: TransitRegion, filtering: [CardInfo]) {
            self.region = region
            self.cards = filtering.filter {
                isSupported(cardInfo: $0) }
            expanded = false
        }
        
        var isEmpty: Bool {
            get {
                return cards.isEmpty
            }
        }
    }
    var items = CardInfoRegistry.init().allCardsByRegion.map { Section(
        region: $0.first as! TransitRegion,
        filtering: $0.second as! [CardInfo]) }.filter { !$0.isEmpty }
    
    // MARK: - UICollectionViewDataSource protocol
    
    override func numberOfSections(in collectionView: UICollectionView) -> Int {
        return self.items.count
    }
    
    // tell the collection view how many cells to make
    override func collectionView(_ collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int {
        return items[section].expanded ? self.items[section].cards.count : 0
    }
    
    // make a cell for each cell index path
    override func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        print("generating \(indexPath)")
        
        // get a reference to our storyboard cell
        let cell = collectionView.dequeueReusableCell(withReuseIdentifier: reuseIdentifier, for: indexPath as IndexPath) as! SupportedCardsViewCell
        
        // Use the outlet in our custom class to get a reference to the UILabel in the cell
        cell.setCardInfo(self.items[indexPath.section].cards[indexPath.item])
        
        return cell
    }
    
    func toggleSection(sectionNumber: Int) -> Bool {
        print ("Toggle \(sectionNumber)")
        let expanded = !items[sectionNumber].expanded
        items[sectionNumber].expanded = expanded
        collectionView.reloadSections(NSIndexSet(index: sectionNumber) as IndexSet)
        return expanded
    }
    
    override func collectionView(_ collectionView: UICollectionView, viewForSupplementaryElementOfKind kind: String, at indexPath: IndexPath) -> UICollectionReusableView {
        if let sectionHeader = collectionView.dequeueReusableSupplementaryView(ofKind: kind, withReuseIdentifier: "SupportedCardsHeader", for: indexPath) as? SupportedCardsHeader {
            sectionHeader.setState(
                title: self.items[indexPath.section].region.translatedName,
                count:self.items[indexPath.section].cards.count,
                delegate: self,
                section: indexPath.section,
                expanded: items[indexPath.section].expanded)
            return sectionHeader
        }
        return UICollectionReusableView()
    }
}

