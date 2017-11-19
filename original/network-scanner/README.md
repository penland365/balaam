balaam-network-scanner
=====
network-scanner is a small web-server that serves one endpoint `GET /wifis`.
The wifis are used by Google in order to determine you latitude / longitude for retrieving weather.

### Building
Requires Swift 3, only Mac OSX compatible at the moment. From this folder
```shell
$ swift build
$ ./.build/debug/network-scanner
```

### Launchctl file
Below is an example of a launchtl file in order to daemonize this process on a Mac OSX. `codes.balaam.networkscanner.plist` is assumed to be the file shown below.
```shell
$ cd ~/Library/LaunchAgents/
$ launchctl start codes..networkscanner.plist
```
Once you reboot, the server will be started automatically.
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
	<key>EnvironmentVariables</key>
	<dict>
		<key>PATH</key>
		<string>/usr/local/bin/</string>
	</dict>
	<key>KeepAlive</key>
	<dict>
		<key>SuccessfulExit</key>
		<false/>
	</dict>
	<key>Label</key>
	<string>codes.balaam.networkscanner</string>
	<key>ProgramArguments</key>
	<array>
      <string>/usr/local/bin/network-scanner</string>
	</array>
	<key>StandardErrorPath</key>
	<string>/var/log/balaamerr.log</string>
	<key>StandardOutPath</key>
	<string>/var/log/balaamout.log</string>
</dict>
</plist>
```
