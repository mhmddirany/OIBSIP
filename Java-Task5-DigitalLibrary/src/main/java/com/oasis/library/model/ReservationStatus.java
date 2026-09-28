package com.oasis.library.model;

public enum ReservationStatus {
    /** In the queue, waiting for a copy to be returned. */
    WAITING,
    /** A copy was returned and automatically issued to this member. */
    FULFILLED,
    /** Cancelled by the member (or skipped because it could no longer be honoured). */
    CANCELLED
}
