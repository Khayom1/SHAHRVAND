package tj.shahrvand.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InterestSelectionRulesTest {
    @Test
    fun rejectsEmptySelection() {
        assertFalse(isValidInterestSelection(emptySet()))
    }

    @Test
    fun acceptsMoreThanFiveInterests() {
        assertTrue(
            isValidInterestSelection(
                setOf("science", "technology", "history", "geography", "mathematics", "languages")
            )
        )
    }

    @Test
    fun acceptsNewTopics() {
        assertTrue(isValidInterestSelection(setOf("dentistry", "ai", "robotics", "photography")))
    }

    @Test
    fun rejectsUnsupportedTopics() {
        assertFalse(isValidInterestSelection(setOf("science", "not-a-real-topic")))
    }
}
