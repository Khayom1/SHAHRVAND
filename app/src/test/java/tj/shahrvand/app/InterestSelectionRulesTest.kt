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
    fun acceptsExactlyFiveSupportedInterests() {
        assertTrue(isValidInterestSelection(setOf("science", "technology", "history", "geography", "mathematics")))
    }

    @Test
    fun rejectsMoreThanFiveInterests() {
        assertFalse(isValidInterestSelection(setOf("science", "technology", "history", "geography", "mathematics", "languages")))
    }

    @Test
    fun rejectsFewerThanFiveInterests() {
        assertFalse(isValidInterestSelection(setOf("science", "technology", "history", "geography")))
    }

    @Test
    fun acceptsNewTopicsWithinFiveSelectedItems() {
        assertTrue(isValidInterestSelection(setOf("dentistry", "ai", "robotics", "photography", "cybersecurity")))
    }

    @Test
    fun rejectsUnsupportedTopics() {
        assertFalse(isValidInterestSelection(setOf("science", "not-a-real-topic", "history", "geography", "mathematics")))
    }
}
