import Foundation

final class ExpiryCache {
  let timeIntervalInSeconds: Double
  var expiration: Date?
  var value: NSArray


  init(timeIntervalInSeconds: Double) {
    self.value = [] 
    self.timeIntervalInSeconds = timeIntervalInSeconds
  }

  func put(value: NSArray) -> Void {
    expiration = Date(timeIntervalSinceNow: timeIntervalInSeconds)
    self.value = value
  }

  func get() -> NSArray {
    let now = Date()
    if(expiration == nil || now > expiration!) {
      self.value = []
    }
    return value
  }
}
