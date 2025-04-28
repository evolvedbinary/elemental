/*
 * Elemental
 * Copyright (C) 2024, Evolved Binary Ltd
 *
 * admin@evolvedbinary.com
 * https://www.evolvedbinary.com | https://www.elemental.xyz
 *
 * Use of this software is governed by the Business Source License 1.1
 * included in the LICENSE file and at www.mariadb.com/bsl11.
 *
 * Change Date: 2028-04-27
 *
 * On the date above, in accordance with the Business Source License, use
 * of this software will be governed by the Apache License, Version 2.0.
 *
 * Additional Use Grant: Production use of the Licensed Work for a permitted
 * purpose. A Permitted Purpose is any purpose other than a Competing Use.
 * A Competing Use means making the Software available to others in a commercial
 * product or service that: substitutes for the Software; substitutes for any
 * other product or service we offer using the Software that exists as of the
 * date we make the Software available; or offers the same or substantially
 * similar functionality as the Software.
 */
package org.exist.collections.triggers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.Set;
import java.util.StringTokenizer;

public enum TriggerEvent {

	CREATE_COLLECTION,
	UPDATE_COLLECTION,
	COPY_COLLECTION,
	MOVE_COLLECTION,
	DELETE_COLLECTION,

	CREATE_DOCUMENT,
	UPDATE_DOCUMENT,
	COPY_DOCUMENT,
	MOVE_DOCUMENT,
	DELETE_DOCUMENT;

	private static final Logger LOG = LogManager.getLogger(TriggerEvent.class);

	@Deprecated
	public String legacyEventName() {
		return name().replace('_', '-');
	}

	@Deprecated
	public static @Nullable
	TriggerEvent forLegacyEventName(final String legacyEventName) {
		for (final TriggerEvent event : TriggerEvent.values()) {
			if (event.legacyEventName().equals(legacyEventName)) {
				return event;
			}
		}
		return null;
	}

	public static Set<TriggerEvent> convertFromLegacyEventNamesString(final String events) {
		final Set<TriggerEvent> result = EnumSet.noneOf(TriggerEvent.class);
		final StringTokenizer tok = new StringTokenizer(events, ", ");
		while (tok.hasMoreTokens()) {
			final String eventStr = tok.nextToken();
			final TriggerEvent event = TriggerEvent.forLegacyEventName(eventStr);
			if (event == null) {
//	        	throw new TriggerException("Unknown event type: " + eventStr);
				LOG.warn("Unknown event when converting from legacy event names string: " + eventStr);
			} else {
				result.add(event);
			}
		}
		return result;
	}

	public static Set<TriggerEvent> convertFromOldDesign(final String events) {
		final Set<TriggerEvent> result = EnumSet.noneOf(TriggerEvent.class);
		final StringTokenizer tok = new StringTokenizer(events, ", ");
		while (tok.hasMoreTokens()) {
			final String eventStr = tok.nextToken();
			switch (eventStr) {
				case "STORE":
					result.add(TriggerEvent.CREATE_DOCUMENT);
					break;

				case "UPDATE":
					result.add(TriggerEvent.UPDATE_DOCUMENT);
					break;

				case "REMOVE":
					result.add(TriggerEvent.DELETE_DOCUMENT);
					break;

				default:
					LOG.warn("Unknown event when converting from old design event names string: " + eventStr);
//					throw new TriggerException("Unknown event type: " + eventStr);
			}
		}
		return result;
	}
}
