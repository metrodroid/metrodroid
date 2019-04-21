package au.id.micolous.metrodroid.card

abstract class Protocol(protected val tag: CardTransceiver,
                        private val protocol: CardTransceiver.Protocol) {

    @Throws(CardTransceiver.UnsupportedProtocolException::class)
    open fun connect() {
        tag.connect(protocol)
    }

    open fun disconnect() {
        tag.disconnect()
    }
}