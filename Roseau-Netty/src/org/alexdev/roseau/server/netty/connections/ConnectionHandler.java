package org.alexdev.roseau.server.netty.connections;

import org.alexdev.roseau.Roseau;
import org.alexdev.roseau.game.player.Player;
import org.alexdev.roseau.log.Log;
import org.alexdev.roseau.messages.outgoing.handshake.HELLO;
import org.alexdev.roseau.server.IServerHandler;
import org.alexdev.roseau.server.netty.readers.NettyRequest;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

public class ConnectionHandler extends SimpleChannelHandler {
	
	private IServerHandler serverHandler;
	
	public ConnectionHandler(IServerHandler serverHandler) {
		this.serverHandler = serverHandler;
	}
	
	@Override
	public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) {

		ctx.getChannel().write(new HELLO());
		
		this.serverHandler.getSessionManager().addSession(ctx.getChannel());
		
		Player player = (Player) ctx.getChannel().getAttachment();
		
		if (Roseau.getUtilities().getConfiguration().get("Logging", "log.connections", Boolean.class)) {
			Log.println("[" + player.getNetwork().getConnectionId() + "] Connection from " + ctx.getChannel().getRemoteAddress().toString().replace("/", "").split(":")[0]);
		}

	} 

	@Override
	public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) {
		
		this.serverHandler.getSessionManager().removeSession(ctx.getChannel());
		
		Player player = (Player) ctx.getChannel().getAttachment();
		
		if (Roseau.getUtilities().getConfiguration().get("Logging", "log.connections", Boolean.class)) {
			Log.println("[" + player.getNetwork().getConnectionId() + "] Disconnection from " + ctx.getChannel().getRemoteAddress().toString().replace("/", "").split(":")[0]);
		}
		
		player.dispose();
		
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {

		try {

			Player player = (Player) ctx.getChannel().getAttachment();
			NettyRequest request = (NettyRequest) e.getMessage();
			
			if (request == null) {
				return;
			}

			if (Roseau.getUtilities().getConfiguration().get("Logging", "log.packets", Boolean.class)) {
				Log.println("[" + player.getNetwork().getConnectionId() + "] Received: " + request.getHeader() + " / " + request.getMessageBody());
			}

			if (player != null){
				Roseau.getServer().getMessageHandler().handleRequest(player, request);
			}

		} catch (Exception ex) {
			Log.exception(ex);
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
		ctx.getChannel().close();
	}

}