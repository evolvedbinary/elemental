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
 :)
xquery version "3.1";

module namespace ist = "http://exist-db.org/xquery/image/scale/test";


import module namespace image = "http://exist-db.org/xquery/image";
import module namespace util = "http://exist-db.org/xquery/util";
import module namespace xmldb = "http://exist-db.org/xquery/xmldb";

declare namespace test = "http://exist-db.org/xquery/xqsuite";

declare
    %test:setUp
function ist:setup() {
  let $_ := xmldb:create-collection("/db", "image-scale-test")
  let $img := file:read-binary("src/test/resources/h+p.jpeg")
  return
    xmldb:store-as-binary("/db/image-scale-test", "h+p.jpeg", $img)
};

declare
    %test:tearDown
function ist:tear-down() {
  xmldb:remove("/db/image-scale-test")
};

declare
    %test:assertEquals("/db/image-scale-test/h+p_scale_legacy_200.jpeg")
function ist:scale-down-legacy() {
  let $source-img := util:binary-doc("/db/image-scale-test/h+p.jpeg")
  let $scaled-img := image:scale($source-img, (200, 200), "image/jpeg")
  return
    xmldb:store-as-binary("/db/image-scale-test", "h+p_scale_legacy_200.jpeg", $scaled-img)
};

declare
    %test:assertEquals("/db/image-scale-test/h+p_scale_legacy_1000.jpeg")
function ist:scale-up-legacy() {
  let $source-img := util:binary-doc("/db/image-scale-test/h+p.jpeg")
  let $scaled-img := image:scale($source-img, (1000, 1000), "image/jpeg")
  return
    xmldb:store-as-binary("/db/image-scale-test", "h+p_scale_legacy_1000.jpeg", $scaled-img)
};

declare
    %test:assertEquals("/db/image-scale-test/h+p_scale_200_bicubic.jpeg")
function ist:scale-down-bicubic() {
  let $source-img := util:binary-doc("/db/image-scale-test/h+p.jpeg")
  let $scaled-img := image:scale(
        $source-img,
        map {
            "source": map {
                "media-type": "image/jpeg"
            },
            "destination": map {
                "max-width": 200,
                "max-height": 200,
                "rendering-hints": map {
                    $image:interpolation: $image:interpolation_bicubic
                }
            }
        }
  )
  return
    xmldb:store-as-binary("/db/image-scale-test", "h+p_scale_200_bicubic.jpeg", $scaled-img)
};

declare
    %test:assertEquals("/db/image-scale-test/h+p_scale_1000_bicubic.jpeg")
function ist:scale-up-bicubic() {
  let $source-img := util:binary-doc("/db/image-scale-test/h+p.jpeg")
  let $scaled-img := image:scale(
        $source-img,
        map {
            "source": map {
                "media-type": "image/jpeg"
            },
            "destination": map {
                "max-width": 1000,
                "max-height": 1000,
                "rendering-hints": map {
                    $image:interpolation: $image:interpolation_bicubic
                }
            }
        }
  )
  return
    xmldb:store-as-binary("/db/image-scale-test", "h+p_scale_1000_bicubic.jpeg", $scaled-img)
};

declare
    %test:assertEquals("/db/image-scale-test/h+p_scale_200_bilinear.jpeg")
function ist:scale-down-bilinear() {
  let $source-img := util:binary-doc("/db/image-scale-test/h+p.jpeg")
  let $scaled-img := image:scale(
        $source-img,
        map {
            "source": map {
                "media-type": "image/jpeg"
            },
            "destination": map {
                "max-width": 200,
                "max-height": 200,
                "rendering-hints": map {
                    $image:interpolation: $image:interpolation_bilinear
                }
            }
        }
  )
  return
    xmldb:store-as-binary("/db/image-scale-test", "h+p_scale_200_bilinear.jpeg", $scaled-img)
};

declare
    %test:assertEquals("/db/image-scale-test/h+p_scale_1000_bilinear.jpeg")
function ist:scale-up-bilinear() {
  let $source-img := util:binary-doc("/db/image-scale-test/h+p.jpeg")
  let $scaled-img := image:scale(
        $source-img,
        map {
            "source": map {
                "media-type": "image/jpeg"
            },
            "destination": map {
                "max-width": 1000,
                "max-height": 1000,
                "rendering-hints": map {
                    $image:interpolation: $image:interpolation_bilinear
                }
            }
        }
  )
  return
    xmldb:store-as-binary("/db/image-scale-test", "h+p_scale_1000_bilinear.jpeg", $scaled-img)
};
