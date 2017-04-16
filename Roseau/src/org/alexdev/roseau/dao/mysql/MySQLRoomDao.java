package org.alexdev.roseau.dao.mysql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.alexdev.roseau.Roseau;
import org.alexdev.roseau.dao.RoomDao;
import org.alexdev.roseau.dao.util.IProcessStorage;
import org.alexdev.roseau.game.player.Bot;
import org.alexdev.roseau.game.player.Player;
import org.alexdev.roseau.game.player.PlayerDetails;
import org.alexdev.roseau.game.room.Room;
import org.alexdev.roseau.game.room.RoomConnection;
import org.alexdev.roseau.game.room.RoomData;
import org.alexdev.roseau.game.room.model.Position;
import org.alexdev.roseau.game.room.model.RoomModel;
import org.alexdev.roseau.game.room.settings.RoomType;
import org.alexdev.roseau.log.Log;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class MySQLRoomDao extends IProcessStorage<Room, ResultSet> implements RoomDao {

	private MySQLDao dao;
	private Map<String, RoomModel> roomModels;

	public MySQLRoomDao(MySQLDao dao) {

		this.dao = dao;
		this.roomModels = Maps.newHashMap();

		Connection sqlConnection = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;

		try {

			sqlConnection = this.dao.getStorage().getConnection();
			preparedStatement = this.dao.getStorage().prepare("SELECT * FROM room_models", sqlConnection);
			resultSet = preparedStatement.executeQuery();

			while (resultSet.next()) {
				roomModels.put(resultSet.getString("id"), new RoomModel(resultSet.getString("id"), resultSet.getString("heightmap"), resultSet.getInt("door_x"), resultSet.getInt("door_y"), 
						resultSet.getInt("door_z"), resultSet.getInt("door_dir"), resultSet.getByte("has_pool") == 1, resultSet.getByte("disable_height_check") == 1));
			}

		} catch (Exception e) {
			Log.exception(e);
		} finally {
			Storage.closeSilently(resultSet);
			Storage.closeSilently(preparedStatement);
			Storage.closeSilently(sqlConnection);
		}
	}


	@Override
	public List<Room> getPlayerRooms(PlayerDetails details) {
		return getPlayerRooms(details, false);
	}

	@Override
	public List<Room> getPublicRooms(boolean storeInMemory) {

		List<Room> rooms = Lists.newArrayList();

		Connection sqlConnection = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;

		try {

			sqlConnection = this.dao.getStorage().getConnection();
			preparedStatement = this.dao.getStorage().prepare("SELECT * FROM rooms WHERE enabled = 1 AND room_type = " + RoomType.PUBLIC.getTypeCode() + " ORDER BY order_id ASC", sqlConnection);
			resultSet = preparedStatement.executeQuery();

			while (resultSet.next()) {

				int id = resultSet.getInt("id");

				Room room = Roseau.getGame().getRoomManager().getRoomByID(id);

				if (room == null) {
					room = this.fill(resultSet);
					room.setOrderID(resultSet.getInt("order_id"));
				}

				rooms.add(room);

				if (storeInMemory) {
					Roseau.getGame().getRoomManager().add(room);
				}
			}

		} catch (Exception e) {
			Log.exception(e);
		} finally {
			Storage.closeSilently(resultSet);
			Storage.closeSilently(preparedStatement);
			Storage.closeSilently(sqlConnection);
		}

		return rooms;
	}

	@Override
	public List<Integer> setRoomConnections(Room room) {

		List<Integer> connections = Lists.newArrayList();

		Connection sqlConnection = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;

		try {

			sqlConnection = this.dao.getStorage().getConnection();
			preparedStatement = this.dao.getStorage().prepare("SELECT * FROM room_public_connections WHERE room_id = ?", sqlConnection);
			preparedStatement.setInt(1, room.getData().getID());
			resultSet = preparedStatement.executeQuery();

			while (resultSet.next()) {

				int toRoomID = resultSet.getInt("to_id");

				if (!connections.contains(toRoomID)) {
					connections.add(toRoomID);
				}

				for (String coordinate : resultSet.getString("coordinates").split(" ")) {

					Position pos = new Position(coordinate);

					Position doorPosition = null;

					if (resultSet.getInt("door_x") > -1) {
						doorPosition = new Position(resultSet.getInt("door_x"), resultSet.getInt("door_y"), resultSet.getInt("door_z"));
					}

					room.getMapping().getConnections()[pos.getX()][pos.getY()] = new RoomConnection(room.getData().getID(), toRoomID, doorPosition);
				}
			}

		} catch (Exception e) {
			Log.exception(e);
		} finally {
			Storage.closeSilently(resultSet);
			Storage.closeSilently(preparedStatement);
			Storage.closeSilently(sqlConnection);
		}

		return connections;
	}

	@Override
	public List<Room> getPlayerRooms(PlayerDetails details, boolean storeInMemory) {

		List<Room> rooms = Lists.newArrayList();

		Connection sqlConnection = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;

		try {

			sqlConnection = this.dao.getStorage().getConnection();
			preparedStatement = this.dao.getStorage().prepare("SELECT * FROM rooms WHERE owner_id = " + details.getID(), sqlConnection);
			resultSet = preparedStatement.executeQuery();

			while (resultSet.next()) {

				int id = resultSet.getInt("id");

				Room room = Roseau.getGame().getRoomManager().getRoomByID(id);

				if (room == null) {
					room = this.fill(resultSet);
				}

				rooms.add(room);

				if (storeInMemory) {
					Roseau.getGame().getRoomManager().add(room);
				}
			}

		} catch (Exception e) {
			Log.exception(e);
		} finally {
			Storage.closeSilently(resultSet);
			Storage.closeSilently(preparedStatement);
			Storage.closeSilently(sqlConnection);
		}

		return rooms;
	}

	@Override
	public Room getRoom(int roomID) {
		return getRoom(roomID, false);
	}

	@Override
	public Room getRoom(int roomID, boolean storeInMemory) {

		Room room = null;
		Connection sqlConnection = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;

		try {

			sqlConnection = this.dao.getStorage().getConnection();
			preparedStatement = this.dao.getStorage().prepare("SELECT * FROM rooms WHERE id = " + roomID, sqlConnection);
			resultSet = preparedStatement.executeQuery();

			if (resultSet.next()) {

				int id = resultSet.getInt("id");

				room = Roseau.getGame().getRoomManager().getRoomByID(id);

				if (room == null) {
					room = this.fill(resultSet);
				}

				if (storeInMemory) {
					Roseau.getGame().getRoomManager().add(room);
				}
			}

		} catch (Exception e) {
			Log.exception(e);
		} finally {
			Storage.closeSilently(resultSet);
			Storage.closeSilently(preparedStatement);
			Storage.closeSilently(sqlConnection);
		}

		return room;
	}


	@Override
	public List<Integer> getRoomRights(int roomID) {

		List<Integer> rooms = Lists.newArrayList();

		Connection sqlConnection = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;

		try {

			sqlConnection = this.dao.getStorage().getConnection();
			preparedStatement = this.dao.getStorage().prepare("SELECT * FROM room_rights WHERE room_id = " + roomID, sqlConnection);
			resultSet = preparedStatement.executeQuery();

			while (resultSet.next()) {
				rooms.add(resultSet.getInt("user_id"));
			}

		} catch (Exception e) {
			Log.exception(e);
		} finally {
			Storage.closeSilently(resultSet);
			Storage.closeSilently(preparedStatement);
			Storage.closeSilently(sqlConnection);
		}


		return rooms;
	}

	@Override
	public void deleteRoom(Room room) {
		this.dao.getStorage().execute("DELETE FROM rooms WHERE id = " + room.getData().getID());
	}

	@Override
	public Room createRoom(Player player, String name, String description, String model, int state) {

		Connection sqlConnection = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;

		try {

			sqlConnection = this.dao.getStorage().getConnection();

			preparedStatement = dao.getStorage().prepare("INSERT INTO rooms (name, description, owner_id, model, state) VALUES (?, ?, ?, ?, ?)", sqlConnection);
			preparedStatement.setString(1, name);
			preparedStatement.setString(2, description);
			preparedStatement.setInt(3, player.getDetails().getID());
			preparedStatement.setString(4, model);
			preparedStatement.setInt(5, state);
			preparedStatement.executeUpdate();

			ResultSet row = preparedStatement.getGeneratedKeys();

			if (row != null && row.next()) {
				return this.getRoom(row.getInt(1), true);
			}

		} catch (SQLException e) {
			Log.exception(e);
		} finally {
			Storage.closeSilently(resultSet);
			Storage.closeSilently(preparedStatement);
			Storage.closeSilently(sqlConnection);
		}

		return null;
	}

	@Override
	public void updateRoom(Room room) {

		RoomData data = room.getData();


		Connection sqlConnection = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;

		try {

			sqlConnection = this.dao.getStorage().getConnection();

			preparedStatement = dao.getStorage().prepare("UPDATE rooms SET name = ?, description = ?, state = ?, password = ?, wallpaper = ?, floor = ? WHERE id = ?", sqlConnection);

			preparedStatement.setString(1, data.getName());
			preparedStatement.setString(2, data.getDescription());
			preparedStatement.setInt(3, data.getState().getStateCode());
			preparedStatement.setString(4, data.getPassword());
			preparedStatement.setString(5, data.getWall());
			preparedStatement.setString(6, data.getFloor());
			preparedStatement.setInt(7, data.getID());

			preparedStatement.executeUpdate();

		} catch (SQLException e) {
			Log.exception(e);
		} finally {
			Storage.closeSilently(resultSet);
			Storage.closeSilently(preparedStatement);
			Storage.closeSilently(sqlConnection);
		}

	}

	@Override
	public List<Bot> getBots(Room room, int roomID) {

		List<Bot> bots = Lists.newArrayList();

		Connection sqlConnection = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;

		try {

			sqlConnection = this.dao.getStorage().getConnection();

			preparedStatement = this.dao.getStorage().prepare("SELECT id, name,figure,motto,start_x,start_y,start_z,start_rotation,walk_to,messages,triggers,responses FROM room_bots WHERE room_id = ?", sqlConnection);
			preparedStatement.setInt(1, roomID);

			resultSet = preparedStatement.executeQuery();

			while (resultSet.next()) {

				List<int[]> positions = Lists.newArrayList();
				List<String> responses = Lists.newArrayList();
				List<String> triggers = Lists.newArrayList();

				for (String coordinate : resultSet.getString("walk_to").split(" ")) {
					int x = Integer.valueOf(coordinate.split(",")[0]);
					int y = Integer.valueOf(coordinate.split(",")[1]);
					positions.add(new int[] { x, y} );
				}

				String dbResponses = resultSet.getString("responses");

				if (dbResponses.contains("|")) {
					for (String response : dbResponses.split("|")) {
						responses.add(response);
					}
				} else {
					responses.add(dbResponses);
				}
				
				String dbTriggers = resultSet.getString("triggers");

				if (dbTriggers.contains(",")) {
					for (String trigger : dbTriggers.split(",")) {
						triggers.add(trigger);
					}
				} else {
					triggers.add(dbTriggers);
				}

				Bot bot = new Bot(
						new Position(
								resultSet.getInt("start_x"), 
								resultSet.getInt("start_y"),
								resultSet.getInt("start_z"), 
								resultSet.getInt("start_rotation")),
						positions,
						responses,
						triggers);

				bot.getDetails().fill(resultSet.getInt("id"), resultSet.getString("name"), resultSet.getString("motto"), resultSet.getString("figure"), "Male");

				bot.getRoomUser().getPosition().setX(bot.getStartPosition().getX());
				bot.getRoomUser().getPosition().setY(bot.getStartPosition().getY());
				bot.getRoomUser().getPosition().setZ(bot.getStartPosition().getZ());
				bot.getRoomUser().getPosition().setRotation(bot.getStartPosition().getBodyRotation(), false);

				bot.getRoomUser().setRoom(room);

				bots.add(bot);
			}

		} catch (Exception e) {
			Log.exception(e);
		} finally {
			Storage.closeSilently(resultSet);
			Storage.closeSilently(preparedStatement);
			Storage.closeSilently(sqlConnection);
		}

		return bots;
	}

	@Override
	public RoomModel getModel(String model) {
		return roomModels.get(model);
	}

	@Override
	public Room fill(ResultSet row) throws Exception {

		RoomType type = RoomType.getType(row.getInt("room_type"));

		PlayerDetails details = null;

		if (type == RoomType.PRIVATE) {
			details = Roseau.getDataAccess().getPlayer().getDetails(row.getInt("owner_id"));
		}

		Room instance = new Room();

		instance.getData().fill(row.getInt("id"), (row.getInt("hidden") == 1), type, details == null ? 0 : details.getID(), details == null ? "" : details.getUsername(), row.getString("name"), 
				row.getInt("state"), row.getString("password"), row.getInt("users_now"), row.getInt("users_max"), row.getString("description"), row.getString("model"),
				row.getString("cct"), row.getString("wallpaper"), row.getString("floor"));

		instance.load();

		return instance;
	}



}
