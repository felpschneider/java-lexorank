package com.java.lexorank.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for LexoRankGeneratorUtils
 * 
 * Validates the implementation follows LexoRank principles:
 * - Ranks are in "bucket|value" format
 * - Values are base-36 alphanumeric strings
 * - Generated ranks maintain lexicographic ordering
 * - Works correctly with edge cases (null left/right, adjacent values)
 */
class LexoRankGeneratorTest {

    @Test
    void shouldGenerateInitialRank() {
        String rank = LexoRankGeneratorUtils.initial();
        
        assertNotNull(rank);
        assertTrue(rank.contains("|"), "Rank should contain bucket separator");
        assertEquals("0|h", rank, "Initial rank should be in bucket 0 with middle value");
    }

    @Test
    void shouldGenerateBetweenNullAndNull() {
        String rank = LexoRankGeneratorUtils.between(null, null);
        
        assertEquals("0|h", rank);
    }

    @Test
    void shouldGenerateBeforeRank() {
        String rank = LexoRankGeneratorUtils.between(null, "0|h");
        
        assertTrue(rank.compareTo("0|h") < 0, 
            "Generated rank should be before 0|h");
        assertTrue(LexoRankGeneratorUtils.isValidRank(rank), "Generated rank should be valid");
    }

    @Test
    void shouldGenerateAfterRank() {
        String rank = LexoRankGeneratorUtils.between("0|h", null);
        
        assertTrue(rank.compareTo("0|h") > 0,
            "Generated rank should be after 0|h");
        assertTrue(LexoRankGeneratorUtils.isValidRank(rank), "Generated rank should be valid");
    }

    @Test
    void shouldGenerateBetweenTwoRanks() {
        String left = "0|a00000";
        String right = "0|z00000";
        String rank = LexoRankGeneratorUtils.between(left, right);
        
        assertTrue(rank.compareTo(left) > 0, "Generated rank should be after left");
        assertTrue(rank.compareTo(right) < 0, "Generated rank should be before right");
        assertTrue(LexoRankGeneratorUtils.isValidRank(rank), "Generated rank should be valid");
    }

    @Test
    void shouldGenerateBetweenCloseRanks() {
        // Test when ranks are very close (adjacent characters)
        String left = "0|m00000";
        String right = "0|n00000";
        String rank = LexoRankGeneratorUtils.between(left, right);
        
        assertTrue(rank.compareTo(left) > 0, "Generated rank should be after left");
        assertTrue(rank.compareTo(right) < 0, "Generated rank should be before right");
        assertTrue(LexoRankGeneratorUtils.isValidRank(rank), "Generated rank should be valid");
    }

    @Test
    void shouldGenerateMultipleBetweenRanks() {
        // Simulate multiple insertions between same two ranks
        String left = "0|a00000";
        String right = "0|z00000";
        
        String rank1 = LexoRankGeneratorUtils.between(left, right);
        assertTrue(rank1.compareTo(left) > 0 && rank1.compareTo(right) < 0,
            "rank1 should be between left and right");
        
        String rank2 = LexoRankGeneratorUtils.between(left, rank1);
        assertTrue(rank2.compareTo(left) > 0 && rank2.compareTo(rank1) < 0,
            "rank2 should be between left and rank1");
        
        String rank3 = LexoRankGeneratorUtils.between(rank1, right);
        assertTrue(rank3.compareTo(rank1) > 0 && rank3.compareTo(right) < 0,
            "rank3 should be between rank1 and right");
        
        // Verify ordering
        assertTrue(left.compareTo(rank2) < 0);
        assertTrue(rank2.compareTo(rank1) < 0);
        assertTrue(rank1.compareTo(rank3) < 0);
        assertTrue(rank3.compareTo(right) < 0);
    }

    @Test
    void shouldThrowWhenLeftGreaterThanRight() {
        assertThrows(IllegalArgumentException.class, 
            () -> LexoRankGeneratorUtils.between("0|z00000", "0|a00000"),
            "Should throw when left rank is greater than right rank");
    }

    @Test
    void shouldValidateCorrectRankFormat() {
        assertTrue(LexoRankGeneratorUtils.isValidRank("0|h"));
        assertTrue(LexoRankGeneratorUtils.isValidRank("1|abc"));
        assertTrue(LexoRankGeneratorUtils.isValidRank("2|z"));
    }

    @Test
    void shouldRejectInvalidRankFormat() {
        assertFalse(LexoRankGeneratorUtils.isValidRank(null), "Null should be invalid");
        assertFalse(LexoRankGeneratorUtils.isValidRank(""), "Empty string should be invalid");
        assertFalse(LexoRankGeneratorUtils.isValidRank("h"), "Missing bucket should be invalid");
        assertFalse(LexoRankGeneratorUtils.isValidRank("0"), "Missing value should be invalid");
        assertFalse(LexoRankGeneratorUtils.isValidRank("3|h"), "Invalid bucket should be invalid");
        assertFalse(LexoRankGeneratorUtils.isValidRank("0|ABC"), "Uppercase letters should be invalid");
        assertFalse(LexoRankGeneratorUtils.isValidRank("0|h@z"), "Special characters should be invalid");
    }

    @Test
    void shouldWorkWithDifferentBuckets() {
        // When buckets are different, use the left bucket for result
        String left = "0|a00000";
        String right = "1|z00000";
        
        String rank1 = LexoRankGeneratorUtils.between(left, right);
        assertNotNull(rank1);
        assertTrue(LexoRankGeneratorUtils.isValidRank(rank1));
        // Result should use default bucket (0)
        assertTrue(rank1.startsWith("0|"));
    }

    @Test
    void shouldHandleLegacyFormatWithoutBucket() {
        // Test backward compatibility with ranks without bucket
        String legacyRank = "h";
        
        // Should still work by assuming default bucket
        String newRank = LexoRankGeneratorUtils.between(legacyRank, null);
        assertNotNull(newRank);
        assertTrue(LexoRankGeneratorUtils.isValidRank(newRank));
    }
}
