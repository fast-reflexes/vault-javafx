
## v. 1.1
* Add version label
* Show current location of profiles when trying to update it
* Sort associations without respect to casing

### v. 1.1.1
* Add app icons
* Bug documentation on double-click on close buttons
* Update all packages
* Improve build scripts
### v. 1.1.2
* Fix bug where selection on association was lost when saving the first association for a user.
* Fix so filters are always active and search input always seen

## v. 1.2 - security audit
* Updated file permissions for vault files, settings' file and exported file (rw-------) including migration
* Copies in clipboard deleted after X seconds and clipboards are enlightened about content via mime types
* Usernames restricted to lowercase, numbers, underscore and hyphen
* Added logging utility (activiated via IS_DEVELOPMENT=true and DEBUG=true)
* Other minor fixes

## v. 2.0 - security audit continued
* Updated wrongful use of `bytesToUTF8` which lost entropy with `bytesToBase64` instead (vaults migrated before final change)
* Updated adjustment of wrongful permissions on Vault files
* Improve support for Windows file permissions

## v. 2.0.1 - security audit continued
* Protect against in-memory attacks by scrapping stored passwords as often as possible
* Tighten interval during which the vault password does not need to be reiterated for continued access
