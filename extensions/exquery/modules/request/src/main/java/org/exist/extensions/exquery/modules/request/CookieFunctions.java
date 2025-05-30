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
package org.exist.extensions.exquery.modules.request;

import org.exist.dom.QName;
import org.exist.xquery.Cardinality;
import org.exist.xquery.FunctionSignature;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.value.FunctionParameterSequenceType;
import org.exist.xquery.value.FunctionReturnSequenceType;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.SequenceType;
import org.exist.xquery.value.StringValue;
import org.exist.xquery.value.Type;
import org.exquery.http.HttpRequest;

/**
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class CookieFunctions extends AbstractRequestModuleFunction {

    private final static QName qnCookie = new QName("cookie", RequestModule.NAMESPACE_URI, RequestModule.PREFIX);
    
    
    public final static FunctionSignature FNS_COOKIE = new FunctionSignature(
        qnCookie,
        "Gets the value of the named Cookie in the HTTP Request. If there is no such cookie, then an empty sequence is returned.",
        new SequenceType[] {
            new FunctionParameterSequenceType("cookie-name", Type.STRING, Cardinality.EXACTLY_ONE, "The name of the cookie to retrieve the value of.")
        },
        new FunctionReturnSequenceType(Type.STRING, Cardinality.ZERO_OR_ONE, "The value of the named cookie, or an empty sequence.")
    );
    
    public final static FunctionSignature FNS_COOKIE_WITH_DEFAULT = new FunctionSignature(
        qnCookie,
        "Gets he value of the named Cookie in the HTTP Request. If there is no such cookie in the HTTP Request, then the value specified in $default is returned instead.",
        new SequenceType[] {
            new FunctionParameterSequenceType("cookie-name", Type.STRING, Cardinality.EXACTLY_ONE, "The name of the cookie to retrieve the value of."),
            new FunctionParameterSequenceType("default", Type.STRING, Cardinality.EXACTLY_ONE, "The default value to use if the named cookie is not present in the request.")
        },
        new FunctionReturnSequenceType(Type.STRING, Cardinality.EXACTLY_ONE, "The value of the named cookie, or the default value.")
    );
    
    public CookieFunctions(final XQueryContext context, final FunctionSignature signature) {
        super(context, signature);
    }
    
    @Override
    public Sequence eval(final Sequence[] args, final HttpRequest request) throws XPathException {
        final Sequence result;
        
        if(isCalledAs(qnCookie.getLocalPart())) {
            final String cookieName = args[0].getStringValue();
            
            if(getSignature().getArgumentCount() == 1) {
                result = getCookie(request, cookieName, null);
            } else if(getSignature().getArgumentCount() == 2) {
                final Sequence defaultValues = args[1];
                result = getCookie(request, cookieName, defaultValues);
            } else {
                throw new XPathException(this, "Unknown function call: " + getSignature());
            }   
        } else {
            throw new XPathException(this, "Unknown function call: " + getSignature());
        }
        
        return result;
    }
    
    private Sequence getCookie(final HttpRequest request, final String cookieName, final Sequence defaultValues) throws XPathException {
        final Sequence result;
        final String cookieValue = request.getCookieValue(cookieName);
        if(cookieValue == null) {
            if(defaultValues != null) {
                result = defaultValues;
            } else {
                result = Sequence.EMPTY_SEQUENCE;
            }
        } else {
            result = new StringValue(this, cookieValue);
        }
        
        return result;
    }
}
