//
//  SupportedCardsViewCell.swift
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

class SupportedCardsViewCell: UICollectionViewCell {
    @IBOutlet weak var cardName: UILabel!
    
    @IBOutlet weak var cardImage: UIImageView!
    @IBOutlet weak var cardLocation: UILabel!
    @IBOutlet weak var cardNotice: UILabel!
    
    func getCardInfoDrawable(ci: CardInfo) -> UIImage? {
        guard let imageId = ci.imageId else {
            return nil
        }
        guard let image = UIImage(named: imageId.id) else {
            return nil
        }
        guard let imageAlphaId = ci.imageAlphaId else {
            return image
        }
        guard let imageAlpha = UIImage(named: imageAlphaId.id) else {
            return image
        }
        print("Masked bitmap \(imageId.id) / \(imageAlphaId.id)")
        if (image.size != imageAlpha.size) {
            print("Source image (\(image.size)) and mask (\(imageAlpha.size)) are not the same size -- returning image without alpha")
            return image
        }
        UIGraphicsBeginImageContextWithOptions(image.size, false, image.scale)
        image.draw(at: CGPoint.zero)
        imageAlpha.draw(at: CGPoint.zero, blendMode: .destinationIn, alpha: 1.0)
        return UIGraphicsGetImageFromCurrentImageContext()
    }
    
    func getNoteText(_ ci: CardInfo) -> String? {
        var notes = ""
        if let li = ci.iOSExtraNote {
            notes += Utils.localizeString(li)
        } else if let li = ci.resourceExtraNote {
            notes += Utils.localizeString(li)
        }
        if (ci.preview) {
            notes += " " + Utils.localizeString(RKt.R.string.card_preview_reader)
        }
        if notes == "" {
            return nil
        }
        return notes
    }

    func setCardInfo(_ ci: CardInfo) {
        cardName.text = ci.name
        if let li = ci.locationId {
            cardLocation.text = Utils.localizeString(li)
            cardLocation.isHidden = false
        } else {
            cardLocation.isHidden = true
        }
        cardNotice.text = getNoteText(ci)
        cardImage.image = getCardInfoDrawable(ci: ci)
        layer.cornerRadius = 8.0
    }
}
