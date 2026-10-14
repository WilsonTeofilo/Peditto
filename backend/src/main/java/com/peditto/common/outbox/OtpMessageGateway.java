package com.peditto.common.outbox;

import com.peditto.identity.domain.OtpChannel;

public interface OtpMessageGateway {
    void send(OtpChannel channel, String destination, String code);
}
