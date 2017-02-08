import PackageDescription

let package = Package(
    name: "network-scanner",
    dependencies: [
      .Package(url: "https://github.com/httpswift/swifter.git", majorVersion: 1)]
)
