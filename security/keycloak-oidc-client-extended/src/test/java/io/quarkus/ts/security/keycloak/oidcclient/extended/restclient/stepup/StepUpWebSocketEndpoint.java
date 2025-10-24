package io.quarkus.ts.security.keycloak.oidcclient.extended.restclient.stepup;

import io.quarkus.oidc.AuthenticationContext;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;

/**
 * WebSocket endpoint protected with Step-Up Authentication.
 * The @AuthenticationContext annotation must be placed on the class level for WebSocket endpoints
 * because SecurityIdentity is created before the HTTP connection is upgraded to WebSocket.
 */
@AuthenticationContext("silver")
@WebSocket(path = "/ws/step-up/silver")
public class StepUpWebSocketEndpoint {

    @OnOpen
    public String onOpen() {
        return "WebSocket opened with ACR silver";
    }

    @OnTextMessage
    public String onMessage(String message) {
        return "Echo with silver ACR: " + message;
    }
}
