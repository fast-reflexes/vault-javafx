# Vault - registration, subscription and password organiser

Rewritten with only JavaFX instead of the initial project which used TornadoFX which is now deprecated. Most core 
functionality has almost not been touched (e.g. crypto classes).

Current versions used are JavaFX 24.0.2 and Java 23 with Gradle 8.14.3 and Kotlin plugin 2.2.0.

## Documentation:

Vault is an offline desktop application used to manage memberships of different kind, be it subscriptions, usernames, 
codes and passwords or any other similar use cases. To do this, it uses the concept of **associations** and 
**credentials**. 

### Associations

An association is a main identifier (e.g. name or reference) associated with an amount of secret and non-secret data.
An association has a main identifier and an arbitrary number of secondary identifiers. The main identifier is the principal
name of the association and can be the name of a website or a service or anything else. The secondary identifiers are 
alternative names that an association may be found under, for example the same password is used for both `Amazon.com` and 
`Prime Video`, then`Amazon.com` might be the main identifier and `Prime video` may be a secondary identifier 
(or vice versa). The main identifier must be unique whereas alternative identifiers may overlap.

An association also may belong to a category and have a comment attached to it. Three flags indicate whether the
association is currently needed, whether it should be deactivated or whether it already has been deactivated (unused
accounts might need to be closed but it might be a long procedure with the service in question).

The very core of the association is its credentials.

### Credentials

An association may have one or many credentials attached to it. A credential is a password with one or several usernames
connected to it. When logging in to Vault, the credential data is never stored in memory in unencrypted form
except for when a particular credential is being edited.

* Använder Base64 för allt som har med krypto att göra, använder UTF8 för att läsa och skriva till fil (eftersom jag
  har radbrytningar där så Base64 räcker ej) samt UTF8 i den okrypterade versionen av valvet (när den krypteras blir den
  till Base64).
* Tänkte använda GCM för att kryptera men insåg att detta ej var bra då GCM är mer känsligt än CBC. Med GCM hade jag
  behövt hålla reda på vilken IV som användes varje gång och det ger ett probem, nämligen att en attackerare kan spara en
  gammal fil (med en tidigare IV) och låta en användare använda den igen (varvid 2 olika krypteringar med samma IV / Nonce
  kan erhållas vilket breakar hela confifentialityn. Dessutom skulle jag även MED GCM beöva köra en HMAC på hela den
  sparade fieln för at se att inte anat har förändrats.. Dena mac innehåller även ciphertexten vilket ger en
  Encrypt-then-MAC-skydd till CBCn... Detta är tillräckligt varför den extra autenticering som GCM bringar till tablen
  är onödig. Man hade kunnat använda en randomiserad IV för GCM (är ok med 128 bitar) men återigen finns det ingen vinst
  med det.. GCM är fett men i detta fall är CBC bättre).

### Setup

Vault uses two different locations; the location of the system settings and the location of profiles. The latter is set
manually when you start Vault for the first time. The former is set to the current directory if the env var
`IS_DEVELOPMENT` is set to `true`, otherwise it uses os information to determine what path to use.

### Usage

When you start Vault for the first time, you must select where to store your profiles. Once this choice has been made,
you may create a profile and log into Vault with that profile. The selection that you made about where to store profiles
is stored in a file called `vault.settings` which is stored in the same directory that you run Vault from. If you 
encounter problems with Vault regarding where profiles are stored, you may delete this file which will cause a prompt
about this setting the next time you start vault. The profiles themselves will not be deleted in this process but if you 
change the directory where they are stored, you might need to move them from where they initially were. You may also 
change this directory when logged in (might also require you to manually move the profile if you change directory).

After registering with a password, you may log in to Vault with the same password. This password is used to encrypt all 
the other data that you store in Vault. Therefore, the data may only be decrypted by you. When logged in, you may add 
and remove associations and credentials as you wish. When doing some operations you may have to enter your vault password
anew. This includes operations which are extra sensitive (exporting your vault or persisting it to disk) or that require
access to encrypted data (see `Encrypted data changes in memory` further down). For the former case, you must always
enter the password but for the latter, there is a parameter that controls for how much time an entered vault password may
be saved in memory. During this time and for these types of operations, Vault will not ask you to reenter the password
but will instead reuse the password stored in memory. Each time you do such an operation, the timeout when Vault
forgets your password will be extended. Once the timeout expires, Vault will remove your entered password from memory
and you must enter it anew the next time you wish to perform an operation where the password is needed. You may 
configure this timeout in the `Profile settings` where you may also manage categories and your default password length.

When adding a new credential, Vault offers you the opportunity to create the new password for you. Your default password
length (configurable in `Profile settings`) is used as initial value for this setting but it may be adjusted in the dialog.
You may also use the `String generator` to generate arbitrary random strings for external use.

The vault can be exported to a cleartext text file in case you want to store your password in some safe location
other than Vault. As said, this operation always requires your password and storing passwords in cleartext obviously
implies a certain risk which you must be aware of.

## Run

