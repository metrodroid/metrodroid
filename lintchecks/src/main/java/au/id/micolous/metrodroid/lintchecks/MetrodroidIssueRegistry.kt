package au.id.micolous.metrodroid.lintchecks

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.detector.api.Issue

@Suppress("unused")
class MetrodroidIssueRegistry : IssueRegistry() {
    override val issues: List<Issue>
        get() = listOf(
                CardInfoValidator.MISSING_CARD_IMAGE,
                CardInfoValidator.MISSING_CARD_LOCATION,
                CardInfoValidator.MISSING_CARD_NAME,
                CardInfoValidator.MISSING_CARD_TYPE)

    override val api: Int = com.android.tools.lint.detector.api.CURRENT_API
}
