// ============================================================================
// FrameDecoder.kt
// ----------------------------------------------------------------------------
// Turns inbound jpeg bytes (from LiveFrame) into something Compose can draw.
// The interface/implementation split exists purely for testability: the real
// AndroidFrameDecoder needs android.graphics.BitmapFactory (only available on a
// device/emulator), so the operator ViewModel depends on the FrameDecoder
// interface and tests inject a fake that returns canned bitmaps on the JVM.
// ============================================================================
package com.bhoomibot.os.connection.transport

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/**
 * Decodes an inbound jpeg frame into a Compose [ImageBitmap].
 *
 * Injected (rather than called directly) so the operator ViewModel can be
 * unit-tested with a fake decoder — [BitmapFactory] needs the Android runtime.
 */
interface FrameDecoder {
    fun decode(jpeg: ByteArray): ImageBitmap?
}

class AndroidFrameDecoder : FrameDecoder {
    override fun decode(jpeg: ByteArray): ImageBitmap? =
        BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)?.asImageBitmap()
}
