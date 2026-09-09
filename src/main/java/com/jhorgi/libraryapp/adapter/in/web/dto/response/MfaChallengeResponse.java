package com.jhorgi.libraryapp.adapter.in.web.dto.response;

import com.jhorgi.libraryapp.domain.model.MfaChallenge;

public record MfaChallengeResponse(boolean mfaRequired, String challengeId, long expiresInSeconds) {

    public static MfaChallengeResponse from(MfaChallenge challenge) {
        return new MfaChallengeResponse(true, challenge.challengeId(), challenge.expiresInSeconds());
    }
}
