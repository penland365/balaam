import Foundation
import Swifter

let port: UInt16 = UInt16(CommandLine.arguments[0]) ?? 6001
let server = HttpServer()
let handler = LatitudeLongitudeHandler()
server["/latlong"] = { handler.handle(req: $0) }
try server.start(port)
print("starting network-scanner on port \(port)")
RunLoop.main.run()
