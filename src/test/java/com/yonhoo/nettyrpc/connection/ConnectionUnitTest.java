package com.yonhoo.nettyrpc.connection;


import io.netty.channel.Channel;
import io.netty.channel.DefaultChannelPromise;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

class ConnectionUnitTest {

    @Test
    void should_close_channel_right_when_invoke_connection_close_given_mock_channel_and_referenceCount_is_0() {
        //given
        Channel channel = mock(Channel.class);
        Attribute<Connection> objectAttribute = mock(Attribute.class);
        given(channel.attr(any(AttributeKey.class))).willReturn(objectAttribute);
        Connection connection = new Connection(channel);

        given(channel.isActive()).willReturn(true);
        given(channel.isWritable()).willReturn(true);

        DefaultChannelPromise defaultChannelPromise = new DefaultChannelPromise(channel);
        given(channel.close()).willReturn(defaultChannelPromise);
        given(channel.closeFuture()).willReturn(defaultChannelPromise);

        //when
        connection.closeChannel();

        //then
        then(channel).should(times(1)).close();
    }

    @Test
    void should_not_close_when_invoke_connection_close_given_mock_channel_and_referenceCount_greater_than_0() {
        //given
        Channel channel = mock(Channel.class);
        Attribute<Connection> objectAttribute = mock(Attribute.class);
        given(channel.attr(any(AttributeKey.class))).willReturn(objectAttribute);
        Connection connection = new Connection(channel);

        given(channel.isActive()).willReturn(true);
        given(channel.isWritable()).willReturn(true);

         connection.addInvokeFuture(1, new CompletableFuture<>());


        //when
        connection.closeChannel();

        //then
        then(channel).should(times(0)).close();
    }
}