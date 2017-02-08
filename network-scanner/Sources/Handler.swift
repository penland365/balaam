import CoreWLAN
import Foundation
import Swifter

final class LatitudeLongitudeHandler {

  let cache: ExpiryCache

  init() {
    self.cache = ExpiryCache(timeIntervalInSeconds: 30)
  }

  func handle(req: HttpRequest) -> HttpResponse {
    if(cache.get().count != 0) {
      return .ok(.json(cache.get()))
    } else {
      let networks = scan() 
      let arr = NSMutableArray()
      for network in networks {
        arr.add(network.asDictionary())
      }
      cache.put(value: arr)
      return .ok(.json(arr))
    }
  }

  func scan() -> [Network] {
    let interface = CWWiFiClient.shared().interface()
    var networks: Set<CWNetwork> = []
    var xs: [Network] = []
    do {
      networks = try interface?.scanForNetworks(withName: nil) ?? []
      xs = networks.map { Network.fromCWNetwork(network: $0) }
    } catch {}
  
    return xs
  }
}