To run during development, execute `./gradlew run`

To build a thin jar, execute `./gradlew jar`

To build a fat jar, execute `./gradlew fatJar`

To run fat jar, execute `java -jar <JAR>>`

An error is displayed on started due to that this project does not use Java modules and thus, this project is an unnamed
module. This can be ignored.

## Build

Build on a Mac computer with Java 23 installed:

jpackage --input build/ --name Vault --main-jar libs/vault-javafx-1.0-fat.jar --main-class com.lousseief.vault.MainKt --type dmg

For Windows, build on a Windows computer with

jpackage --input build/ --name Vault --main-jar libs/vault-javafx-1.0-fat.jar --main-class com.lousseief.vault.MainKt --type exe

## Technical documentation

### Domain classes

Domain classes that are not prefixed with `Ui` are the "regular" domain classes that are general Kotlin / Java objects
and which map 1:1 to the actual data that is persisted. Corresponding classes prefixed with `Ui` are ui versions of the
same classes where most properties are replaced with JavaFX property observables and sometimes data is duplicated in
multiple forms for convenience.

### Format and function

A `.vault` file contains 6 rows of data: `keyMaterialSalt`, `verificationSalt`, `verificationHash`, `iv`, 
`encryptedData` and `checkSum`.

#### Step 1: deriving the password and verifying it

