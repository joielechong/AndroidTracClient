/*
 * Copyright (C) 2013,2014 Michiel van Loon
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mfvl.trac.client.util;

/**
 * Helper class for handling ISO 8601 strings of the following format:
 * "2008-03-01T13:00:00+01:00". It also supports parsing the "Z" timezone.
 */
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import android.annotation.SuppressLint;

public final class ISO8601 {
	/** Transform Calendar to ISO 8601 string. */
	@SuppressLint("SimpleDateFormat")
	public static String fromCalendar(final Calendar calendar) {
		final Date date = calendar.getTime();
		final String formatted = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(date);
		return formatted.substring(0, 22) + ":" + formatted.substring(22);
	}

	@SuppressLint("SimpleDateFormat")
	public static String fromUnix(final long tijd) {
		final Date date = new Date();
		date.setTime(tijd);
		final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		return sdf.format(date);
	}

	/** Get current date and time formatted as ISO 8601 string. */
	public static String now() {
		return fromCalendar(Calendar.getInstance());
	}

	/** Transform ISO 8601 string to Calendar. */
	@SuppressLint("SimpleDateFormat")
	public static Calendar toCalendar(final String iso8601string) throws ParseException {
		final Calendar calendar = Calendar.getInstance();
		String s = iso8601string.replace("Z", "+00:00");
		try {
			s = s.substring(0, 22) + s.substring(23);
		} catch (final IndexOutOfBoundsException e) {
			throw new ParseException("Invalid length", 0);
		}
		final Date date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").parse(s);
		calendar.setTime(date);
		return calendar;
	}
}