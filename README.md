# XML Calabash support for printing with XSL FO and CSS

[![Build Status](https://travis-ci.org/ndw/xmlcalabash1-print.svg?branch=master)](https://travis-ci.org/ndw/xmlcalabash1-print.svg?branch=master)

This repository contains
[XML Calabash](http://github.com/ndw/xmlcalabash1) 1.1 support for
printing with
[XSL FO](http://www.w3.org/standards/techs/xsl#w3c_all) or
[CSS](http://www.w3.org/Style/CSS/).

Support is included for XSL FO processing with
[FOP](https://xmlgraphics.apache.org/fop/),
[Antenna House](https://www.antennahouse.com/xsl-specification),
or
[RenderX](http://www.renderx.com/)
and CSS processing with
[Antenna House](http://www.antennahouse.com/css/)
or
[PrinceXML](http://www.princexml.com/).

Printing with XML Calabash requires these libraries in addition to any
commercial libraries required. (No additional libraries are required
for FOP.)

This step should work with either Saxon 9.5 or Saxon 9.6.

## Installation

For standalone installation, get the most recent release from the project
[releases](http://github.com/ndw/xmlcalabash1-print/releases) page.
The release distributed there includes
relevant dependencies. Unpack it somewhere on your system and add the
included, top-level `jar` file to your class path.

If you're using Maven, you can also get it from there. Note, however, that
the Maven distribution includes a POM file that identifies other dependencies
that must also be downloaded. You'll need them too, which happens naturally
if you're including the Maven dependency in some other Maven project.
If you just grab the `jar` from Maven and don't get the other dependencies,
you're likely to find that the step doesn't work because of some missing
dependencies.

