//
//  NFCReader.swift
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
import CoreNFC
import UIKit
import metrolib

class NFCReader : NSObject, NFCTagReaderSessionDelegate, TagReaderFeedbackInterface {
    
    private var session: NFCReaderSession?
    private var navigationController: UINavigationController?
    
    func tagReaderSessionDidBecomeActive(_ session: NFCTagReaderSession) {
        print("NFC Session became active")
    }
    
    func tagReaderSession(_ session: NFCTagReaderSession, didInvalidateWithError error: Error) {
        print("NFC Session end \(error)")
    }
    
    class DesfireWrapper : ISO7816TransceiverSwiftWrapper {
        var tag: NFCMiFareTag
        
        init(tag: NFCMiFareTag) {
            self.tag = tag
        }
        
        func getIdentifier() -> Data {
            return tag.identifier
        }
        
        func transmit(input: Data, channel__ channel: Kotlinx_coroutines_coreSendChannel) {
            tag.sendMiFareISO7816Command(NFCISO7816APDU(data: input)!, completionHandler: {data,sw1,sw2,err in
                ISO7816Transceiver.companion.callback(channel: channel, reply: data, sw1: sw1, sw2: sw2, error: err)})
        }
    }
    
    class Iso7816Wrapper : ISO7816TransceiverSwiftWrapper {
        var tag: NFCISO7816Tag
        
        init(tag: NFCISO7816Tag) {
            self.tag = tag
        }
        
        func getIdentifier() -> Data {
            return tag.identifier
        }
        
        func transmit(input: Data, channel__ channel: Kotlinx_coroutines_coreSendChannel) {
            tag.sendCommand(apdu: NFCISO7816APDU(data: input)!, completionHandler: {data,sw1,sw2,err in
                ISO7816Transceiver.companion.callback(channel: channel, reply: data, sw1: sw1, sw2: sw2, error: err)})
        }
    }
    
    class FelicaWrapper : FelicaTransceiverIOSSwiftWrapper {
        func transmit(input: Data, channel: Kotlinx_coroutines_coreSendChannel) {
            print("Sending \(input)")
            tag.sendFeliCaCommand(commandPacket: input, completionHandler: {reply, err in FelicaTransceiverIOS.companion.callback(channel: channel, reply: reply, error: err)})
        }
        
        var tag: NFCFeliCaTag
        
        init(tag: NFCFeliCaTag) {
            self.tag = tag
        }
        
        func getIdentifier() -> Data {
            return tag.currentIDm
        }
    }

    class UltralightWrapper : UltralightTransceiverIOSSwiftWrapper {
        func transmit(input: Data, channel_ channel: Kotlinx_coroutines_coreSendChannel) {
            print("Sending \(input)")
            tag.sendMiFareCommand(commandPacket: input, completionHandler: {reply, err in
                print ("Reply \(reply), err \(String(describing: err))")
                UltralightTransceiverIOS.companion.callback(channel: channel, reply: reply, error: err)
            })
        }
        
        var tag: NFCMiFareTag
        
        init(tag: NFCMiFareTag) {
            self.tag = tag
        }
        
        func getIdentifier() -> Data {
            return tag.identifier
        }
    }

    func postDump(card: Card) {
        do {
            let json = try CardSerializer.init().toPersist(card: card)
            print ("json=\(json)")
            let url = try CardPersister.persistCard(card: card, json: json)
            if Preferences.init().speakBalance {
                if let balance = card.safeBalance?.formatCurrencyString(isBalance: true).unformatted {
                    let balanceStr = Utils.localizeString(RKt.R.string.balance_speech, balance)
                    Utils.speakText(voiceOutdata: balanceStr)
                }
            }
            DispatchQueue.main.async {
                let cr = CardViewController.create(json: json, url: url)
                self.navigationController?.pushViewController(cr, animated: true)
            }
        } catch {
            Utils.showError(viewController: self.navigationController!, msg: Utils.localizeString(RKt.R.string.ios_nfcreader_exception, "\(error)"))
        }
    }

    func updateStatusText(msg: String) {
        self.msg = msg
        refresh()
    }
    
    func updateProgressBar(progress: Int32, max: Int32) {
        self.cur = Int(progress)
        self.max = Int(max)
        refresh()
    }
    
