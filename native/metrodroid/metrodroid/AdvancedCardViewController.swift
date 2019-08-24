//
//  AdvancedCardViewController.swift
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

class AdvancedCardViewController: UITabBarController {
    var card: Card?
    var url: URL?
    var tdError: String?
    var isCritical: Bool = false
    
    class func create(card: Card, url: URL, error: String? = nil, isCritical: Bool = false) -> AdvancedCardViewController {
        let storyboard = UIStoryboard(name: "Main", bundle: nil)
        let cr = storyboard.instantiateViewController(withIdentifier: "AdvancedCardViewController") as! AdvancedCardViewController
        cr.card = card
        cr.url = url
        cr.tdError = error
        cr.isCritical = isCritical
        return cr
    }
    
    func copyJsonAction(_: UIAlertAction) {
        if let json = CardPersister.loadJsonAtUrl(url: url!) {
            UIPasteboard.general.string = json
            let alert = Utils.makeAlertDialog(msg: Utils.localizeString(RKt.R.string.copied_to_clipboard))
            self.present(alert, animated: true, completion: nil)
        }
    }
    
    func shareJsonAction(_: UIAlertAction) {
        let activity = UIActivityViewController(
            activityItems: [url!],
            applicationActivities: nil
        )
        present(activity,animated: true, completion: nil)
    }
    
    @objc func menuAction() {
        let optionMenu = UIAlertController(title: nil, message: Utils.localizeString(RKt.R.string.export_), preferredStyle: .actionSheet)
            optionMenu.addAction(
                UIAlertAction(title: Utils.localizeString(RKt.R.string.copy_xml), style: .default, handler: copyJsonAction))
        optionMenu.addAction(
            UIAlertAction(title: Utils.localizeString(RKt.R.string.share_xml), style: .default, handler: shareJsonAction))
        optionMenu.addAction(UIAlertAction(title: Utils.localizeString(RKt.R.string.ios_menu_cancel), style: .cancel))
        
        self.present(optionMenu, animated: true, completion: nil)
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        navigationItem.rightBarButtonItem = UIBarButtonItem(title: Utils.localizeString(RKt.R.string.export_), style: .plain, target: self, action: #selector(menuAction))
    }
    
    override func viewWillAppear(_ animated: Bool) {
        var hiddenSerial: String? = nil
        if (Preferences.init().hideCardNumbers) {
            hiddenSerial = Utils.localizeString(RKt.R.string.hidden_card_number)
        }
        let unknown = Utils.localizeString(RKt.R.string.unknown)
        title = "\(card?.cardType.name ?? unknown) - \(hiddenSerial ?? card?.tagId.toHexString() ?? unknown)"
        self.viewControllers = self.viewControllers?.filter { v in
            ((v as? CardViewProtocol)?.setTransitData(card: card!, transitData: nil) ?? false)
        }
        var tdErrorColor: UIColor?
        if tdError == nil {
            tdErrorColor = nil
        } else if (isCritical) {
            tdErrorColor = UIColor.red
        } else {
            tdErrorColor = UIColor.orange
        }
        self.viewControllers?.forEach { v in
            (v as? ErrorLabelProtocol)?.setErrorLabel(msg: tdError, bgColor: tdErrorColor)
        }
    }
}
