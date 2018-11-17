package au.id.micolous.metrodroid.lintchecks

import com.android.tools.lint.detector.api.*
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression

class CardInfoValidator : Detector(), Detector.UastScanner {
    private var mHasName = false
    private var mHasLocation = false
    private var mHasImage = false
    private var mHasCardType = false

    private fun reset() {
        mHasCardType = false
        mHasImage = false
        mHasLocation = false
        mHasName = false
    }

    override fun applicableSuperClasses() = listOf(BUILDER_CLS)

    override fun getApplicableMethodNames() = METHODS

    override fun visitMethod(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (!context.evaluator.extendsClass(method.containingClass,
                        "au.id.micolous.metrodroid.transit.CardInfo.Builder", false)) {
            return
        }

        when (method.name) {
            SET_CARD_TYPE -> mHasCardType = true
            SET_IMAGE -> mHasImage = true
            SET_LOCATION -> mHasLocation = true
            SET_NAME -> mHasName = true
            BUILD -> {
                // We're "done", lets check for issues
                if (!mHasCardType) {
                    context.report(MISSING_CARD_TYPE, node, context.getLocation(node),
                            "Card type required")
                }

                if (!mHasImage) {
                    context.report(MISSING_CARD_IMAGE, node, context.getLocation(node),
                            "Card image missing, a placeholder will appear instead.")
                }

                if (!mHasLocation) {
                    context.report(MISSING_CARD_LOCATION, node, context.getLocation(node),
                            "Location required")
                }

                if (!mHasName) {
                    context.report(MISSING_CARD_NAME, node, context.getLocation(node),
                            "Name required")
                }

                reset()
            }
        }
    }

    companion object {
        internal const val BUILD = "build"
        internal const val SET_NAME = "setName"
        internal const val SET_LOCATION = "setLocation"
        internal const val SET_IMAGE = "setImageId"
        internal const val SET_CARD_TYPE = "setCardType"
        internal const val BUILDER_CLS = "au.id.micolous.metrodroid.transit.CardInfo.Builder"

        internal val METHODS = listOf(BUILD, SET_CARD_TYPE, SET_IMAGE, SET_LOCATION, SET_NAME)

        private val IMPLEMENTATION = Implementation(
                CardInfoValidator::class.java, Scope.JAVA_FILE_SCOPE)

        internal val MISSING_CARD_TYPE = Issue.create(
                "MissingCardType",
                "CardInfo usages which have no card type",
                "Verifies that all CardInfos have a card type associated with them " +
                        "(setCardType). This is used in the 'supported cards' list.\n\n" +
                        "This is required for all cards.",
                Category.CORRECTNESS,
                8,
                Severity.ERROR,
                IMPLEMENTATION
        )

        internal val MISSING_CARD_IMAGE = Issue.create(
                "MissingCardImage",
                "CardInfo usages which have no card image",
                "Verifies that all CardInfos have an image associated with them " +
                        "(setImageId). This is used in the 'supported cards' list, and shown when " +
                        "a card is scanned.\n\n" +
                        "If there is no card image supplied, then the Metrodroid logo will be " +
                        "displayed instead.",
                Category.USABILITY,
                4,
                Severity.WARNING,
                IMPLEMENTATION
        )

        internal val MISSING_CARD_NAME = Issue.create(
                "MissingCardName",
                "CardInfo usages which have no card name",
                "Verifies that all CardInfos have a name associated with them " +
                        "(setName). This is used in the 'supported cards' list, and shown when " +
                        "a card is scanned.\n\n" +
                        "This is required for all cards.",
                Category.CORRECTNESS,
                8,
                Severity.ERROR,
                IMPLEMENTATION
        )

        internal val MISSING_CARD_LOCATION = Issue.create(
                "MissingCardLocation",
                "CardInfo usages which have no card location",
                "Verifies that all CardInfos have a location associated with them " +
                        "(setLocation). This is used in the 'supported cards' list.\n\n" +
                        "This is required for all cards.",
                Category.CORRECTNESS,
                8,
                Severity.ERROR,
                IMPLEMENTATION
        )
    }
}
