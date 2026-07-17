package com.bhoomibot.os.connection.model

/** A single (still-encoded) video frame received from the robot: jpeg bytes + arrival time. */
data class LiveFrame(val jpeg: ByteArray, val receivedAt: Long = 0L) {
    // equals/hashCode are overridden by hand because this data class holds a
    // ByteArray. A default data-class equals would compare ByteArray by reference
    // (identity), so two frames with identical bytes would look "different". We use
    // contentEquals / contentHashCode so value comparison behaves as expected —
    // important for Compose recomposition and for assertions in tests.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LiveFrame) return false
        return receivedAt == other.receivedAt && jpeg.contentEquals(other.jpeg)
    }

    override fun hashCode(): Int = 31 * jpeg.contentHashCode() + receivedAt.hashCode()
}
