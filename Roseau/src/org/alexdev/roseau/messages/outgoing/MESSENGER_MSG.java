package org.alexdev.roseau.messages.outgoing;

import org.alexdev.roseau.game.player.Player;
import org.alexdev.roseau.messages.OutgoingMessageComposer;
import org.alexdev.roseau.server.messages.Response;

public class MESSENGER_MSG implements OutgoingMessageComposer {

	private Player from;
	
	public MESSENGER_MSG(Player from) {
		this.from = from;
	}

	@Override
	public void write(Response response) {
		response.init("MESSENGER_MSG");
		response.appendNewArgument(String.valueOf("655641"));
		response.appendTabArgument(String.valueOf("2"));
        response.appendTabArgument("");
        response.appendTabArgument("04/03/1980 10:10:10");
        response.appendTabArgument("hiii");
        response.appendTabArgument(from.getDetails().getFigure());
	}

}