/*
 * Copyright © 2001, Adam Retter
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.exist.extensions.exquery.restxq.impl.adapters;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import org.exist.xquery.value.Type;

import javax.annotation.Nullable;

/**
 *
 * @author <a href="mailto:adam.retter@googlemail.com">Adam Retter</a>
 */
public class TypeAdapter {

    private static final Int2ObjectMap<org.exquery.xquery.Type> EXISTDB_TO_EXQUERY_MAP = new Int2ObjectOpenHashMap<>(53);
    private static final Reference2IntMap<org.exquery.xquery.Type> EXQUERY_TO_EXISTDB_MAP = new Reference2IntOpenHashMap<>(53);

    private static void addMapping(final int existdbType, final org.exquery.xquery.Type exqueryType) {
        EXISTDB_TO_EXQUERY_MAP.put(existdbType, exqueryType);
        EXQUERY_TO_EXISTDB_MAP.put(exqueryType, existdbType);
    }

    static {
            addMapping(Type.NODE,
                org.exquery.xquery.Type.NODE);
            
            addMapping(Type.ELEMENT,
                org.exquery.xquery.Type.ELEMENT);
                
            addMapping(Type.ATTRIBUTE,
                org.exquery.xquery.Type.ATTRIBUTE);
                
            addMapping(Type.TEXT,
                org.exquery.xquery.Type.TEXT);
            
            addMapping(Type.PROCESSING_INSTRUCTION,
                org.exquery.xquery.Type.PROCESSING_INSTRUCTION);
                
            addMapping(Type.COMMENT,
                org.exquery.xquery.Type.COMMENT);
                    
            addMapping(Type.DOCUMENT,
                org.exquery.xquery.Type.DOCUMENT);
                    
            addMapping(Type.ITEM,
                org.exquery.xquery.Type.ITEM);
                
            addMapping(Type.ANY_TYPE,
                org.exquery.xquery.Type.ANY_TYPE);
                
            addMapping(Type.ANY_SIMPLE_TYPE,
                org.exquery.xquery.Type.ANY_SIMPLE_TYPE);
                
            addMapping(Type.UNTYPED,
                org.exquery.xquery.Type.UNTYPED);
                
            addMapping(Type.STRING,
                org.exquery.xquery.Type.STRING);
                
            addMapping(Type.BOOLEAN,
                org.exquery.xquery.Type.BOOLEAN);
                
            addMapping(Type.QNAME,
                org.exquery.xquery.Type.QNAME);
                
            addMapping(Type.ANY_URI,
                org.exquery.xquery.Type.ANY_URI);
                
            addMapping(Type.BASE64_BINARY,
                org.exquery.xquery.Type.BASE64_BINARY);
                
            addMapping(Type.HEX_BINARY,
                org.exquery.xquery.Type.HEX_BINARY);
            
            addMapping(Type.NOTATION,
                org.exquery.xquery.Type.NOTATION);
               
            addMapping(Type.INTEGER,
                org.exquery.xquery.Type.INTEGER);
                    
            addMapping(Type.DECIMAL,
                org.exquery.xquery.Type.DECIMAL);
                
            addMapping(Type.FLOAT,
                org.exquery.xquery.Type.FLOAT);
                    
            addMapping(Type.DOUBLE,
                org.exquery.xquery.Type.DOUBLE);
                
            addMapping(Type.NON_POSITIVE_INTEGER,
                org.exquery.xquery.Type.NON_POSITIVE_INTEGER);
                
            addMapping(Type.NEGATIVE_INTEGER,
                org.exquery.xquery.Type.NEGATIVE_INTEGER);
                    
            addMapping(Type.LONG,
                org.exquery.xquery.Type.LONG);
                
            addMapping(Type.INT,
                org.exquery.xquery.Type.INT);
                
            addMapping(Type.SHORT,
                org.exquery.xquery.Type.SHORT);
                
            addMapping(Type.BYTE,
                org.exquery.xquery.Type.BYTE);
                
            addMapping(Type.NON_NEGATIVE_INTEGER,
                org.exquery.xquery.Type.NON_NEGATIVE_INTEGER);
                
            addMapping(Type.UNSIGNED_LONG,
                org.exquery.xquery.Type.UNSIGNED_LONG);

            addMapping(Type.UNSIGNED_SHORT,
                org.exquery.xquery.Type.UNSIGNED_SHORT);
                
            addMapping(Type.UNSIGNED_BYTE,
                org.exquery.xquery.Type.UNSIGNED_BYTE);
                
            addMapping(Type.POSITIVE_INTEGER,
                org.exquery.xquery.Type.POSITIVE_INTEGER);
    
            addMapping(Type.DATE_TIME,
                org.exquery.xquery.Type.DATE_TIME);
                    
            addMapping(Type.DATE,
                org.exquery.xquery.Type.DATE);
                
            addMapping(Type.TIME,
                org.exquery.xquery.Type.TIME);
                
            addMapping(Type.DURATION,
                org.exquery.xquery.Type.DURATION);
                
            addMapping(Type.YEAR_MONTH_DURATION,
                org.exquery.xquery.Type.YEAR_MONTH_DURATION);
                
            addMapping(Type.DAY_TIME_DURATION,
                org.exquery.xquery.Type.DAY_TIME_DURATION);
                
            addMapping(Type.G_YEAR,
                org.exquery.xquery.Type.G_YEAR);
                
            addMapping(Type.G_MONTH,
                org.exquery.xquery.Type.G_MONTH);
                
            addMapping(Type.G_DAY,
                org.exquery.xquery.Type.G_DAY);
                
            addMapping(Type.G_YEAR_MONTH,
                org.exquery.xquery.Type.G_YEAR_MONTH);
                
            addMapping(Type.G_MONTH_DAY,
                org.exquery.xquery.Type.G_MONTH_DAY);
    
            addMapping(Type.TOKEN,
                org.exquery.xquery.Type.TOKEN);
                
            addMapping(Type.NORMALIZED_STRING,
                org.exquery.xquery.Type.NORMALIZED_STRING);
                
            addMapping(Type.LANGUAGE,
                org.exquery.xquery.Type.LANGUAGE);
                
            addMapping(Type.NMTOKEN,
                org.exquery.xquery.Type.NM_TOKEN);
                
            addMapping(Type.NAME,
                org.exquery.xquery.Type.NAME);
                
            addMapping(Type.NCNAME,
                org.exquery.xquery.Type.NC_NAME);
                
            addMapping(Type.ID,
                org.exquery.xquery.Type.ID);
                
            addMapping(Type.IDREF,
                org.exquery.xquery.Type.ID_REF);
                
            addMapping(Type.ENTITY,
                org.exquery.xquery.Type.ENTITY);
    }
    
    public static org.exquery.xquery.Type toExQueryType(final int type) {
        @Nullable org.exquery.xquery.Type exQueryType = EXISTDB_TO_EXQUERY_MAP.get(type);
        if(exQueryType == null) {
            exQueryType = org.exquery.xquery.Type.ANY_TYPE;
        }
        
        return exQueryType;
    }
    
    public static int toExistType(final org.exquery.xquery.Type type) {
        int existType = EXQUERY_TO_EXISTDB_MAP.getOrDefault(type,-99);
        if (existType == -99) {
            existType = Type.ANY_TYPE;
        }

        return existType;
    }
}
