steam-toolkit
=============

[![Build Status](https://travis-ci.org/SourceUtils/hl2-hud-editor.svg?branch=master)](https://travis-ci.org/SourceUtils/hl2-hud-editor)

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
