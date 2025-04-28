#
# Elemental
# Copyright (C) 2024, Evolved Binary Ltd
#
# admin@evolvedbinary.com
# https://www.evolvedbinary.com | https://www.elemental.xyz
#
# Use of this software is governed by the Business Source License 1.1
# included in the LICENSE file and at www.mariadb.com/bsl11.
#
# Change Date: 2028-04-27
#
# On the date above, in accordance with the Business Source License, use
# of this software will be governed by the Apache License, Version 2.0.
#
# Additional Use Grant: Production use of the Licensed Work for a permitted
# purpose. A Permitted Purpose is any purpose other than a Competing Use.
# A Competing Use means making the Software available to others in a commercial
# product or service that: substitutes for the Software; substitutes for any
# other product or service we offer using the Software that exists as of the
# date we make the Software available; or offers the same or substantially
# similar functionality as the Software.
#

format = 'UDZO'
filesystem = 'HFS+'
files = [
    ('${project.build.directory}/elemental-${project.version}.app', 'Elemental.app'),
]
symlinks = { 'Applications': '/Applications' }

icon = '${project.basedir}/src/dmg/VolumeIcon.icns'

icon_locations = {
	'Elemental.app': (100, 140),
	'Applications': (500, 140)
}
background = '${project.basedir}/src/dmg/background.png'

# Window position in ((x, y), (w, h)) format
window_rect = ((100, 100), (640, 300))

default_view = 'icon-view'
icon_size = 128
license = {
	'default-language': 'en_US',
	'licenses': {
		'en_US': '${project.basedir}/../LICENSE'
	}
}
