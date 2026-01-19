package com.screenleads.backend.app.domain.model;

/**
 * Enumeration of possible interaction types with an Advice (promotion)
 */
public enum InteractionType {
    /**
     * User viewed detailed information about the promotion
     */
    VIEW_DETAILS,

    /**
     * User started the redemption process (clicked redeem button)
     */
    REDEEM_START,

    /**
     * User completed the redemption process
     */
    REDEEM_COMPLETE,

    /**
     * User shared the promotion (social media, message, etc)
     */
    SHARE,

    /**
     * User saved/bookmarked the promotion for later
     */
    SAVE,

    /**
     * User clicked on an external link (website, app store, etc)
     */
    EXTERNAL_LINK,

    /**
     * User played a video related to the promotion
     */
    VIDEO_PLAY,

    /**
     * User expanded/collapsed additional content
     */
    EXPAND_CONTENT,

    /**
     * User participated in a survey/poll
     */
    SURVEY_RESPONSE,

    /**
     * Other custom interaction type (see details field)
     */
    OTHER
}
