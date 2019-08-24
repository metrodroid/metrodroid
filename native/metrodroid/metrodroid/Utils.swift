//
//  Utils.swift
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

import AVFoundation
import UIKit
import metrolib

class Utils {
    class func openUrl(url: String) {
        guard let typedUrl = URL(string: url) else {
            return
        }
        
        if UIApplication.shared.canOpenURL(typedUrl) {
            UIApplication.shared.open(typedUrl, completionHandler: { (success) in
                print("URL opened: \(success)")
            })
        }
    }
    
    class func getVersion() -> String? {
        return Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String
    }
    
    class func getModelIdentifier() -> String {
        var systemInfo = utsname()
        uname(&systemInfo)
        let machineMirror = Mirror(reflecting: systemInfo.machine)
        let identifier = machineMirror.children.reduce("") { identifier, element in
            guard let value = element.value as? Int8, value != 0 else { return identifier }
            return identifier + String(UnicodeScalar(UInt8(value)))
        }
        return identifier
    }
    
    class func getModelName(modelId: String) -> String? {
        guard let iOSDeviceModelsPath = Bundle.main.path(forResource: "iOSDeviceModelMapping", ofType: "plist") else { return "" }
        guard let iOSDevices = NSDictionary(contentsOfFile: iOSDeviceModelsPath) else { return nil }
        
        return iOSDevices.value(forKey: modelId) as? String
    }
    
    class func getDeviceStringReal(localize: (StringResource, Any?...) -> String) -> String {
        let modelId = getModelIdentifier()
        let modelName = getModelName(modelId: modelId) ?? localize(RKt.R.string.unknown)
        let version = getVersion() ?? localize(RKt.R.string.unknown)
        return localize(RKt.R.string.app_version, version) + "\n" +
            localize(RKt.R.string.device_model, modelName, modelId) + "\n" + localize(RKt.R.string.ios_version, UIDevice.current.systemVersion)
    }
    
    class func getDeviceString() -> String {
        return Utils.getDeviceStringReal(localize: Utils.localizeString(_:_:))
    }

    class func getDeviceStringEnglish() -> String {
        return Utils.getDeviceStringReal(localize: Utils.englishString(_:_:))
    }

    class func loadResourceFileString(resource: String) -> String? {
        if let filepath = Bundle.main.path(forResource: resource, ofType: "txt") {
            do {
                return try String(contentsOfFile: filepath)
            } catch {
                return nil
            }
        } else {
            return nil
        }
    }
    
    class func localizeString(_ res: StringResource, _ args: Any?...) -> String {
        return Localizer.init().localizeString(res: res, v: KotlinArray.init(size: Int32(args.count), init: {
            args[Int(truncating: $0)]
        }))
    }
    
    class func englishString(_ res: StringResource, _ args: Any?...) -> String {
        return Localizer.init().englishString(res: res, v: KotlinArray.init(size: Int32(args.count), init: {
            args[Int(truncating: $0)]
        }))
    }
    
    class func localizeFormatted(_ res: StringResource, _ args: Any?...) -> FormattedString {
        return Localizer.init().localizeFormatted(res: res, v: KotlinArray.init(size: Int32(args.count), init: {
            args[Int(truncating: $0)]
        }))
    }
    
    class func localizePlural(_ res: PluralsResource, _ plural: Int, _ args: Any?...) -> String {
        return Localizer.init().localizePlural(res: res, count: Int32(plural), v: KotlinArray.init(size: Int32(args.count), init: {
            args[Int(truncating: $0)]
        }))
    }
    
    class func makeAlertDialog(msg: String) -> UIViewController {
        let alertController = UIAlertController(title: Utils.localizeString(RKt.R.string.app_name), message:
            msg, preferredStyle: .alert)
        alertController.addAction(UIAlertAction(title: Utils.localizeString(RKt.R.string.ios_error_ok), style: .cancel))
        return alertController
    }
    
    class func setImageVisibility(image: UIImageView, visible: Bool) {
        if (visible) {
            image.isHidden = false
            image.setContentCompressionResistancePriority(UILayoutPriority(rawValue: 750), for: .horizontal)
            image.setContentCompressionResistancePriority(UILayoutPriority(rawValue: 750), for: .vertical)
        } else {
            image.isHidden = true
            image.setContentCompressionResistancePriority(UILayoutPriority(rawValue: 0), for: .horizontal)
            image.setContentCompressionResistancePriority(UILayoutPriority(rawValue: 0), for: .vertical)
        }
    }
    
    class func speakText(voiceOutdata: String ) {
        DispatchQueue.global().async {
            print("Speaking \(voiceOutdata)")
            do {
                try AVAudioSession.sharedInstance().setCategory(AVAudioSession.Category.playAndRecord, mode: .default, options: .defaultToSpeaker)
                try AVAudioSession.sharedInstance().setActive(true, options: .notifyOthersOnDeactivation)
            } catch {
                print("audioSession properties weren't set because of an error.")
            }
            
            let utterance = AVSpeechUtterance(string: voiceOutdata)
            utterance.voice = AVSpeechSynthesisVoice()
            
            let synth = AVSpeechSynthesizer()
            synth.speak(utterance)
            
            disableAVSession()
        }
    }
    
    private class func disableAVSession() {
        do {
            try AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
        } catch {
            print("audioSession properties weren't disabled.")
        }
    }
    
    class func renderPax(paxLabel: UILabel, paxImage: UIImageView, pax: Int) {
        if pax <= 0 {
            paxLabel.text = nil
            paxImage.image = nil
            Utils.setImageVisibility(image: paxImage, visible: false)
        } else {
            paxLabel.text = "\(pax)"
            paxImage.image = UIImage(named: (pax == 1) ? "ic_person": "ic_group")
            paxImage.accessibilityLabel = Utils.localizePlural(RKt.R.plurals.passengers, Int(pax), pax)
            Utils.setImageVisibility(image: paxImage, visible: true)
        }
    }
    
    class func makeErrorScreen(msg: String) -> UIViewController {
        let fullMsg = Utils.localizeString(RKt.R.string.error) + ": " + msg
        let a = UIAlertController(title: Utils.localizeString(RKt.R.string.error), message: fullMsg, preferredStyle: .alert)
        a.addAction(UIAlertAction(title: Utils.localizeString(RKt.R.string.ios_error_ok), style: .cancel, handler: nil))
        return a
    }
    
    class func showError(viewController: UIViewController, msg: String) {
        DispatchQueue.main.async {
            viewController.present(makeErrorScreen(msg: msg), animated: true)
        }
    }
    
    class func weakLTR(_ input: String) -> String {
        if UIView.userInterfaceLayoutDirection(for: .unspecified) == .rightToLeft {
            return "\u{200E}" + input + "\u{200E}"
        }
        return input
    }
    
    static var directedDash: String {
        get {
            if UIView.userInterfaceLayoutDirection(for: .unspecified) == .rightToLeft {
                return "\u{200F}-\u{200F}"
            }
            return "-"
        }
    }
}
