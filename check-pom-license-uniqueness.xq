(:
 : Elemental
 : Copyright (C) 2024, Evolved Binary Ltd
 :
 : admin@evolvedbinary.com
 : https://www.evolvedbinary.com | https://www.elemental.xyz
 :
 : This library is free software; you can redistribute it and/or
 : modify it under the terms of the GNU Lesser General Public
 : License as published by the Free Software Foundation; version 2.1.
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
 xquery version "3.1";

(:~
 : Checks within the <excludes> and <includes> of each <licenseSet> with a pom.xml
 : file to make sure there are no duplicate entries.
 :)

declare namespace output = "http://www.w3.org/2010/xslt-xquery-serialization";
declare namespace pom = "http://maven.apache.org/POM/4.0.0";

declare option output:omit-xml-declaration "yes";


(: Must be set externally with the file-system path to the pom.xml file :)
declare variable $pom-file-path as xs:string external;

declare %private function local:path-to-uri($path as xs:string) as xs:string {
    if (starts-with($path, "/"))
    then
        "file://" || $path
    else
        "file:///" || replace($path, "\\", "/")
};

let $pom-file-uri := local:path-to-uri($pom-file-path)
let $pom := doc($pom-file-uri)
return
(

  let $includes-elements := $pom//pom:includes
  for $includes-element in $includes-elements
  let $total-includes := count($includes-element/pom:include/string(.))
  let $distinct-includes := count(distinct-values($includes-element/pom:include/string(.)))
  return
    if ($total-includes ne $distinct-includes)
    then
      let $duplicates :=
          distinct-values(
              for $include in $includes-element/pom:include/string(.)
              where count($includes-element/pom:include[. eq $include]) gt 1
              return $include
          )
      return
        error(xs:QName("duplicate-include"), "There are duplicate 'include' license entries within a 'licenseSet' in: " || $pom-file-uri || " at: " || path($includes-element) || " duplicates: " || string-join($duplicates, ", "))
    else
      ()
      
,

  let $excludes-elements := $pom//pom:excludes[parent::pom:licenseSet]
  for $excludes-element in $excludes-elements
  let $total-excludes := count($excludes-element/pom:exclude/string(.))
  let $distinct-excludes := count(distinct-values($excludes-element/pom:exclude/string(.)))
  return
    if ($total-excludes ne $distinct-excludes)
    then
      let $duplicates :=
          distinct-values(
              for $exclude in $excludes-element/pom:exclude/string(.)
              where count($excludes-element/pom:exclude[. eq $exclude]) gt 1
              return $exclude
          )
      return
      error(xs:QName("duplicate-exclude"), "There are duplicate 'exclude' license entries within a 'licenseSet' in: " || $pom-file-uri || " at: " || path($excludes-element) || " duplicates: " || string-join($duplicates, ", "))
    else
      ()

)