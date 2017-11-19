import CoreWLAN
import Foundation

final class Network {
  let macAddress: String
  let signalStrength: Int
  let age: Int = 0
  let channel: Int
  let noiseMeasurement: Int

  init(macAddress: String, signalStrength: Int, channel: Int, noiseMeasurement: Int) {
    self.macAddress       = macAddress
    self.signalStrength   = signalStrength
    self.channel          = channel
    self.noiseMeasurement = noiseMeasurement
  }

  func asDictionary() -> [String : Any] {
    return ["mac_address" : macAddress,
            "signal_strength" : signalStrength,
            "age" : age,
            "channel" : channel,
            "signal_to_noise_ratio" : noiseMeasurement]
  }

  static func fromCWNetwork(network: CWNetwork) -> Network {
    return Network(macAddress: network.bssid!, signalStrength: network.rssiValue,
      channel: network.wlanChannel.channelNumber, noiseMeasurement: network.noiseMeasurement) 
  }
}
