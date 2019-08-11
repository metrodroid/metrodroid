//
//  SelfSizedTableView.swift
//  metrodroid
//
//  Created by Vladimir Serbinenko on 20.07.19.
//  Copyright © 2019 Vladimir. All rights reserved.
//

import Foundation
import UIKit

class SelfSizedTableView: UITableView {
    override func reloadData() {
        super.reloadData()
        self.invalidateIntrinsicContentSize()
        self.layoutIfNeeded()
    }
    
    override var intrinsicContentSize: CGSize {
        return CGSize(width: contentSize.width, height: contentSize.height+30)
    }
}
