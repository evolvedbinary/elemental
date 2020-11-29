/*
 * Elemental
 * Copyright (C) 2024, Evolved Binary Ltd
 *
 * admin@evolvedbinary.com
 * https://www.evolvedbinary.com | https://www.elemental.xyz
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; version 2.1.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * NOTE: Parts of this file contain code from 'The eXist-db Authors'.
 *       The original license header is included below.
 *
 * =====================================================================
 *
 * eXist-db Open Source Native XML Database
 * Copyright (C) 2001 The eXist-db Authors
 *
 * info@exist-db.org
 * http://www.exist-db.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.exist.util;

import org.exist.mediatype.MediaTypeUtil;
import org.junit.jupiter.api.Test;
import xyz.elemental.mediatype.MediaType;
import xyz.elemental.mediatype.MediaTypeResolver;
import xyz.elemental.mediatype.StorageType;

import javax.annotation.Nullable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Test case for mime-type mapping.
 * Tests the distribution edition of mime-types.xml
 * as well as variants that exploit the default mime type feature
 * 
 * @author Peter Ciuffetti
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class MediaTypeResolverTest {

	/**
	 * This test checks the behavior of Media Type Resolver
	 * with respect to the distribution version of mime.types.
	 */
    @Test
	public void distributionVersionOfMimeTypesXml() {
        @Nullable final MediaTypeResolver mediaTypeResolver = MediaTypeUtil.newMediaTypeResolver(null);

		assertNotNull(mediaTypeResolver);

		@Nullable MediaType mediaType;

		mediaType = mediaTypeResolver.fromFileName("test.xml");
		assertNotNull(mediaType);
		assertEquals(MediaType.APPLICATION_XML, mediaType.getIdentifier());
		assertEquals(StorageType.XML, mediaType.getStorageType());

		mediaType = mediaTypeResolver.fromFileName("test.html");
		assertNotNull(mediaType);
		assertEquals(MediaType.TEXT_HTML, mediaType.getIdentifier());
		assertEquals(StorageType.XML, mediaType.getStorageType());

		mediaType = mediaTypeResolver.fromFileName("test.jpg");
		assertNotNull(mediaType);
		assertEquals(MediaType.IMAGE_JPEG, mediaType.getIdentifier());
		assertEquals(StorageType.BINARY, mediaType.getStorageType());

		mediaType = mediaTypeResolver.fromFileName("foo");
		assertNull(mediaType);

		mediaType = mediaTypeResolver.fromFileName("foo.bar");
		assertNull(mediaType);
	}
}
