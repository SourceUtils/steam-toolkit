steam-toolkit
=============

[![Build Status](https://travis-ci.org/SourceUtils/steam-toolkit.svg?branch=master)]
(https://travis-ci.org/SourceUtils/steam-toolkit)

## DESCRIPTION

For working with the same data Steam relies on

VDF example:

``` java
VDFNode root = VDF.load(InputStream);
for (VDFNode node : root.getNodes()) {
    String nodeName = (String) node.getCustom(); // The name of this node
    String keyValue = (String) node.getValue("key", "placeholder"); // The value of a key, or "placeholder"
}
```
