package com.mfvl.trac.client.util;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class XMLHandler extends DefaultHandler {

	String _appname = null;
	int state = -1;
	private String profileName;
	private LoginProfile lp;
	private final ProfileDatabaseHelper _pdb;

	public XMLHandler(String appname, ProfileDatabaseHelper pdb) {
		super();
		_appname = appname;
		_pdb = pdb;
		state = 0;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException,
			RuntimeException {

		if (localName.equals(_appname) && state == 0) {
			state++;
		} else if (localName.equals(ProfileDatabaseHelper.TABLE_NAME) && state == 1) {
			state++;
			_pdb.beginTransaction();
			if (_pdb.delProfiles() == -1) {
				throw new RuntimeException("delProfiles mislukt");
			}
		} else if (localName.equals("profile") && state == 2) {
			state++;
			lp = new LoginProfile(attributes.getValue(ProfileDatabaseHelper.URL_ID),
					attributes.getValue(ProfileDatabaseHelper.USERNAME_ID), attributes.getValue(ProfileDatabaseHelper.PASSWORD_ID),
					attributes.getValue(ProfileDatabaseHelper.SSLHACK_ID).equals("1"));
			profileName = attributes.getValue(ProfileDatabaseHelper.NAME_ID);
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (localName.equals(_appname) && state == 1) {
			state--;
		} else if (localName.equals(ProfileDatabaseHelper.TABLE_NAME) && state == 2) {
			_pdb.endTransaction();
			state--;
		} else if (localName.equals("profile") && state == 3) {
			_pdb.addProfile(profileName, lp);
			state--;
		}
	}
}
