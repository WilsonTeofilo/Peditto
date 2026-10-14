package com.peditto.identity.domain;

public enum AuthChallengePurpose {
    LOGIN,
    PASSWORD_RESET,
    CONTACT_CHANGE,
    MFA_RECOVERY,
    VERIFY_EMAIL,
    VERIFY_PHONE
}
