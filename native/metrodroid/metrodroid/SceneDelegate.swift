//
//  SceneDelegate.swift
//
// Copyright 2026 Google
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.
//
//

import UIKit

class SceneDelegate: UIResponder, UIWindowSceneDelegate {

    var window: UIWindow?

    func scene(_ scene: UIScene, willConnectTo session: UISceneSession, options connectionOptions: UIScene.ConnectionOptions) {
        guard connectionOptions.urlContexts.isEmpty == false else {
            return
        }

        DispatchQueue.main.async { [weak self] in
            self?.importFirstURL(from: connectionOptions.urlContexts)
        }
    }

    func scene(_ scene: UIScene, openURLContexts URLContexts: Set<UIOpenURLContext>) {
        importFirstURL(from: URLContexts)
    }

    private func importFirstURL(from urlContexts: Set<UIOpenURLContext>) {
        guard let url = urlContexts.first?.url,
              let navigationController = window?.rootViewController as? UINavigationController else {
            return
        }

        HistoryViewController.importFile(navigationController: navigationController, from: url)
    }
}
