package au.id.micolous.metrodroid.card

import kotlinx.io.core.Closeable

/**
 * Declares an abstract protocol implementation for communicating with [CardTransceiver].
 *
 * This is intended to reduce the amount of repeated code between implementations.
 *
 * @property tag A [CardTransceiver] to communicate with the card on.
 * @param protocol The physical-layer protocol that this card uses. This should generally be hard
 * coded in implementations.
 */
abstract class AbstractProtocol(
        protected val tag: CardTransceiver,
        private val protocol: CardTransceiver.Protocol) : Closeable {

    /**
     * Connects to the card.
     *
     * The default implementation calls [CardTransceiver.connect] with the [protocol] parameter.
     * This is normally good enough for most implementations.
     *
     * @throws CardProtocolUnsupportedException
     */
    open fun connect() {
        tag.connect(protocol)
    }

    /**
     * Disconnects from the card.
     *
     * This calls [CardTransceiver.close].  Most implementations should not need to override this.
     */
    override fun close() {
        tag.close()
    }
}