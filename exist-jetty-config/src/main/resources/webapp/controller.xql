(:
 : Elemental
 : Copyright (C) 2024, Evolved Binary Ltd
 :
 : admin@evolvedbinary.com
 : https://www.evolvedbinary.com | https://www.elemental.xyz
 :
 : Use of this software is governed by the Business Source License 1.1
 : included in the LICENSE file and at www.mariadb.com/bsl11.
 :
 : Change Date: 2028-04-27
 :
 : On the date above, in accordance with the Business Source License, use
 : of this software will be governed by the Apache License, Version 2.0.
 :
 : Additional Use Grant: Production use of the Licensed Work for a permitted
 : purpose. A Permitted Purpose is any purpose other than a Competing Use.
 : A Competing Use means making the Software available to others in a commercial
 : product or service that: substitutes for the Software; substitutes for any
 : other product or service we offer using the Software that exists as of the
 : date we make the Software available; or offers the same or substantially
 : similar functionality as the Software.
 :
 : NOTE: Parts of this file contain code from 'The eXist-db Authors'.
 :       The original license header is included below.
 :
 : =====================================================================
 :
 : eXist-db Open Source Native XML Database
 : Copyright (C) 2001 The eXist-db Authors
 :
 : info@exist-db.org
 : http://www.exist-db.org
 :
 : This library is free software; you can redistribute it and/or
 : modify it under the terms of the GNU Lesser General Public
 : License as published by the Free Software Foundation; either
 : version 2.1 of the License, or (at your option) any later version.
 :
 : This library is distributed in the hope that it will be useful,
 : but WITHOUT ANY WARRANTY; without even the implied warranty of
 : MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 : Lesser General Public License for more details.
 :
 : You should have received a copy of the GNU Lesser General Public
 : License along with this library; if not, write to the Free Software
 : Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 :)
(: 	This is the main controller for the web application. It is called from the
	XQueryURLRewrite filter configured in web.xml. :)
xquery version "3.0";

(:~ -------------------------------------------------------
    Main controller: handles all requests not matched by
    sub-controllers.
    ------------------------------------------------------- :)

declare namespace c="http://exist-db.org/xquery/controller";
declare namespace expath="http://expath.org/ns/pkg";

import module namespace request="http://exist-db.org/xquery/request";
import module namespace xdb = "http://exist-db.org/xquery/xmldb";

declare function local:get-dashboard() {
	let $path := collection(repo:get-root())//expath:package[@name = "http://exist-db.org/apps/dashboard"]
    return
        if ($path) then
            substring-after(util:collection-name($path), repo:get-root())
        else
            ()
};

let $query := request:get-parameter("q", ())
return
	(: redirect webapp root to index.xml :)
    if ($exist:path eq '') then
	   <dispatch xmlns="http://exist.sourceforge.net/NS/exist">
	     <redirect url="{concat(request:get-uri(), '/')}"/>
	   </dispatch>
    else if ($exist:path eq '/') then
    	let $dashboard := local:get-dashboard()
    	return
			<dispatch xmlns="http://exist.sourceforge.net/NS/exist">
			{
			if ($dashboard) then
                if(request:get-uri() = "/exist/" and request:get-header("X-Forwarded-URI") = "/") then
                   <redirect url="/apps/{$dashboard}/"/>
                else
                   <redirect url="apps/{$dashboard}/"/>
            else
                <redirect url="404.html"/>
			}
			</dispatch>
	else
		<ignore xmlns="http://exist.sourceforge.net/NS/exist">
            <cache-control cache="yes"/>
		</ignore>
