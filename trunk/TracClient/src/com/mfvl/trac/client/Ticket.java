package com.mfvl.trac.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.concurrent.Semaphore;

import org.alexd.jsonrpc.JSONRPCException;
import org.alexd.jsonrpc.JSONRPCHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Base64;
import com.mfvl.trac.client.util.tcLog;

import com.mfvl.trac.client.util.ISO8601;

interface onTicketCompleteListener {
	void onComplete(Ticket t);
}

interface onAttachmentCompleteListener {
	void onComplete(byte[] data);
}

public class Ticket {
	public final static String TICKET_GET = "GET";
	public final static String TICKET_CHANGE = "CHANGE";
	public final static String TICKET_ATTACH = "ATTACH";
	public final static String TICKET_ACTION = "ACTION";

	private JSONObject _velden;
	private JSONArray _history;
	private JSONArray _attachments;
	private JSONArray _actions;
	private int _ticknr;
	private String _url;
	private String _username;
	private String _password;
	private boolean _sslHack;
	private boolean _hasdata = false;
	private boolean _isloading = false;
	private static Semaphore available = new Semaphore(1, true);
	private final Semaphore actionLock = new Semaphore(1, true);
	private String _rpcerror = null;
	/* static */private JSONRPCHttpClient req = null;

	public Ticket(final JSONObject velden) {
		_ticknr = -1;
		_velden = velden;
		_history = null;
		_attachments = null;
		_actions = null;
		actionLock.acquireUninterruptibly();
		_hasdata = true;
	}

	public Ticket(final int ticknr, final JSONObject velden, final JSONArray history, final JSONArray attachments,
			final JSONArray actions) {
		_ticknr = ticknr;
		_velden = velden;
		_history = history;
		_attachments = attachments;
		_actions = actions;
		if (actions == null) {
			actionLock.acquireUninterruptibly();
		}
		_hasdata = velden != null;
	}

	public Ticket(final int ticknr, TracStart context, onTicketCompleteListener oc) {
		tcLog.i(this.getClass().getName(), "Ticket(72) ticketnr = " + ticknr);
		_ticknr = ticknr;
		loadTicketData(context, oc);
	}

	public void refresh(TracStart context, onTicketCompleteListener oc) {
		tcLog.i(this.getClass().getName() + "refresh", "Ticketnr = " + _ticknr);
		actionLock.release();
		available.release();
		loadTicketData(context, oc);
	}

	public int getTicketnr() {
		return _ticknr;
	}

	@Override
	public String toString() {
		if (_velden == null) {
			return _ticknr + "";
		}
		try {
			return _ticknr + " - " + _velden.getString("status") + " - " + _velden.getString("summary");
		} catch (final JSONException e) {
			return _ticknr + "";
		}
	}

	private JSONObject makeComplexCall(String id, String method, Object... params) throws JSONException {
		final JSONObject call = new JSONObject();
		call.put("method", method);
		call.put("id", id);
		final JSONArray args = new JSONArray();
		for (final Object o : params) {
			args.put(o);
		}
		call.put("params", args);
		return call;
	}

	private void loadTicketData(TracStart context, final onTicketCompleteListener oc) {
		tcLog.i(this.getClass().getName(), "loadTicketData ticketnr = " + _ticknr);
		actionLock.acquireUninterruptibly();
		_isloading = true;
		if (_url == null) {
			_url = context.getUrl();
			_username = context.getUsername();
			_password = context.getPassword();
			_sslHack = context.getSslHack();
		}
		final Thread networkThread = new Thread() {
			@Override
			public void run() {
				available.acquireUninterruptibly();
				if (req == null) {
					req = new JSONRPCHttpClient(_url, _sslHack);
					req.setCredentials(_username, _password);
				}

				try {
					final JSONArray mc = new JSONArray();
					mc.put(makeComplexCall(TICKET_GET, "ticket.get", _ticknr));
					mc.put(makeComplexCall(TICKET_CHANGE, "ticket.changetcLog.d", _ticknr));
					mc.put(makeComplexCall(TICKET_ATTACH, "ticket.listAttachments", _ticknr));
					mc.put(makeComplexCall(TICKET_ACTION, "ticket.getActions", _ticknr));
					final JSONArray mcresult = req.callJSONArray("system.multicall", mc);
					_hasdata = false;
					_velden = null;
					_history = null;
					_attachments = null;
					_actions = null;
					for (int i = 0; i < mcresult.length(); i++) {
						try {
							final JSONObject res = mcresult.getJSONObject(i);
							final String id = res.getString("id");
							final JSONArray result = res.getJSONArray("result");
							if (id.equals(TICKET_GET)) {
								_velden = result.getJSONObject(3);
							} else if (id.equals(TICKET_CHANGE)) {
								_history = result;
							} else if (id.equals(TICKET_ATTACH)) {
								_attachments = result;
							} else if (id.equals(TICKET_ACTION)) {
								_actions = result;
								actionLock.release();
							} else {
								tcLog.i(this.getClass().getName(), "loadTicketData, onverwachte respons = " + result);
							}
						} catch (final Exception e1) {
							e1.printStackTrace();
						}
					}
					_hasdata = _velden != null && _history != null && _actions != null;
					_isloading = false;
					if (oc != null) {
						available.release();
						oc.onComplete(Ticket.this);
					}
				} catch (final JSONRPCException e) {
					tcLog.i(this.getClass().getName() + "loadTicketData", e.toString());
				} catch (final JSONException e) {
					tcLog.i(this.getClass().getName() + "loadTicketData", e.toString());
				} catch (final Exception e) {
					e.printStackTrace();
				} finally {
					available.release();
				}
			}
		};
		networkThread.start();
	}

