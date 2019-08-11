//
//  HistoryViewController.swift
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

class HistoryViewController : UITableViewController {
    var cardHistory: [CardPersister.Entry] = []
    
    required init?(coder: NSCoder) {
        super.init(coder: coder)
        do {
            try cardHistory = CardPersister.listCards()
        } catch {
        }
    }
    
    func reload() {
        do {
            try cardHistory = CardPersister.listCards()
        } catch {
        }
        tableView.reloadData()
    }
    
    func exportAllAction(_: UIAlertAction) {
        do {
            let url = try CardPersister.makeZip()
            let activity = UIActivityViewController(
                activityItems: [url],
                applicationActivities: nil
            )
            present(activity,animated: true, completion: nil)
        } catch {
            let alert = Utils.makeAlertDialog(msg: Utils.localizeString( RKt.R.string.ios_nfcreader_exception,
            "\(error)"))
            self.present(alert, animated: true, completion: nil)
        }
    }
    
    func dedupAction(_: UIAlertAction) {
        do {
            let deduped = try CardPersister.dedup()
            let alert = Utils.makeAlertDialog(msg: Utils.localizePlural(RKt.R.plurals.cards_deduped, deduped, deduped))
            self.navigationController?.present(alert, animated: true, completion: nil)
        } catch {
            let alert = Utils.makeErrorScreen(msg: Utils.localizeString(RKt.R.string.ios_nfcreader_exception, "\(error)"))
            self.navigationController?.present(alert, animated: true, completion: nil)
        }
        reload()
    }
    
    func importClipboard(_: UIAlertAction) {
        DispatchQueue.global().async {
            do {
                guard let t = UIPasteboard.general.string else {
                    Utils.showError(viewController: self, msg: Utils.localizeString(RKt.R.string.clipboard_error))
                    return
                }
                let card = try CardSerializer.init().fromPersist(input: t)
                let url = try CardPersister.persistCard(card: card)
                let json = try CardSerializer.init().toJson(card: card)
                DispatchQueue.main.async {
                    self.reload()
                    let alert = Utils.makeAlertDialog(msg: Utils.localizePlural(RKt.R.plurals.cards_imported, 1, 1))
                    self.navigationController?.present(alert, animated: true) {
                        let cr = CardViewController.create(json: json, url: url)
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) {
                            self.navigationController?.pushViewController(cr, animated: true)
                        }
                    }
                }
            } catch {
                Utils.showError(viewController: self, msg: error.localizedDescription)
            }
        }
    }
    
    class func importFile(navigationController: UINavigationController, from url: URL) {
        DispatchQueue.global().async {
            do {
                let (card, url, count) = try CardPersister.readAutodetect(url: url)
                let json: String?
                if card != nil {
                    json = try CardSerializer.init().toJson(card: card!)
                } else {
                    json = nil
                }
                DispatchQueue.main.async {
                    navigationController.viewControllers.forEach{ v in (v as? HistoryViewController)?.reload() }
                    let alert = Utils.makeAlertDialog(msg: Utils.localizePlural(RKt.R.plurals.cards_imported, count, count))
                    navigationController.present(alert, animated: true) {
                        let cr: UIViewController
                        if json != nil && url != nil {
                            cr = CardViewController.create(json: json!, url: url!)
                        } else {
                            let storyboard = UIStoryboard(name: "Main", bundle: nil)
                            cr = storyboard.instantiateViewController(withIdentifier: "HistoryViewController")
                        }
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) {
                                navigationController.pushViewController(cr, animated: true)
                        }
                    }
                }
            } catch {
                Utils.showError(viewController: navigationController, msg: error.localizedDescription)
            }
            do {
                try FileManager.default.removeItem(at: url)
            } catch {}
        }
    }
    
    @objc func menuAction() {
        let optionMenu = UIAlertController(title: nil, message: Utils.localizeString(RKt.R.string.ios_menu_title), preferredStyle: .actionSheet)
        optionMenu.addAction(UIAlertAction(title: Utils.localizeString( RKt.R.string.import_clipboard), style: .default, handler: importClipboard))
        optionMenu.addAction(
            UIAlertAction(title: Utils.localizeString(RKt.R.string.deduplicate_cards), style: .default, handler: dedupAction))
        optionMenu.addAction(
            UIAlertAction(title: Utils.localizeString(RKt.R.string.export_all), style: .default, handler: exportAllAction))
        
        optionMenu.addAction(UIAlertAction(title: Utils.localizeString(RKt.R.string.ios_menu_cancel), style: .cancel))
        
        self.present(optionMenu, animated: true, completion: nil)
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        navigationItem.rightBarButtonItem = UIBarButtonItem(title: Utils.localizeString(RKt.R.string.ios_menu_button), style: .plain, target: self, action: #selector(menuAction))
    }
    
    // Return the number of rows for the table.
    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return cardHistory.count
    }
    
    // Provide a cell object for each row.
    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        // Fetch a cell of the appropriate type.
        let cell = tableView.dequeueReusableCell(withIdentifier: "HistoryCell", for: indexPath)
        
        // Configure the cell’s contents.
        let el = cardHistory[indexPath.item]
        var hiddenSerial: String? = nil
        if (Preferences.init().hideCardNumbers) {
            hiddenSerial =  Utils.localizeString(RKt.R.string.hidden_card_number)
        }
        let unknownCard = Utils.localizeString(RKt.R.string.unknown_card)
        let fallback = "\(unknownCard) \(Utils.directedDash) \(hiddenSerial ?? Utils.weakLTR(el.uid))"
        var card: Card?
        do {
            card = try el.load()
            let ti = card?.parseTransitIdentity()
            if (ti != nil) {
                cell.textLabel!.text =
                "\(ti?.name ?? unknownCard) \(Utils.directedDash) \(card?.label ?? hiddenSerial ?? Utils.weakLTR(ti?.serialNumber ?? el.uid))"
            } else {
                cell.textLabel!.text = fallback
            }
        } catch {
            cell.textLabel!.text = fallback
        }
        if let scanTime = card?.scannedAt {
            cell.detailTextLabel!.attributedText = Utils.localizeFormatted(RKt.R.string.scanned_at_format,
                                                                           TimestampFormatter.init().timeFormat(ts: scanTime),
                                                                           TimestampFormatter.init().dateFormat(ts: scanTime)).attributed
        } else {
            cell.detailTextLabel!.attributedText = nil
        }
        return cell
    }
    
    override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        let el = cardHistory[indexPath.item]
        do {
            guard let json = try el.loadJson() else {
                return
            }
            let cr = try CardViewController.create(json: json, url: el.getUrl())
            navigationController?.pushViewController(cr, animated: true)
        } catch {
            return
        }
    }
    
    override func tableView(_ tableView: UITableView, commit editingStyle: UITableViewCell.EditingStyle, forRowAt indexPath: IndexPath) {
        if editingStyle == .delete {
            let item = indexPath.item
            let el = cardHistory[item]
            do {
                print ("Deleting \(item): \(el.fname)")
                try el.delete()
                cardHistory.remove(at: indexPath.item)
            
                tableView.deleteRows(at: [indexPath], with: .fade)
            } catch {
                print ("Error deleting")
            }
        }
    }
}
