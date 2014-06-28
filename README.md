steam-toolkit
=============

For working with the same data Steam relies on

VDF example:

```
VDFNode root = VDF.load(InputStream);
for(VDFNode node : root.getNodes()) {
    String nodeName = (String) node.getCustom(); // The name of this node
    String keyValue = (String) node.getValue("key", "placeholder"); // The value of a key, or "placeholder"
}
```
