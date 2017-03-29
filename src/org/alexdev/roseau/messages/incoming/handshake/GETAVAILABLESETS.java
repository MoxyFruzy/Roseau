package org.alexdev.roseau.messages.incoming.handshake;

import org.alexdev.roseau.game.player.Player;
import org.alexdev.roseau.messages.incoming.MessageEvent;
import org.alexdev.roseau.messages.outgoing.handshake.AVAILABLESETS;
import org.alexdev.roseau.server.messages.ClientMessage;

public class GETAVAILABLESETS implements MessageEvent {

	@Override
	public void handle(Player player, ClientMessage reader) {
		player.send(new AVAILABLESETS());

	}

}