import Foundation
import Swifter

let server = HttpServer()
let handler = LatitudeLongitudeHandler()
server["/latlong"] = { handler.handle(req: $0) }
try server.start(6001)
RunLoop.main.run()
