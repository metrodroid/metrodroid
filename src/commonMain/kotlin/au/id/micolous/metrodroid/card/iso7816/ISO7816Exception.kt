package au.id.micolous.metrodroid.card.iso7816

open class ISO7816Exception : Exception {
    internal constructor(s: String) : super(s)
}

class ISOEOFException : ISO7816Exception("End of file")
class ISOFileNotFoundException : ISO7816Exception("File not found")
class ISONoCurrentEF : ISO7816Exception("No current EF")
