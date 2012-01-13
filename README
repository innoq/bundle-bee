:README: v${pom.version}

Bundle-Bee allows you to turn multiple OSGi framework instances into a grid, in which service calls are distributed across VM boundaries.
This distribution contains a complete Equinox framework instance, enhanced with Bundle-Bee.


::Requirements::

Java 5
Multicast-capable network
Either no firewall or one with the following open ports: UDP 5555 (multicast), UDP 5556 (unicast), TCP 48110


::Configuration::

To configure the Bundle-Bee components in the Equinox OSGi framework, edit the file plugins/config.ini.
Most available settings are described in comments.

To make methods of services in your own bundles available to the grid, you have to tell Bundle-Bee to instrument them.
To do so you, you will have to specify the system property org.bundlebee.weaver.instrumentedmethods.
Valid values are a semicolon separated list of regular expressions that match method signatures.

Methods are specified without parameter names or return type, like this:

org.bundlebee.testbundle.impl.TestBundleImpl.someMethod(int,java.lang.String,byte[])


Due to escaping rules, the corresponding regular expression looks like this:

org\\.bundlebee\\.testbundle\\.impl\\.TestBundleImpl\\.someMethod\\(int,java\\.lang\\.String,byte\\[\\]\\)


To match all methods in org.bundlebee.testbundle.impl.TestBundleImpl, use this regular expression:

org.bundlebee.weaver.instrumentedmethods=org\\.bundlebee\\.testbundle\\.impl\\.TestBundleImpl\\..*


To add your own bundle, just place it into the plugins folder and add a corresponding entry to the osgi.bundles setting.


::Usage::

Depending on your OS, start the framework with

run.bat|run.sh|ant -f run.xml

This will open the Equinox OSGi console, which lets you install, start, stop and do other useful things to bundles.

You will also be able to use the the Bundle-Bee command line interface (CLI), which lets you find out information about the
grid and specific nodes. It addition, it lets you install, start, stop etc. bundles in the grid.
To find out about the available commands, simply type 'help'.

E.g. type:

bbecho <some message>

The message will be shown on an available node.