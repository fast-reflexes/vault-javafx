package com.lousseief.vault.model

typealias VaultData = Map<String, AssociationWithCredentials>
typealias MutableVaultData = MutableMap<String, AssociationWithCredentials>
typealias MutableVault = Pair<Settings, MutableVaultData>
typealias Vault = Pair<Settings, VaultData>
