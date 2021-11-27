//
//  NFCReader.swift
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

class NFCReader {
    
    private var navigationController: UINavigationController?

    func postDump(card: Card) {
        do {
            let json = try CardSerializer.init().toPersist(card: card)
            print ("json=\(json)")
            let url = try CardPersister.persistCard(card: card, json: json)
            if Preferences.init().speakBalance {
                if let balance = card.safeBalance?.formatCurrencyString(isBalance: true).unformatted {
                    let balanceStr = Utils.localizeString(Rstring.init().balance_speech, balance)
                    Utils.speakText(voiceOutdata: balanceStr)
                }
            }
            DispatchQueue.main.async {
                let cr = CardViewController.create(json: json, url: url)
                self.navigationController?.pushViewController(cr, animated: true)
            }
        } catch {
            Utils.showError(viewController: self.navigationController!, msg: Utils.localizeString(Rstring.init().ios_nfcreader_exception, "\(error)"))
        }
    }
    
    func start(navigationController: UINavigationController?) {
        self.navigationController = navigationController
        CardReaderSessionIOS.init().readTag(postDump: self.postDump)
    }
}
