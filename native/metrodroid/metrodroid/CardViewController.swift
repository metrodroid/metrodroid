//
//  CardViewController.swift
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

class CardViewController: UITabBarController {
    var card: Card?
    var td: TransitDataStored?
    var url: URL?
    
    class func create(card: Card, url: URL) -> UITabBarController {
        let storyboard = UIStoryboard(name: "Main", bundle: nil)
        let cr = storyboard.instantiateViewController(withIdentifier: "CardViewController") as! CardViewController
        cr.card = card
        cr.url = url
        var err: String? = nil
        var td: TransitDataStored?
        do {
            try TransitDataStored.Storer.init().parse(card: card, result: &td)
        } catch {
            err = error.localizedDescription
        }
        if (td == nil || err != nil) {
            return AdvancedCardViewController.create(card: card, url: url,
                                                        error:
                err ?? Utils.localizeString(RKt.R.string.unknown_card_description), isCritical: err != nil)
        }
        cr.td = td
        return cr
    }
    
    class func create(json: String, url: URL) -> UIViewController {
        do {
            return create(card: try CardSerializer.init().fromPersist(input: json), url: url)
        } catch {
            return Utils.makeErrorScreen(msg: error.localizedDescription)
        }
    }
    
    func copyNumberAction(_: UIAlertAction) {
        UIPasteboard.general.string = td?.serialNumber ?? card?.tagId.toHexString()
        let alert = Utils.makeAlertDialog(msg: Utils.localizeString(RKt.R.string.copied_to_clipboard))
        self.present(alert, animated: true, completion: nil)
    }
    
    func advancedInfoAction(_: UIAlertAction) {
        let cr = AdvancedCardViewController.create(card: card!, url: url!)
        navigationController?.pushViewController(cr, animated: true)
    }
    
    func aboutFormatAction(_: UIAlertAction) {
        if let url = td?.moreInfoPage {
            Utils.openUrl(url: url)
        }
    }
    
    func onlineServicesAction(_: UIAlertAction) {
        if let url = td?.onlineServicesPage {
            Utils.openUrl(url: url)
        }
    }
    
    @objc func menuAction() {
        let optionMenu = UIAlertController(title: nil, message: Utils.localizeString(RKt.R.string.ios_menu_title), preferredStyle: .actionSheet)
        if (!Preferences.init().hideCardNumbers) {
            optionMenu.addAction(
                UIAlertAction(title: Utils.localizeString(RKt.R.string.copy_card_number), style: .default, handler: copyNumberAction))
        }
        optionMenu.addAction(
            UIAlertAction(title: Utils.localizeString(RKt.R.string.advanced_info), style: .default, handler: advancedInfoAction))
        if td?.moreInfoPage != nil {
            optionMenu.addAction(
                UIAlertAction(title: Utils.localizeString(RKt.R.string.about_card_format), style: .default, handler: aboutFormatAction))
        }
        if td?.onlineServicesPage != nil {
            optionMenu.addAction(
                UIAlertAction(title: Utils.localizeString(RKt.R.string.online_services), style: .default, handler: onlineServicesAction))
        }
        optionMenu.addAction(UIAlertAction(title: Utils.localizeString(RKt.R.string.ios_menu_cancel), style: .cancel))
        
        self.present(optionMenu, animated: true, completion: nil)
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        navigationItem.rightBarButtonItem = UIBarButtonItem(title: Utils.localizeString(RKt.R.string.ios_menu_button), style: .plain, target: self, action: #selector(menuAction))
    }
    
    override func viewWillAppear(_ animated: Bool) {
        var hiddenSerial: String? = nil
        if (Preferences.init().hideCardNumbers) {
            hiddenSerial = Utils.localizeString(RKt.R.string.hidden_card_number)
        }
        let unknown = Utils.localizeString(RKt.R.string.unknown)
        title = "\(td?.cardName ?? unknown) \(Utils.directedDash) \(hiddenSerial ?? (td?.serialNumber ?? card?.tagId.toHexString()).flatMap(Utils.weakLTR) ?? unknown)"
        self.viewControllers = self.viewControllers?.filter { v in
            ((v as? CardViewProtocol)?.setTransitData(card: card!, transitData: td) ?? false)
        }
    }
}