    func showCardType(cardInfo: CardInfo?) {
    }
    
    private var msg: String
    private var cur: Int
    private var max: Int
    
    override init() {
        self.msg = Utils.localizeString(RKt.R.string.ios_nfcreader_tap)
        self.cur = 0
        self.max = 1
    }
    
    func refresh() {
        session?.alertMessage = "\(msg) \(cur * 100 / max) %"
    }
    
    func statusConnecting(cardType: CardType) {
        updateStatusText(msg: Utils.localizeString(RKt.R.string.ios_nfcreader_connecting, cardType.description()))
    }

    func statusReading(cardType: CardType) {
        updateStatusText(msg: Utils.localizeString(RKt.R.string.ios_nfcreader_reading, cardType.description()))
    }
    
    class func connectionError(session: NFCTagReaderSession, err: Error?) {
            session.invalidate(errorMessage:
                Utils.localizeString(RKt.R.string.ios_nfcreader_connection_error, " \(String(describing: err))"))
    }
    
    func vicinityRead(tag: NFCISO15693Tag) -> Card {
        let dg = DispatchGroup()
        var sectors: [Data] = []
        var partialRead: Bool = false
        for sectorIdx in 0...255 {
            dg.enter()
            var reachedEnd: Bool = false
            tag.readSingleBlock(requestFlags: RequestFlag(rawValue: 0x22), blockNumber: UInt8(sectorIdx), completionHandler: {
                data, errorIn in
                print ("Read \(sectorIdx) -> \(data), \(String(describing: errorIn))")
                if (errorIn != nil && (errorIn! as NSError).code == 102) {
                    reachedEnd = true
                } else if (errorIn != nil && (errorIn! as NSError).code == 100) {
                    partialRead = true
                } else {
                    sectors.append(data)
                }
                dg.leave()
            })
            dg.wait()
            if (reachedEnd || partialRead) {
                break
            }
        }
        let nfcv = NFCVCard(sysInfo: nil,
                            pages: sectors.map { NFCVPage(dataRaw: UtilsKt.toImmutable($0 as Data),
                                                          isUnauthorized: $0.isEmpty) }, isPartialRead: partialRead)
        return Card(tagId: UtilsKt.toImmutable(tag.identifier).reverseBuffer(), scannedAt: TimestampFull.Companion.init().now(),
                    label: nil, mifareClassic: nil, mifareDesfire: nil, mifareUltralight: nil, cepasCompat: nil, felica: nil, iso7816: nil, vicinity: nfcv)
    }

