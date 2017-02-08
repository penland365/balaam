import CoreWLAN
import Swifter

func scan() -> [Network] {
  let interface = CWWiFiClient.shared().interface()
  var networks: Set<CWNetwork> = []
  var xs: [Network] = []
  do {
    networks = try interface?.scanForNetworks(withName: nil) ?? []
    xs = networks.map { Network.fromCWNetwork(network: $0) }
    //let ys = xs.map { $0.asDictionary() }
    //let all = try JSONSerialization.data(withJSONObject: ys, options: [])
    //let string = String(bytes: all, encoding: .utf8)!
    //print(string)
  } catch {}

  return xs
}

func scanny() -> NSArray {
  let n = Network(macAddress: "macy", signalStrength: 71, channel: "channel").asDictionary()
  let xs = NSMutableArray()
  xs.add(n)
  return xs 
}

func handleLatLong(req: HttpRequest) -> HttpResponse {
  let networks = scan() 
  let arr = NSMutableArray()
  for network in networks {
    arr.add(network.asDictionary())
  }

  return .ok(.json(arr))
}
