//
//  ListViewDelegate.swift
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

class ListItemRender {
    let text1: FormattedString?
    let text2: FormattedString?
    let level: Int
    let url: String?
    let isHeader: Bool
    
    init(li: ListItem, level: Int) {
        text1 = li.text1
        text2 = li.text2
        url = (li as? UriListItem)?.uri
        self.level = level
        self.isHeader = li is HeaderListItem && level == 0
    }
    
    class func addOffset(_ input: FormattedString?, _ level: Int) -> NSAttributedString? {
        if input == nil {
            return nil
        }
        let result = NSMutableAttributedString(attributedString: input!.attributed)
        let style : NSMutableParagraphStyle = NSParagraphStyle.default.mutableCopy() as! NSMutableParagraphStyle
        style.alignment = NSTextAlignment.left
        style.firstLineHeadIndent = CGFloat(10 * level)
        style.headIndent = CGFloat(10 * level)
        let attribute = [
            NSAttributedString.Key.paragraphStyle: style
        ]
        let fullRange = NSMakeRange(0, result.string.count)
        result.addAttributes(attribute, range: fullRange)
        return result
    }
    
    func getText1() -> NSAttributedString? {
        return ListItemRender.addOffset(text1, level)
    }
    
    func getText2() -> NSAttributedString? {
        return ListItemRender.addOffset(text2, level)
    }
}

class ListViewSection {
    let title: String?
    let items: [ListItemRender]
    
    init(title: String?, items: [ListItemRender]) {
        self.title = title
        self.items = items
    }
}

class ListViewDataSource : NSObject, UITableViewDataSource, UITableViewDelegate {
    let sections: [ListViewSection]
    
    class func flatRecurse(output: inout [ListItemRender], input: [ListItem], level: Int) {
        for cur in input {
            if let rec = cur as? metrolib.ListItemRecursive {
                output.append(ListItemRender(li: cur, level: level))
                if let subTree = rec.subTree {
                    flatRecurse(output: &output, input: subTree, level: level + 1)
                }
                continue
            }
            
            output.append(ListItemRender(li: cur, level: level))
        }
    }
    
    // TODO: Remove this once we have tree
    class func flatten(_ input: [ListItem]) -> [ListItemRender] {
        if input.first(where: {el in el is metrolib.ListItemRecursive}) == nil {
            return input.map { ListItemRender(li: $0, level: 0) }
        }
        var res: [ListItemRender] = []
        for cur in input {
            if let rec = cur as? metrolib.ListItemRecursive {
                if cur.text2 == nil {
                    if let title = cur.text1 {
                        res.append(ListItemRender(li: HeaderListItem(title_: title), level: 0))
                    }
                    if let subTree = rec.subTree {
                        flatRecurse(output: &res, input: subTree, level: 0)
                    }
                } else {
                    res.append(ListItemRender(li: cur, level: 0))
                    if let subTree = rec.subTree {
                        flatRecurse(output: &res, input: subTree, level: 1)
                    }
                }
                continue
            }
            res.append(ListItemRender(li: cur, level: 0))
        }
        return res
    }
    

    init(items: [ListItem]) {
        var res: [ListViewSection] = []
        var curSection: [ListItemRender] = []
        var sectionTitle: String?
        for cur in ListViewDataSource.flatten(items) {
            if (cur.isHeader) {
                if (!curSection.isEmpty || sectionTitle != nil) {
                    res.append(ListViewSection(title: sectionTitle, items: curSection))
                }
                curSection = []
                sectionTitle = cur.text1?.unformatted
                continue
            }
            curSection.append(cur)
        }
        if (!curSection.isEmpty || sectionTitle != nil) {
            res.append(ListViewSection(title: sectionTitle, items: curSection))
        }
            
        sections = res
    }
    
    // Return the number of rows for the table.
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return sections[section].items.count
    }
    
    func tableView(_ tableView: UITableView, titleForHeaderInSection section: Int) -> String? {
        return sections[section].title
    }
    
    func numberOfSections(in tableView: UITableView) -> Int {
        return sections.count
    }
    
    // Provide a cell object for each row.
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let item = sections[indexPath.section].items[indexPath.item]
        
        let cell = tableView.dequeueReusableCell(withIdentifier: "ListItemCell", for: indexPath) 
        
        // Configure the cell’s contents.
        cell.textLabel?.attributedText = item.getText1()
        cell.textLabel?.numberOfLines = 0
        cell.detailTextLabel?.attributedText = item.getText2()
        cell.detailTextLabel?.numberOfLines = 0
        return cell
    }
    
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        if let item = sections[indexPath.section].items[indexPath.item].url {
            Utils.openUrl(url: item)
        }
    }
}