    func tagReaderSession(_ session: NFCTagReaderSession, didDetect tags: [NFCTag]) {
        if (tags.count < 1) {
            print ("No tags found")
            return
        }
        let tag = tags.first!
        print ("Found tag \(tag)")
        switch(tag) {
        case .miFare(let mifare):
            switch (mifare.mifareFamily) {
            case NFCMiFareFamily.desfire:
                statusConnecting(cardType: .mifaredesfire)
                session.connect(to: tag, completionHandler: {
                    err in
                    if (err != nil) {
                        NFCReader.connectionError(session: session, err: err)
                        return
                    }
                    
                    self.statusReading(cardType: .mifaredesfire)
                    DispatchQueue.global().async {
                        print("swift async")
                        do {
                            let card = try DesfireCardReaderIOS.init().dump(wrapper: DesfireWrapper(tag: mifare), feedback: self)
                            if (!card.isPartialRead) {
                                self.updateProgressBar(progress: 1, max: 1)
                            }
                            session.invalidate()
                            self.postDump(card: card)
                        } catch {
                            session.invalidate(errorMessage: error.localizedDescription)
                        }
                    }
                })
                break
            case NFCMiFareFamily.ultralight:
                statusConnecting(cardType: .mifareultralight)
                session.connect(to: tag, completionHandler: {
                    err in
                    if (err != nil) {
                        NFCReader.connectionError(session: session, err: err)
                        return
                    }
                    
                    self.statusReading(cardType: .mifareultralight)
                    DispatchQueue.global().async {
                        print("swift async")
                        do {
                            let card = try UltralightCardReaderIOS.init().dump(wrapper: UltralightWrapper(tag: mifare), feedback: self)
                            if (!card.isPartialRead) {
                                self.updateProgressBar(progress: 1, max: 1)
                            }
                            session.invalidate()
                            self.postDump(card: card)
                        } catch {
                            session.invalidate(errorMessage: error.localizedDescription)
                        }
                    }
                })
                break
            case .plus:
                statusConnecting(cardType: .mifareplus)
                session.connect(to: tag, completionHandler: {
                    err in
                    if (err != nil) {
                        NFCReader.connectionError(session: session, err: err)
                        return
                    }
                    
                    self.statusReading(cardType: .mifaredesfire)
                    DispatchQueue.global().async {
                        print("swift async")
                        do {
                            let card = try PlusCardReaderIOS.init().dump(wrapper: UltralightWrapper(tag: mifare), feedback: self)
                            if (!card.isPartialRead) {
                                self.updateProgressBar(progress: 1, max: 1)
                            }
                            session.invalidate()
                            self.postDump(card: card)
                        } catch {
                            session.invalidate(errorMessage: error.localizedDescription)
                        }
                    }
                })
                break
            default:
                session.invalidate(errorMessage: Utils.localizeString(RKt.R.string.ios_unknown_mifare, "\(mifare)"))
                break
            }
            break
        case .iso7816(let iso):
            statusConnecting(cardType: .iso7816)
            session.connect(to: tag, completionHandler: {
                err in
                if (err != nil) {
                    NFCReader.connectionError(session: session, err: err)
                    return
                }
                
                self.statusReading(cardType: .iso7816)
                DispatchQueue.global().async {
                    print("swift async")
                    do {
                        let card = try ISO7816CardReaderIOS.init().dump(wrapper: Iso7816Wrapper(tag: iso), feedback: self)
                        if (!card.isPartialRead) {
                            self.updateProgressBar(progress: 1, max: 1)
                        }
                        session.invalidate()
                        self.postDump(card: card)
                    } catch {
                        session.invalidate(errorMessage: error.localizedDescription)
                    }
                }
            })
            
        case .feliCa(let felica):
            statusConnecting(cardType: .felica)
            session.connect(to: tag, completionHandler: {
                err in
                if (err != nil) {
                    NFCReader.connectionError(session: session, err: err)
                    return
                }
                
                print("idm:\(felica.currentIDm.base64EncodedString()) syscode:\(felica.currentSystemCode.base64EncodedString())")
                self.statusReading(cardType: .felica)
                DispatchQueue.global().async {
                    print("swift async")
                    do {
                        let card = try FelicaCardReaderIOS.init().dump(wrapper: FelicaWrapper(tag: felica), defaultSysCode: felica.currentSystemCode, feedback: self)
                        if (!card.isPartialRead) {
                            self.updateProgressBar(progress: 1, max: 1)
                        }
                        session.invalidate()
                        self.postDump(card: card)
                    } catch {
                        session.invalidate(errorMessage: error.localizedDescription)
                    }
                }
            })
            
        case .iso15693(let vicinity):
            statusConnecting(cardType: .vicinity)
            session.connect(to: tag, completionHandler: {
                err in
                if (err != nil) {
                    NFCReader.connectionError(session: session, err: err)
                    return
                }
                
                self.statusReading(cardType: .vicinity)
                DispatchQueue.global().async {
                    print("swift async")
                    let card = self.vicinityRead(tag: vicinity)
                    if (!card.isPartialRead) {
                        self.updateProgressBar(progress: 1, max: 1)
                    }
                    session.invalidate()
                    self.postDump(card: card)
                }
            })

        default:
            session.invalidate(errorMessage:
                Utils.localizeString(RKt.R.string.ios_unknown_tag, "\(tag)"))
        }
    }
    
    func start(navigationController: UINavigationController?) {
        print ("Reading available: \(NFCTagReaderSession.readingAvailable)")
        session = NFCTagReaderSession(pollingOption: [
            .iso14443, .iso18092, .iso15693],
                                         delegate: self)
        session!.alertMessage = Utils.localizeString(RKt.R.string.ios_nfcreader_tap)
        session!.begin()
        self.navigationController = navigationController
    }
}
