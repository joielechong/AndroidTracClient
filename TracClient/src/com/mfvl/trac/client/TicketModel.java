package com.mfvl.trac.client;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.alexd.jsonrpc.JSONRPCException;
import org.alexd.jsonrpc.JSONRPCHttpClient;
import org.json.JSONArray;
import org.json.JSONException;

import com.mfvl.trac.client.util.Credentials;
import com.mfvl.trac.client.util.tcLog;

public class TicketModel extends Object implements Serializable, Cloneable {
	private static final long serialVersionUID = 4307815225424930343L;
	private final Map<String, TicketModelVeld> _velden;
	private final Map<Integer, String> _volgorde;
	private final String _url;
	private final String _username;
	private final String _password;
	private final boolean _sslHack;
	private final boolean _sslHostNameHack;
	private final Thread networkThread;
	private int _count = 0;
	private boolean loading;

	public TicketModel() {
		_url = Credentials.getUrl();
		_username = Credentials.getUsername();
		_password = Credentials.getPassword();
		_sslHack = Credentials.getSslHack();
		_sslHostNameHack = Credentials.getSslHostNameHack();

		_velden = new HashMap<String, TicketModelVeld>();
		_velden.clear();
		_volgorde = new TreeMap<Integer, String>();
		_volgorde.clear();
		loading = true;
		networkThread = new Thread() {
			@Override
			public void run() {
				tcLog.d(getClass().getName(), "TicketModel_url = "+_url);
				final JSONRPCHttpClient req = new JSONRPCHttpClient(_url, _sslHack, _sslHostNameHack);
				req.setCredentials(_username, _password);

				try {
					final JSONArray v = req.callJSONArray("ticket.getTicketFields");
					_count = v.length();
					for (int i = 0; i < _count; i++) {
						final String key = v.getJSONObject(i).getString("name");
						_velden.put(key, new TicketModelVeld(v.getJSONObject(i)));
						_volgorde.put(i, key);
					}
					_velden.put("max", new TicketModelVeld("max", "max", "500"));
					_volgorde.put(_count, "max");
					_velden.put("page", new TicketModelVeld("page", "page", "0"));
					_volgorde.put(_count + 1, "page");
				} catch (final JSONRPCException e) {
					e.printStackTrace();
				} catch (final JSONException e) {
					e.printStackTrace();
				} catch (final TicketModelException e) {
					e.printStackTrace();
				}
				loading = false;
			}
		};
		networkThread.start();
	}

	@Override
	public String toString() {
		wacht();
		String s = "";
		for (int i = 0; i < _count; i++) {
			s += _velden.get(_volgorde.get(i)) + "\n";
		}
		return s;
	}

	public ArrayList<String> velden() {
		final ArrayList<String> v = new ArrayList<String>();

		wacht();
		if (_count > 0) {
			v.add("id");
			for (int i = 0; i < _count + 2; i++) {
				v.add(_velden.get(_volgorde.get(i)).name());
			}
		}
		return v;
	}

	public int count() {
		wacht();
		return _count;
	}

	private void wacht() {
		if (loading) {
			try {
				networkThread.join();
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
		loading = false;
	}

	TicketModelVeld getVeld(final String naam) {
		wacht();
		return _velden.containsKey(naam) ? _velden.get(naam) : naam.equals("id") ? new TicketModelVeld(naam, naam, "0") : null;
	}

	TicketModelVeld getVeld(final int i) {
		return i < 0 || i >= _count ? null : getVeld(_volgorde.get(i));
	}

	private int hc(Object o) {
		return o == null ? 0 : o.hashCode();
	}

	@Override
	public int hashCode() {
		// Start with a non-zero constant.
		int result = 17;

		// Include a hash for each field.
		result = 31 * result + (_sslHack ? 1 : 0);
		result = 31 * result + (_sslHostNameHack ? 1 : 0);
		result = 31 * result + _count;
		result = 31 * result + hc(_velden);
		result = 31 * result + hc(_volgorde);
		result = 31 * result + hc(_url);
		result = 31 * result + hc(_username);
		result = 31 * result + hc(_password);
		return result + super.hashCode();
	}
}
