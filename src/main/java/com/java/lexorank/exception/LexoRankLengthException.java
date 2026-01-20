package com.java.lexorank.exception;

/**
 * Thrown when a generated LexoRank would exceed the maximum allowed length.
 * Ranks can grow when repeatedly inserting at the same position; rebalancing
 * reassigns shorter ranks to all items.
 */
public class LexoRankLengthException extends RuntimeException {
    public LexoRankLengthException(String message) {
        super(message);
    }
}