When we login, the password is used as input to a `PBKDF2` password derivation or password hash function. This function 
uses `HMAC` with `SHA512` to derive a password (or a hash if it's not used as a password). The output is variable but
should not exceed the output size of the used hash so since we use `SHA512`, we take 512 bits of outputs from this
`PBKDF2` function. The difference between a hash and a password derivation function is that the latter uses a salt
as partial input (to remedy the use of rainbow tables) and also runs the HMAC a large number of times for it to take
a lot longer than a regular hash.

The 512-bit output from this step is then run AGAIN through the `PBKDF` function with a new salt (`verificationSalt`). 
This time, we save the result (`verificationHash`) as a hash of the generated key from the previous step. This way, 
we can easily verify whether an entered password is correct or not.

The final part in this step is to divide the key generated in the first iteration into two parts consisting of 256
bits each. The first part is used as encryption key and the second part is used as a hmac key.

#### Step 2: verifying the vault file and decrypt data

In this step we use the hmac key (256 bits) from the previous step and we run the entire vault file and the hmac key
through `HMAC` with `SHA256`. The output is the `checkSum` above and when we access the vault, we verify that the 
calculated checksum is the same as the stored one.

The encryption key derived in step 1 is then used along with an initialization vector (`iv` above) to decrypt the
vault's encrypted data containing the actual data of the vault. The encryption algorithm used here is `AES256` in
`CBC` mode.

#### Summary
Vault reads from an encrypted file and uses the user's password to derive an encryption key used to decrypt and encrypt
the data in this file. When the vault is open and the program is used to manipulate data, the actual credentials are not
in memory, instead, they are kept in the encrypted string in memory. When credentials are accessed, one must enter the
password if a certain configurable time has passed. When the credentials view is exited, the credentials are
directly saved to the encrypted data in memory. To save it to disk, one must also save the entire vault (button to the
lower right). Other data updated in the vault will also only be saved upon saving the entire vault.

### Data states during usage

There are three levels of persistence in Vault:

* Data structure changes in memory
* Encrypted data changes in memory (secret data is not available in data structures in memory during usage)
* Vault file changes

Different usage patterns trigger different of these changes and ultimately, one must save the vault to disk to persist
it across sessions.

#### Data structure changes in memory
 
Changes to data structures in memory are triggered by:

* Updating profile settings (add or remove categories, changing default password length, changing vault opening time)
* Updating existing associations (main identifier and credentials not included)

#### Encrypted data changes in memory

Changes to encrypted data in memory are triggered by:

* Changing vault master password
* Adding, removing or updating credentials
* Adding or removing associations or updating the main identifier of an association

#### Vault file changes

Vault file changes are only triggered by one action:

* Clicking `Save to vault to disk` buttons

#### Summary

The encrypted data in memory exits because we don't want to store the data in plaintext in memory. However, we don't 
want to reencrypt that data all the time when small changes are made to associations. Therefore we only do this for
select cases. The association data in memory is both a map from string (main identifier) to association as well as
an ordered list of associations (ordered by main identifier). The ordered list must be manually updated whenever
we need to add or remove associations from the map (e.g. this does not trigger an update of the ordered list) but
the map and the list contain the same association objects.

When associations are added or removed or when a main identifier is updated, the map in memory is updated. The encrypted 
data is updated as well on these occasions. When we manipulate credentials, we manipulate the encrypted data stored in 
memory as well. So if a vault has been updated in some ways, the current encrypted data thus reflects everything from 
none to all of these changes, typically only a few of them.

To be able to determine if a user has saved the current state of the vault or not, we also need a way of comparing the
current state with the state of the persisted data. This is done by, upon login and when we persist to disk, saving 
data from the persisted file. We can then easily at any time compare the current state and say if it's the same as
the persisted one or not. To avoid randomness altering data which is not in essence different, we use the same IV
(initialization vector) when updating the encrypted data in session but when we persist it to disk, we make sure to
use a new IV.

All in all, the cleartext data structures in memory reflect the current state of the vault. Some data are not in these
structures and they reside in the encrypted data stored in memory. The encrypted data can be decrypted and read from
when needed. The encrypted data is also updated in-session in select cases. Upon login and persisting data to file,
the structures from the file are stored in such a way so that we may compare the current state of the vault to the 
persisted state at any time to determine whether the user needs to save or not. The encrypted data in memory is thus
a partially updated vault with a state between the in-memory cleartext data structures ans the persisted state on file.
The file holds the stored state of the vault.

## Knowledge
		
### JavaFX

* Set bindings and event listeners in `Platform.runLater {}` in `initialize` instead of directly in `initialize`. In
some cases, timing issues related to FXML bindings between controller and FXML may cause bindings and event listeners
not to work as they should. This seems to happen more often when attaching sub views and similar as opposed to when ¨
setting a view directly on the root element of a window.

### Icons

This project uses icons from a series of different libraries:
* Fontawesome (https://fontawesome.com/v3/icons/, https://fontawesome.com/v4/icons/)
* Material Icons
* Material Design Font (https://pictogrammers.com/library/mdi/)

Even more icons are available. To see icons online, check the build file and extract the version of the icon library used 
and search for that, for example Font Awesome 4.7.0

### JavaFX CSS

* You can't target elements
* Classes are added with `styleClass` attribute in FXML and may be found in the global css file or a local one
* Some attributes are not the same as the ones used in HTML, for example, the button color is set with `-fx-color` 
instead and one should NOT use `-fx-background-color` because then focus behaviours are messed up as well.

### Buttons

Buttons may be **default** or **cancel** buttons (or none). A default button is blueish and is clicked when hitting enter
and a cancel button has no special appearance but is clicked when hitting the escape button. This applies if no other
button tries to do the same in the same scene. Note that a third button can be the focused button, e.g. that is not the
same as being a default button. Normally, button types which are `OK` or `OK_DONE` are default buttons and `CANCEL` or
`CANCEL_CLOSE` are cancel buttons. This can also be set manually in the code.

### Observable properties

Remember that observables support LAZY evaluation and if so, they will not trigger reevaluation as expected. For example,
if an observable is added as a dependency to a binding, the evaluation of the binding will NOT necessarily be retriggered
just because the observable changes value. The only way an observable becomes eaglery evaluated is by adding a change 
listener.

### Miscellanous

* SceneBuilder is a GUI with which you can build JavaFX applications using drag and drop.
* Don't nest dialogs, it's SUPPOSED to work but it's better to chain dialogs instead, closing the first before opening 
the second and I HAVE experienced that the application hangs without any error message. when nesting dialogs.

### Questions

None currently
  
### Questions and answers

#### Hur funkar konversion till 7-bit ASCII tex? Vad görs med den sista biten?
ALLA bytearrayer som omvandlas till en sträng genom new String(bytes, Charset) har automatiskt en replacement character 
för bytes som inte finns i deras representation. Vill man ha mer kontroll över detta så får man använda en 
CharacterEncoder. Detta innebär att i krypteringsavseende är det bra om man ser till att det Charset man använder kan 
hantera allt tänkbart bytesinput som man skickar in, eftersom annars begränsas säkerheten genom att OLIKA bytes mappas 
till SAMMA karaktär vilket är ofördelaktigt. Kom också ihåg att även tecken som inte SYNS faktiskt ÄR tecken och lagras 
som sådana i en sträng. Ska strängen användas för I7O av en människa finns det dock såklart en begränsning här.

## TODO

### Backlog:

***NOTHING atm*** 

* Make sure that references to the old scene is lost how to?
* Bygg i Windows också

### Inbox (to do MAYBE at some later point)
* When you add the password, also add it with asterisks except if a checkbox is filled indicating clear text (like when passwords are shown)
* Add feature to see stats (or use a different word), for example list usernames and how often they are used
* Implement coloring of entries that are changed and unsaved in the entries list and also color fields that are changed.
* Remove default Java menu (and content) from the application
* Use Java modules and add module-info.java to perhaps get rid of error when starting with jar
* Fixa ny ikon till jaren
* Switch to red buttons in some dialogs where you confirm delete are confirmed
* Add logging via some central utility and frequently used logger library (dependency) and remove println's
* Perhaps use last updated flag in the association as well (not only in credential?)
* How is translations to ASCII done if wrong bytes are used? Is it sure? (not sure what this question is about?)
* Unify the terminology for the parameter used to keep the vault open, perhaps the term vaultOpeningTimeMinutes can be
used?
* Out everything in Platform.runLater instead of initialize that concerns bindings and listeners (especially if we see odd
timing issues, however, we must only add layout-related code in runLater, not heavy other stuff)
