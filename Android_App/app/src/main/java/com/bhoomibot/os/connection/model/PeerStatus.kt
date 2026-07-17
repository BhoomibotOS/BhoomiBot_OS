package com.bhoomibot.os.connection.model

// Lets each side know whether its counterpart has joined: the operator UI shows
// "waiting for robot" until robotOnline flips true, and vice-versa. The relay
// server (not the peer) is the source of these flags.

/** Who is currently present in the session, per the relay's PEER_STATUS broadcasts. */
data class PeerStatus(
    val robotOnline: Boolean = false,
    val operatorOnline: Boolean = false
)
