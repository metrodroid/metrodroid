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
    class func isSupported(cardInfo: CardInfo) -> Bool {
        // Classic and vicinity are not supported currently
        return cardInfo.iOSSupported as? Bool ?? (cardInfo.cardType == CardType.iso7816 || cardInfo.cardType == CardType.mifaredesfire || cardInfo.cardType == CardType.felica || cardInfo.cardType == CardType.mifareultralight || cardInfo.cardType == CardType.vicinity)
    }
    var items = CardInfoRegistry.init().allCardsAlphabetical.filter {
        isSupported(cardInfo: $0) }
    
    // MARK: - UICollectionViewDataSource protocol
    
    // tell the collection view how many cells to make
    override func collectionView(_ collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int {
        print("count: \(self.items.count)")
        return self.items.count
    }
    
    // make a cell for each cell index path
    override func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        print("generating \(indexPath)")
        
        // get a reference to our storyboard cell
        let cell = collectionView.dequeueReusableCell(withReuseIdentifier: reuseIdentifier, for: indexPath as IndexPath) as! SupportedCardsViewCell
        
        // Use the outlet in our custom class to get a reference to the UILabel in the cell
        cell.setCardInfo(self.items[indexPath.item])
        
        return cell
    }
}

