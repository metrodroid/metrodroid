package au.id.micolous.metrodroid.util

import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.support.v7.content.res.AppCompatResources
import au.id.micolous.metrodroid.multi.DrawableResource
import au.id.micolous.metrodroid.multi.Log
import au.id.micolous.metrodroid.transit.CardInfo

object DrawableUtils {

    /**
     * Creates a drawable with alpha channel from two component resources.
     *
     * This is used to add alpha channels to JPEG images.
     *
     * Adapted from http://www.piwai.info/transparent-jpegs-done-right, with pre-Honeycomb support
     * removed, and resource annotations added.
     *
     * @param res [Resources] from the current context.
     * @param sourceRes Source image to get R/G/B channels from.
     * @param maskRes Source image to get Alpha channel from. This is a greyscale + alpha image,
     * with a black mask and transparent pixels where they should be added. This must be the same
     * dimensions as `sourceRes`.
     * @return Composited image with RGBA channels combined.
     * @throws IllegalArgumentException If image sizes differ.
     */
    @JvmStatic
    fun getMaskedBitmap(context: Context, sourceRes: DrawableResource, maskRes: DrawableResource): Drawable? {
        Log.d(TAG, "Masked bitmap ${sourceRes.hexString} / ${maskRes.hexString}")
        val res = context.resources

        // We want a mutable, ARGB8888 bitmap to work with.
        val options = BitmapFactory.Options()
        options.inMutable = true
        options.inPreferredConfig = Bitmap.Config.ARGB_8888

        // Load the source image.
        val bitmap = BitmapFactory.decodeResource(res, sourceRes, options)
        bitmap.setHasAlpha(true)

        // Put it into a canvas (mutable).
        val canvas = Canvas(bitmap)

        // Load the mask.
        val mask = BitmapFactory.decodeResource(res, maskRes)

        try {
            if (mask.width != canvas.width || mask.height != canvas.height) {
                Log.w(TAG, "Source image (${canvas.width} x ${canvas.height}) and mask (${mask.width} x ${mask.height}) are not the same size -- returning image without alpha")
                return AppCompatResources.getDrawable(context, sourceRes)
            }

            // Paint the mask onto the canvas, revealing transparency.
            val paint = Paint()
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
            canvas.drawBitmap(mask, 0f, 0f, paint)

        } finally {
            // Ditch the mask.
            mask.recycle()
        }

        // Return the completed bitmap.
        return BitmapDrawable(res, bitmap)
    }

    @JvmStatic
    fun getCardInfoDrawable(context: Context, ci: CardInfo): Drawable? {
        val imageAlphaId = ci.imageAlphaId
        val imageId = ci.imageId
        return when {
            imageId == null -> null
            imageAlphaId != null -> getMaskedBitmap(context, imageId, imageAlphaId)
            else -> AppCompatResources.getDrawable(context, imageId)
        }
    }

    private const val TAG = "DrawableUtils"
}
