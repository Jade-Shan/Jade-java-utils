package net.jadedungeon.javautil.mongo.impl;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;

import net.jadedungeon.javautil.mongo.MongoServer;

public class MongoConnection {
	private MongoClient client;;

	public MongoConnection(List<MongoServer> serverList)
			throws UnknownHostException {

		if (null != serverList && serverList.size() > 0) {
			List<ServerAddress> addrlist = new ArrayList<>();
			for (MongoServer s : serverList) {
				addrlist.add(new ServerAddress(s.getHost(), s.getPort()));
			}
			client = new MongoClient(addrlist);
		}
	}

	public void close() {
		if (null != client) {
			client.close();
		}
	}

}
