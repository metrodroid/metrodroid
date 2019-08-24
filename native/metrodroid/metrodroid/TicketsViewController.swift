//
//  TicketsViewController.swift
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

class TicketsViewController: UICollectionViewController, CardViewProtocol {
    var balances: [TransitBalance] = []
    var subs: [Subscription] = []
    
    func setTransitData(card: Card, transitData: TransitData?) -> Bool {
        balances = transitData?.balances ?? []
        let sub = transitData?.subscriptions
        if (balances.isEmpty && sub == nil) {
            return false
        }
        subs = sub ?? []
        return true
    }
    
    // MARK: - UICollectionViewDataSource protocol
    
    // tell the collection view how many cells to make
    override func collectionView(_ collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int {
        return self.balances.count + self.subs.count
    }
    
    // make a cell for each cell index path
    override func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        print("generating \(indexPath)")
        
        let item = indexPath.item
        
        if (item >= balances.count) {
            let cell = collectionView.dequeueReusableCell(
                withReuseIdentifier: "subscription",
                for: indexPath as IndexPath) as! SubscriptionViewCell
            
            do {
                try cell.setSubscription(subs[item - balances.count].format())
            } catch {
                let errorCell = collectionView.dequeueReusableCell(
                    withReuseIdentifier: "error",
                    for: indexPath as IndexPath) as! ErrorViewCell
                errorCell.errorMessage.text = error.localizedDescription
                return errorCell
            }
            return cell

        }
        
        let cell = collectionView.dequeueReusableCell(withReuseIdentifier: "balance", for: indexPath as IndexPath) as! BalanceViewCell
        
        cell.setBalance(balances[item])
                
        return cell
    }
}
