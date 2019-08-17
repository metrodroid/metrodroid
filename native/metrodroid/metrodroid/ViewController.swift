//
//  ViewController.swift
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

import UIKit
import CoreNFC
import metrolib

class ViewController: UIViewController {
    var reader: NFCReader?
    
    override func viewDidLoad() {
        super.viewDidLoad()
        updateObfuscationNotice()
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        updateObfuscationNotice()
    }
    
    @IBAction func scanButton(_ sender: UIButton) {
        reader = NFCReader()
        reader!.start(navigationController: navigationController)
    }
    
    @IBAction func preferencesButton(_ sender: UIButton) {
        Utils.openUrl(url: UIApplication.openSettingsURLString)
    }
    
    @IBOutlet weak var obfuscationLabel: UILabel!
    private func updateObfuscationNotice() {
        let hasNfc = NFCTagReaderSession.readingAvailable
        let obfuscationFlagsOn = (Preferences.init().hideCardNumbers ? 1 : 0) +
            (Preferences.init().obfuscateBalance ? 1 : 0) +
            (Preferences.init().obfuscateTripDates ? 1 : 0) +
            (Preferences.init().obfuscateTripFares ? 1 : 0) +
            (Preferences.init().obfuscateTripTimes ? 1: 0)
        
        if (obfuscationFlagsOn > 0) {
            obfuscationLabel.text = Utils.localizePlural(
                RKt.R.plurals.obfuscation_mode_notice,
                obfuscationFlagsOn, obfuscationFlagsOn)
        } else if (!hasNfc) {
            obfuscationLabel.text = Utils.localizeString(RKt.R.string.nfc_unavailable)
        } else {
            obfuscationLabel.text = nil
        }
    }
}