	public void getAttachment(final String filename, TracStart context, final onAttachmentCompleteListener oc) {
		_url = context.getUrl();
		_username = context.getUsername();
		_password = context.getPassword();
		_sslHack = context.getSslHack();
		final Thread networkThread = new Thread() {
			@Override
			public void run() {
				available.acquireUninterruptibly();
				if (req == null) {
					req = new JSONRPCHttpClient(_url, _sslHack);
					req.setCredentials(_username, _password);
				}

				try {
					final JSONObject data = req.callJSONObject("ticket.getAttachment", _ticknr, filename);
					final String b64 = data.getJSONArray("__jsonclass__").getString(1);
					if (oc != null) {
						oc.onComplete(Base64.decode(b64, Base64.DEFAULT));
					}
				} catch (final Exception e) {
					tcLog.i(this.getClass().getName() + "getAttachment", e.toString());
				} finally {
					available.release();
				}
			}
		};
		networkThread.start();
		try {
			networkThread.join();
			if (_rpcerror != null) {
				throw new RuntimeException(_rpcerror);
			}
		} catch (final Exception e) {
		}
	}

	public void addAttachment(final String filename, final TracStart context, final onTicketCompleteListener oc) {
		_url = context.getUrl();
		_username = context.getUsername();
		_password = context.getPassword();
		_sslHack = context.getSslHack();
		final Thread networkThread = new Thread() {
			@Override
			public void run() {
				available.acquireUninterruptibly();
				if (req == null) {
					req = new JSONRPCHttpClient(_url, _sslHack);
					req.setCredentials(_username, _password);
				}
				final File file = new File(filename);
				final int bytes = (int) file.length();
				final byte[] data = new byte[bytes];
				try {
					final InputStream is = new FileInputStream(file);
					is.read(data);
					final String b64 = Base64.encodeToString(data, Base64.DEFAULT);
					final JSONArray ar = new JSONArray();
					ar.put(_ticknr);
					ar.put(file.getName());
					ar.put("");
					final JSONArray ar1 = new JSONArray();
					ar1.put("binary");
					ar1.put(b64);
					final JSONObject ob = new JSONObject();
					ob.put("__jsonclass__", ar1);
					ar.put(ob);
					ar.put(true);
					final String retfile = req.callString("ticket.putAttachment", ar);
					tcLog.i(this.getClass().getName() + "putAttachment", retfile);
					actionLock.release();
					loadTicketData(context, null);
					if (oc != null) {
						oc.onComplete(Ticket.this);
					}
				} catch (final Exception e) {
					tcLog.i(this.getClass().getName() + "addAttachment", e.toString());
				} finally {
					available.release();
				}
			}
		};
		networkThread.start();
	}

	public String getString(final String veld) throws JSONException {
		try {
			return _velden.getString(veld);
		} catch (final NullPointerException e) {
			return "";
		}
	}

	public JSONObject getJSONObject(final String veld) throws JSONException {
		return _velden.getJSONObject(veld);
	}

	public JSONArray getFields() {
		return _velden.names();
	}

	public void setFields(JSONObject velden) {
		_velden = velden;
		_hasdata = true;
	}

	public JSONArray getHistory() {
		return _history;
	}

	public void setHistory(JSONArray history) {
		_history = history;
	}

	public JSONArray getActions() {
		actionLock.acquireUninterruptibly();
		try {
			return _actions;
		} finally {
			actionLock.release();
		}
	}

	public void setActions(JSONArray actions) {
		_actions = actions;
		actionLock.release();
	}

	public JSONArray getAttachments() {
		return _attachments;
	}

	public void setAttachments(JSONArray attachments) {
		_attachments = attachments;
	}

	public String getAttachmentFile(int nr) throws JSONException {
		return _attachments.getJSONArray(nr).getString(0);
	}

	public boolean hasdata() {
		return _hasdata;
	}

	public boolean isloading() {
		return _isloading;
	}

