//
//  StringExtension.swift
//  iosApp
//
//  Created by Jan Cortiel on 06.02.23.
//  Copyright © 2023 Redlink GmbH. All rights reserved.
//

import Foundation
import CryptoKit

extension String {
    func localize(useTable table: String?, withComment comment: String) -> String {
        var result = NSLocalizedString(self, tableName: table, comment: comment)
        if result == self {
            result = NSLocalizedString(self, tableName: table, comment: comment)
        }
        return result
    }
    static func localizedString(forKey key: String, inTable table: String?, withComment comment: String) -> String {
        var result = NSLocalizedString(key, tableName: table, comment: comment)
        if result == key {
            result = NSLocalizedString(key, tableName: table, comment: comment)
        }
        return result
    }
    
    func toMD5() -> String {
        let digest = Insecure.MD5.hash(data: self.data(using: .utf8) ?? Data())
        return Data(digest).base64EncodedString()
    }
}
