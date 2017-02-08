import Foundation
import Swifter

let server = HttpServer()
server["/latlong"] = { handleLatLong(req: $0) }
try server.start(6001)
RunLoop.main.run()