	public int create(final TracStart context, final boolean notify) throws Exception {
		if (_ticknr != -1) {
			throw new RuntimeException("Aanroep met niet -1");
		}
		tcLog.i(this.getClass().getName(), "create: " + _velden.toString());
		final String s = _velden.getString("summary");
		final String d = _velden.getString("description");
		_velden.remove("summary");
		_velden.remove("description");

		final Thread networkThread = new Thread() {
			@Override
			public void run() {
				available.acquireUninterruptibly();
				try {
					_url = context.getUrl();
					_username = context.getUsername();
					_password = context.getPassword();
					_sslHack = context.getSslHack();
					if (req == null) {
						req = new JSONRPCHttpClient(_url, _sslHack);
						req.setCredentials(_username, _password);
					}
					final int newticknr = req.callInt("ticket.create", s, d, _velden);
					_ticknr = newticknr;
					actionLock.release();
					loadTicketData(context, null);
				} catch (final JSONRPCException e) {
					try {
						final JSONObject o = new JSONObject(e.getMessage());
						_rpcerror = o.getString("message");
					} catch (final JSONException e1) {
						e1.printStackTrace();
						_rpcerror = context.getString(R.string.invalidJson);
					}
				}
				available.release();
			}
		};
		networkThread.start();
		try {
			networkThread.join();
			if (_rpcerror != null) {
				throw new RuntimeException(_rpcerror);
			}
		} catch (final Exception e) {
			throw e;
		}
		if (_ticknr == -1) {
			throw new RuntimeException(context.getString(R.string.noticketUnk));
		}

		return _ticknr;
	}

	public void update(String action, String comment, String veld, String waarde, final boolean notify, final TracStart context)
			throws Exception {
		tcLog.i(this.getClass().getName(), "update: " + action + " '" + comment + "' '" + veld + "' '" + waarde + "'");
		if (_ticknr == -1) {
			throw new Exception(context.getString(R.string.invtick) + " " + _ticknr);
		}
		if (action == null) {
			throw new Exception(context.getString(R.string.noaction));
		}
		_url = context.getUrl();
		_username = context.getUsername();
		_password = context.getPassword();
		_sslHack = context.getSslHack();
		_velden.put("action", action);
		if (waarde != null && veld != null && !veld.equals("") && !waarde.equals("")) {
			_velden.put(veld, waarde);
		}
		final String cmt = comment == null ? "" : comment;
		_velden.remove("changetime");
		_velden.remove("time");

		final Thread networkThread = new Thread() {
			@Override
			public void run() {
				available.acquireUninterruptibly();
				try {
					if (_url != null) {
						if (req == null) {
							req = new JSONRPCHttpClient(_url, _sslHack);
							req.setCredentials(_username, _password);
						}
						req.callJSONArray("ticket.update", _ticknr, cmt, _velden, notify);
						actionLock.release();
						loadTicketData(context, null);
					}
				} catch (final JSONRPCException e) {
					try {
						e.printStackTrace();
						final JSONObject o = new JSONObject(e.getMessage());
						_rpcerror = o.getString("message");
					} catch (final JSONException e1) {
						e1.printStackTrace();
						_rpcerror = context.getString(R.string.invalidJson);
					}
				} finally {
					available.release();
				}
			}
		};
		networkThread.start();
		try {
			networkThread.join();
			if (_rpcerror != null) {
				throw new RuntimeException(_rpcerror);
			}
		} catch (final Exception e) {
			throw e;
		}
	}

	private String toonTijd(final JSONObject v) {
		try {
			return ISO8601.toCalendar(v.getJSONArray("__jsonclass__").getString(1) + "Z").getTime().toString();
		} catch (final Exception e) {
			tcLog.i(this.getClass().getName(), e.toString());
			return "";
		}
	}

	public String toText() {
		String tekst = "Ticket: " + _ticknr;

		try {
			final JSONArray fields = _velden.names();
			tekst += " " + _velden.getString("summary") + "\n\n";
			final int count = fields.length();
			for (int i = 0; i < count; i++) {
				String veld = "veld " + i;
				try {
					veld = fields.getString(i);
					if ("summary".equals(veld) || "_ts".equals(veld)) {
						// skip
					} else if ("time".equals(veld) || "changetime".equals(veld)) {
						tekst += veld + ":\t" + toonTijd(_velden.getJSONObject(veld)) + "\n";
					} else if (_velden.getString(veld).length() > 0) {
						tekst += veld + ":\t" + _velden.getString(veld) + "\n";
					}

				} catch (final Exception e) {

				}
			}
		} catch (final JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for (int j = 0; j < _history.length(); j++) {
			JSONArray cmt;
			try {
				cmt = _history.getJSONArray(j);
				if ("comment".equals(cmt.getString(2)) && cmt.getString(4).length() > 0) {
					tekst += "comment: " + toonTijd(cmt.getJSONObject(0)) + " - " + cmt.getString(1) + " - " + cmt.getString(4)
							+ "\n";
				}
			} catch (final JSONException e) {
				e.printStackTrace();
			}
		}
		for (int j = 0; j < _attachments.length(); j++) {
			JSONArray bijlage;
			try {
				bijlage = _attachments.getJSONArray(j);
				tekst += "bijlage " + (j + 1) + ": " + toonTijd(bijlage.getJSONObject(3)) + " - " + bijlage.getString(4) + " - "
						+ bijlage.getString(0) + " - " + bijlage.getString(1) + "\n";
			} catch (final JSONException e) {
				e.printStackTrace();
			}
		}
		return tekst;
	}

}
