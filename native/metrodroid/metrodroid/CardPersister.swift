//
//  CardPersister.swift
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
import metrolib
import ZIPFoundation
import CommonCrypto

class CardPersister {
    class func cardsDirectory() throws -> URL {
        let DocumentsDirectory = FileManager().urls(for: .documentDirectory, in: .userDomainMask).first!
        let dir = DocumentsDirectory.appendingPathComponent("cards")
        try FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true, attributes: nil)
        return dir
    }
    
    class func newFileName(card: Card) throws -> URL {
        var generation = 0
        while true {
            let at = ExportHelperKt.makeFilename(card: card, generation: Int32(generation))
            let u = try cardsDirectory().appendingPathComponent(at)
            if (!FileManager.default.fileExists(atPath: u.path)) {
                return u
            }
            generation += 1
        }
    }
    
    class func persistCard(card: Card, json: String) throws -> URL {
        let fname = try newFileName(card: card)
        try json.write(toFile: fname.path, atomically: true, encoding: String.Encoding.utf8)
        return fname
    }
    
    class func persistCard(card: Card) throws -> URL {
        return try persistCard(card: card, json: CardSerializer.init().toPersist(card: card))
    }
    
    class Entry :NSObject {
        let fname: String
        let dateGen: String
        let uid: String
        let date: Date
        
        init?(fname: String) {
            self.fname = fname
            if (!fname.hasPrefix("Metrodroid-") || !fname.hasSuffix(".json")){
                return nil
            }
            
            let main = fname[fname.index(fname.startIndex, offsetBy: 11)..<fname.index(fname.endIndex, offsetBy: -5)]
            guard let sepIdx = main.firstIndex(of: "-") else {
                return nil
            }
            self.uid = String(main[main.startIndex..<sepIdx])
            self.dateGen = String(main[main.index(sepIdx, offsetBy: +1)..<main.endIndex])
            guard let dateSepFirstIdx = self.dateGen.firstIndex(of: "-") else {
                return nil
            }
            guard let dateSepLastIdx = self.dateGen.lastIndex(of: "-") else {
                return nil
            }
            let dateStr: String
            if (dateSepFirstIdx != dateSepLastIdx) {
                dateStr = String(self.dateGen[self.dateGen.startIndex..<dateSepLastIdx])
            } else {
                dateStr = self.dateGen
            }
            let dateFormatter = DateFormatter()
            dateFormatter.dateFormat = "yyyyMMdd-HHmmss"
            dateFormatter.timeZone = TimeZone.init(secondsFromGMT: 0)
            guard let date = dateFormatter.date(from: dateStr) else {
                return nil
            }
            self.date = date
        }
        
        override var description: String {
            get {
                return "\(fname) [\(uid); \(dateGen)]"
            }
            set {
            }
        }
        
        func getUrl() throws -> URL {
            return try CardPersister.cardsDirectory().appendingPathComponent(fname)
        }
        
        func loadJson() throws -> String? {
            let url = try getUrl()
            return CardPersister.loadJsonAtUrl(url: url)
        }
        
        func load() throws -> Card? {
            guard let json = try loadJson() else {
                return nil
            }
            return try CardSerializer.init().fromPersist(input: json)
        }
        
        class Info {
            let label: String?
            let transitName: String?
            let transitSerialNumber: String?
            let cardType: CardType
            
            init (card: Card) {
                label = card.label
                let ti = card.safeTransitIdentity
                transitName = ti?.name
                transitSerialNumber = ti?.serialNumber
                cardType = card.cardType
            }
        }
        
        lazy var info: Info? = {
            guard let card = try? load() else {
                return nil
            }
            return Info(card: card)
        }()
        
        func matches(query: String) -> Bool {
            let inf = info
            if inf?.label?.localizedCaseInsensitiveContains(query) ?? false
                || uid.localizedCaseInsensitiveContains(query)
                || inf?.transitName?.localizedCaseInsensitiveContains(query) ?? false
                || inf?.transitSerialNumber?.localizedCaseInsensitiveContains(query) ?? false
                || inf?.cardType.description().localizedCaseInsensitiveContains(query) ?? false
            {
                return true
            }
            if inf?.transitName == nil
                && Utils.localizeString(RKt.R.string.unknown_card).localizedCaseInsensitiveContains(query) {
                return true
            }
            return false
        }
        
        func delete() throws {
            try FileManager.default.removeItem(at: getUrl())
        }
        
        func getSha512() throws -> String {
            let ret = try sha512(url: getUrl())
            print("\(fname) -> \(ret)")
            return ret
        }
    }
    
    class Group : NSObject {
        let uid : String
        var entries : [Entry]
        
        init(entry : Entry) {
            uid = entry.uid
            entries = [entry]
        }
    }
    
    class func loadJsonAtUrl(url: URL) -> String? {
        guard let jsonRaw = FileManager.default.contents(atPath: url.path) else {
            return nil
        }
        return String(data: jsonRaw, encoding: .utf8)!
    }
    
    class func listCards() throws -> [Entry] {
        return try FileManager.default.contentsOfDirectory(atPath: cardsDirectory().path).compactMap { Entry (fname: $0) }.sorted(by: {
            a,b in a.date > b.date
        })
    }
    
    class func listGroupedCards() throws -> [Group] {
        let cards = try listCards()
        var groups : [Group] = []
        var mapUid: Dictionary<String, Int> = [:]
        for card in cards {
            if let idx = mapUid[card.uid] {
                groups[idx].entries.append(card)
            } else {
                let idx = groups.count
                groups.append(Group(entry: card))
                mapUid[card.uid] = idx
            }
        }
        return groups
    }
    
    class func makeTempFile() throws -> URL {
        let temporaryDirectoryURL = URL(fileURLWithPath: NSTemporaryDirectory(),
                                        isDirectory: true)
        let dir =  temporaryDirectoryURL.appendingPathComponent("sharezip")
        try FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true, attributes: nil)
        return dir.appendingPathComponent("Metrodroid-" + UUID.init().uuidString + ".zip")
    }

    class func addZipFileFromString(archive: Archive, name: String, contents: String) throws {
        let bin = contents.data(using: .utf8)!
        
        try archive.addEntry(with: name, type: .file, uncompressedSize: UInt32(bin.count),
                             provider: { (position, size) -> Data in return bin}
        )
    }
    
    class func makeZip() throws -> URL {
        let url = try makeTempFile()
        try FileManager.default.zipItem(at: cardsDirectory(), to: url, shouldKeepParent: false)
        let archive = Archive(url: url, accessMode: .update)!
        let now = TimestampFull.Companion.init().now()
        try addZipFileFromString(archive: archive, name: "README." + Localizer.init().language + ".txt",
                             contents: Utils.localizeString(RKt.R.string.exported_at, now.format()) + Utils.getDeviceString())
        try addZipFileFromString(archive: archive, name: "README.txt",
                             contents: Utils.englishString(RKt.R.string.exported_at, now.isoDateTimeFormat()) + Utils.getDeviceStringEnglish())

        return url
    }
    
    class func readBinaryEntry(arch: Archive, entry: ZIPFoundation.Entry) throws -> Data {
        let res = NSMutableData(length: 0)!
        _ = try arch.extract(entry, consumer: { (data) in
            res.append(data)
        })
        return res as Data
    }
    
    class func readStringEntry(arch: Archive, entry: ZIPFoundation.Entry) throws -> String {
        return String(data: try readBinaryEntry(arch: arch, entry: entry), encoding: .utf8)!
    }
    
    class func readJson(jsonUrl: URL) throws -> (Card?, URL?, Int) {
        let data = try Data(contentsOf: jsonUrl)
        try FileManager.default.removeItem(at: jsonUrl)
        let card = try CardSerializer.init().fromPersist(input: String(data: data, encoding: .utf8)!)
        let importUrl = try CardPersister.persistCard(card: card)
        return (card, importUrl, 1)
    }    
    
    class XMLAST: NodeWrapper {
        var children: [XMLAST]
        
        var childNodes: [NodeWrapper] {
            get {
                return children
            }
        }
        
        let nodeName: String
        
        var inner: String?
        
        let parent: XMLAST?
        let attributes: [String : String]
        init(name : String, attributes: [String : String], parent: XMLAST?) {
            self.nodeName = name
            self.parent = parent
            self.attributes = attributes
            self.inner = ""
            self.children = []
        }
        
        func addChild(child: XMLAST) {
            children.append(child)
        }
        
        func addInner(_ s: String) {
            inner = (inner ?? "") + s
        }
    }
    
    class XMLDelegate: NSObject, XMLParserDelegate {
        var isFirst = true
        var cardDepth = 0
        var depth = 0
        var valid = true
        var astRoot : XMLAST? = nil
        var curElement: XMLAST? = nil
        var lastCard: Card? = nil
        var lastUrl: URL? = nil
        var cardCount: Int = 0
        func parser(_ parser: XMLParser, didStartElement elementName: String, namespaceURI: String?, qualifiedName qName: String?, attributes attributeDict: [String : String] = [:]) {
            if (isFirst) {
                if elementName == "cards" {
                    cardDepth = 2
                } else if elementName == "card" {
                    cardDepth = 1
                } else {
                    valid = false
                    parser.abortParsing()
                }
            }
            isFirst = false
            depth += 1
            if depth == cardDepth {
                astRoot = XMLAST(name: elementName, attributes: attributeDict, parent: nil)
                curElement = astRoot
            }
            if depth > cardDepth {
                let newElement = XMLAST(name: elementName, attributes: attributeDict, parent: curElement)
                curElement?.addChild(child: newElement)
                curElement = newElement
            }
        }
        func parser(_ parser: XMLParser, foundCharacters string: String) {
            curElement?.addInner(string)
        }
        private func parseCard() {
            print("astRoot=\(String(describing: astRoot))")
            guard let root = astRoot else {
                return
            }
            guard let card = try? XmlCardFormatKt.readCardXML(root: root) else {
                return
            }
            lastUrl = try? persistCard(card: card)
            lastCard = card
            cardCount += 1
        }
        func parser(_ parser: XMLParser, didEndElement elementName: String, namespaceURI: String?, qualifiedName qName: String?) {
            if depth == cardDepth {
                parseCard()
                astRoot = nil
            }
            depth -= 1
            curElement = curElement?.parent
        }
        func parserDidEndDocument(_ parser: XMLParser) {
            parseCard()
            astRoot = nil
        }
        func parserDidStartDocument(_ parser: XMLParser) {
            print("Start document")
        }
        func collapse() -> (Card?, URL?, Int)  {
            if cardCount == 1 {
                return (lastCard, lastUrl, 1)
            }
            return (nil, nil, cardCount)
        }
    }
    
    class func readXml(xmlUrl: URL) throws -> (Card?, URL?, Int) {
        let parser = XMLParser.init(contentsOf: xmlUrl)
        let delegate = XMLDelegate()
        parser?.delegate = delegate
        parser?.parse()
        return delegate.collapse()
    }
    
    class func readAutoJson(jsonUrl: URL) throws -> (Card?, URL?, Int) {
        let data = try Data(contentsOf: jsonUrl)
        try FileManager.default.removeItem(at: jsonUrl)
        let cards = try CardSerializer.init().fromAutoJson(json: String(data: data, encoding: .utf8)!)
        var count = 0
        var lastImportUrl: URL? = nil
        var lastCard: Card? = nil
        while (cards.hasNext()) {
            let card = cards.next() as! Card
            lastImportUrl = try CardPersister.persistCard(card: card)
            lastCard = card
            count += 1
        }
        if count == 1 {
            return (lastCard, lastImportUrl, 1)
        }
        return (nil, nil, count)
    }

    class func readZip(zipUrl: URL) throws -> (Card?, URL?, Int) {
        let arch = Archive(url: zipUrl, accessMode: .read)!
        var count = 0
        let jsonFormat = JsonKotlinFormat()
        let mfcFormat = MfcCardImporter()
        var singleCard: Card? = nil
        var singleURL: URL? = nil
        for entry in arch.makeIterator() {
            let name = entry.path(using: .utf8)
            if (name.hasSuffix(".json")) {
                let str = try readStringEntry(arch: arch, entry: entry)
                let card = jsonFormat.readCard(input: str)
                let url = try persistCard(card: card)
                count += 1
                if count == 1 {
                    singleCard = card
                    singleURL = url
                } else {
                    singleCard = nil
                    singleURL = nil
                }
                continue
            }
            if (name.hasSuffix(".mfc")) {
                let bin = try readBinaryEntry(arch: arch, entry: entry)
                let card = mfcFormat.readCard(bin: UtilsKt.toByteArray(bin))
                let url = try persistCard(card: card)
                count += 1
                if count == 1 {
                    singleCard = card
                    singleURL = url
                } else {
                    singleCard = nil
                    singleURL = nil
                }
                continue
            }
        }
        
        return (singleCard, singleURL, count)
    }
    
    class func getUrlSize(url: URL) -> Int? {
        return try? url.resourceValues(forKeys:[.fileSizeKey]).fileSize
    }

    class func readAutodetect(url: URL, largeFileDelegate: (Int) -> Bool) throws -> (Card?, URL?, Int) {
        let i = InputStream.init(url: url)!
        i.open()
        let head = UnsafeMutablePointer<UInt8>.allocate(capacity: 16)
        defer {
            head.deallocate()
        }
        let r = i.read(head, maxLength: 1)
        i.close()
        print("Head byte \(head[0]) [\(r)] \(String(describing: i.streamError))")
        
        if head[0] == 0x50 {
            return try readZip(zipUrl: url)
        }
                
        if head[0] == 0x7b {
            if let size = getUrlSize(url: url) {
                if size > 4194304 && !largeFileDelegate(size) {
                    return (nil, nil, 0)
                }
            }
            return try readAutoJson(jsonUrl: url)
        }

        if head[0] == 0x3c {
            if let size = getUrlSize(url: url) {
                if size > 4194304 && !largeFileDelegate(size) {
                    return (nil, nil, 0)
                }
            }
            return try readXml(xmlUrl: url)
        }
        
        return (nil, nil, 0)
    }
    
    class func sha512(url: URL) throws -> String {
        let bufSize = 8192
        let i = InputStream.init(url: url)!
        i.open()
        defer {
            i.close()
        }
        let buf = UnsafeMutablePointer<UInt8>.allocate(capacity: bufSize)
        defer {
            buf.deallocate()
        }
        var context = CC_SHA512_CTX()
        CC_SHA512_Init(&context)
        while true {
            let r = i.read(buf, maxLength: bufSize)
            if r <= 0 {
                break
            }
            CC_SHA512_Update(&context, buf, numericCast(r))
        }
        
        var digest = Data(count: Int(CC_SHA512_DIGEST_LENGTH))
        digest.withUnsafeMutableBytes { (ptr: UnsafeMutableRawBufferPointer) -> Void in
            _ = CC_SHA512_Final(ptr.baseAddress?.assumingMemoryBound(to: UInt8.self), &context)
        }
        
        return digest.map { String(format: "%02hhx", $0) }.joined()
    }
    
    class func dedup() throws -> Int {
        let hashes = NSMutableSet()
        var count = 0
        for card in try listCards() {
            let hash = try card.getSha512()
            if hashes.contains(hash) {
                try card.delete()
                count += 1
            }
            hashes.add(hash)
        }
        return count
    }
}
