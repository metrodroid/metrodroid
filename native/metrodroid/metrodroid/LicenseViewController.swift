//
//  LicenseViewController.swift
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
import metrolib

class LicenseViewController: UIViewController {
    @IBOutlet weak var licenseTextView: UITextView!

    override func viewDidLoad() {
        super.viewDidLoad()
        
        var licenseText = ""
        
        licenseText += readLicenseTextFromAsset("Metrodroid-NOTICE")
        licenseText += readLicenseTextFromAsset("Logos-NOTICE")
        licenseText += readLicenseTextFromAsset("NOTICE.AOSP")
        licenseText += readLicenseTextFromAsset("NOTICE.noto-emoji")
        licenseText += readLicenseTextFromAsset("NOTICE.protobuf")
        
        for factory in CardInfoRegistry.init().allFactories {
            guard let notice = factory.notice else {
                continue
            }
            licenseText += notice
            licenseText += "\n\n"
        }
        
        licenseTextView.text = licenseText
    }
    
    func readLicenseTextFromAsset(_ res: String) -> String {
        return (Utils.loadResourceFileString(resource: res) ?? "error loading \(res)") + "\n\n"
    }
}
