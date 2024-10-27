#
# Elemental
# Copyright (C) 2024, Evolved Binary Ltd
#
# admin@evolvedbinary.com
# https://www.evolvedbinary.com | https://www.elemental.xyz
#
# This library is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; version 2.1.
#
# This library is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public
# License along with this library; if not, write to the Free Software
# Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
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
