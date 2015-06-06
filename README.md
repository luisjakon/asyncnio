# AsyncNio
NIO-powered Asynchronous Socket Channels for Java

<b>About</b>
 - AsyncNio is a JDK7+ compatible ```java.nio.channels.AsynchronousSocketChannels``` implementation.
 - Powered by the venerable ```java.nio.channels.SocketChannels``` package family.
 - Known to work on JDK6 and above with throughput almost on par with native JDK7+ implementations.

<b>Notable Features</b>
 - Implements an event-based reactive core with multiple managed dispatchers.
 - Implements automatic thread-pooling & executor service management for default groups and channels.
 - Provides tunable settings for timeouts, workerthread counts, and other performance-tuning parameters
 - Contains experimental support for ```zero-copy``` transfers. <b>(New feature**)</b>

<b>Notes</b><br><b>**</b> AFAIK, asynchronous ```zero-copy``` transfers are not part of the JDK NIO.2 proposal, but will hopefully be added to the standard ```java.nio.channels``` package sometime in the future.<br><br> In the meantime, they are hereby included for you to enjoy!

# Contributing
Pull requests, contributions, issue reporting and feedback are welcome and encouraged.
