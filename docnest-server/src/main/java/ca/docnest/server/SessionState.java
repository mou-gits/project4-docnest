package ca.docnest.server;

public enum SessionState {
    CONNECTED,
    AUTHENTICATING,
    READY,
    TRANSFERRING,
    CLOSING
}
