package com.java.lexorank.domain;

import org.springframework.stereotype.Component;

/**
 * LexoRank Generator - Simplified educational implementation
 * Based on Atlassian's LexoRank algorithm
 * 
 * Format: "bucket|value" where:
 * - bucket: 0, 1, or 2 (for balancing)
 * - value: base-36 alphanumeric string (0-9, a-z)
 * 
 * Note: This is a simplified version for educational purposes.
 * Production systems should implement full bucket balancing and rebalancing logic.
 */
@Component
public class LexoRankGenerator {
    private static final String BUCKET_0 = "0";
    private static final String BUCKET_1 = "1";
    private static final String BUCKET_2 = "2";
    private static final String DEFAULT_BUCKET = BUCKET_0;
    
    // Base-36 characters: 0-9, a-z
    private static final String BASE_36_CHARS = "0123456789abcdefghijklmnopqrstuvwxyz";
    private static final int BASE = 36;
    
    // Special rank values (without bucket prefix)
    private static final String MIN_VALUE = "0";
    private static final String MAX_VALUE = "z";
    private static final String MID_VALUE = "h"; // Middle of base-36 range (~17/35)
    
    /**
     * Generate initial rank (typically for the first item)
     * Format: "0|h"
     * 
     * Starting with a minimal rank that grows only as needed.
     */
    public String initial() {
        return DEFAULT_BUCKET + "|" + MID_VALUE;
    }
    
    /**
     * Generate a rank between two existing ranks
     * 
     * @param left  The rank of the item to the left (can be null for "before all")
     * @param right The rank of the item to the right (can be null for "after all")
     * @return A new rank lexicographically between left and right
     */
    public String between(String left, String right) {
        if (left == null && right == null) {
            return initial();
        }
        if (left == null) {
            return beforeRank(right);
        }
        if (right == null) {
            return afterRank(left);
        }
        
        // Extract bucket and value from both ranks
        String[] leftParts = parseRank(left);
        String[] rightParts = parseRank(right);
        
        String leftBucket = leftParts[0];
        String leftValue = leftParts[1];
        String rightBucket = rightParts[0];
        String rightValue = rightParts[1];
        
        // Use the left bucket (or default if both are the same)
        String resultBucket = leftBucket.equals(rightBucket) ? leftBucket : DEFAULT_BUCKET;
        
        // Generate value between left and right values
        String resultValue = betweenValues(leftValue, rightValue);
        
        return resultBucket + "|" + resultValue;
    }
    
    /**
     * Parse a rank string into [bucket, value]
     */
    private String[] parseRank(String rank) {
        if (rank == null || !rank.contains("|")) {
            // Legacy format without bucket - assume default bucket
            return new String[]{DEFAULT_BUCKET, rank != null ? rank : MID_VALUE};
        }
        String[] parts = rank.split("\\|", 2);
        return new String[]{parts[0], parts[1]};
    }
    
    /**
     * Generate a rank before the given rank
     */
    private String beforeRank(String rank) {
        String[] parts = parseRank(rank);
        String bucket = parts[0];
        String value = parts[1];
        
        String newValue = betweenValues(MIN_VALUE, value);
        return bucket + "|" + newValue;
    }
    
    /**
     * Generate a rank after the given rank
     */
    private String afterRank(String rank) {
        String[] parts = parseRank(rank);
        String bucket = parts[0];
        String value = parts[1];
        
        String newValue = betweenValues(value, MAX_VALUE);
        return bucket + "|" + newValue;
    }
    
    /**
     * Generate a value lexicographically between two base-36 strings
     */
    private String betweenValues(String left, String right) {
        // Validate that left < right lexicographically
        if (left.compareTo(right) >= 0) {
            throw new IllegalArgumentException(
                "Invalid rank order: left must be < right (left=" + left + ", right=" + right + ")");
        }
        
        StringBuilder result = new StringBuilder();
        int maxLen = Math.max(left.length(), right.length());
        
        for (int i = 0; i < maxLen; i++) {
            char leftChar = i < left.length() ? left.charAt(i) : '0';
            char rightChar = i < right.length() ? right.charAt(i) : 'z';
            
            if (leftChar == rightChar) {
                result.append(leftChar);
                continue;
            }
            
            // Characters differ - calculate midpoint in base-36
            int leftIndex = BASE_36_CHARS.indexOf(leftChar);
            int rightIndex = BASE_36_CHARS.indexOf(rightChar);
            
            if (leftIndex == -1) leftIndex = 0;
            if (rightIndex == -1) rightIndex = BASE - 1;
            
            int diff = rightIndex - leftIndex;
            
            if (diff > 1) {
                // There's space between them - use midpoint
                int midIndex = leftIndex + diff / 2;
                result.append(BASE_36_CHARS.charAt(midIndex));
                return result.toString();
            } else if (diff == 1) {
                // Adjacent characters - append left and add middle char
                result.append(leftChar);
                result.append(BASE_36_CHARS.charAt(BASE / 2));
                return result.toString();
            } else {
                // leftIndex > rightIndex at this position
                // This means we need to look at previous position
                // Append the left character and continue with remaining positions
                result.append(leftChar);
            }
        }
        
        // All characters processed - append middle character
        result.append(BASE_36_CHARS.charAt(BASE / 2));
        return result.toString();
    }
    
    /**
     * Validate if a rank string is in correct format
     */
    public boolean isValidRank(String rank) {
        if (rank == null || rank.isEmpty()) {
            return false;
        }
        
        if (!rank.contains("|")) {
            return false;
        }
        
        String[] parts = rank.split("\\|", 2);
        if (parts.length != 2) {
            return false;
        }
        
        String bucket = parts[0];
        String value = parts[1];
        
        // Validate bucket
        if (!bucket.equals(BUCKET_0) && !bucket.equals(BUCKET_1) && !bucket.equals(BUCKET_2)) {
            return false;
        }
        
        // Validate value contains only base-36 characters
        return value.matches("[0-9a-z]+");
    }
}
