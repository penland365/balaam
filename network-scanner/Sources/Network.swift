import CoreWLAN
import Foundation

final class Network {
  let macAddress: String
  let signalStrength: Int
  let channel: String
  let age: Int = 0

  init(macAddress: String, signalStrength: Int, channel: String) {
    self.macAddress = macAddress
    self.signalStrength = signalStrength
    self.channel = channel
  }

  func asDictionary() -> [String : Any] {
    return ["mac_address" : macAddress,
            "signal_strength" : signalStrength,
            "channel" : channel,
            "age" : age]
  }

  static func fromCWNetwork(network: CWNetwork) -> Network {
    return Network(macAddress: network.bssid!, signalStrength: network.rssiValue,
      channel: network.ssid!)
  }
}
