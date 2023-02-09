//
//  StringExtension.swift
//  iosApp
//
//  Created by Jan Cortiel on 06.02.23.
//  Copyright © 2023 orgName. All rights reserved.
//

import Foundation

extension String {
    static func localizedString(forKey key: String, inTable table: String?, withComment comment: String) -> String {
        var result = NSLocalizedString(key, tableName: table, comment: comment)
        if result == key {
            result = NSLocalizedString(key, tableName: table, comment: comment)
        }
        return result
    }
}
