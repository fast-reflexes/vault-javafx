# Vault - registration, subscription and password organiser

Rewritten with only JavaFX instead of the initial project which used TornadoFX which is now deprecated.

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
  Encryot-then-MAC-skydd till CBCn... Detta är tillräckligt varför den extra autenticering som GCM bringar till tablen
  är onödig. an hade kunnat använda en randomiserad IV för GCM (är ok med 128 bitar) men återigen finns det ingen vinst
  med det.. GCM är fett men i detta fall är CBC bättre).

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

## Technical documentation

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
* Updating existing associations (credentials not included)

#### Encrypted data changes in memory

Changes to encrypted data in memory are triggered by:

* Changing vault master password
* Adding, removing or updating credentials
* Adding or removing an association

#### Vault file changes

Vault file changes are only triggered by one action:

* Clicking `Save to vault to disk` buttons

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
* Investigate odd error with manual flag in UiProfile and restore to previous state
* Think about if sorting of orderedAssociations should be done in listener instead
* Lägg in fix för att ändra iterations
* Lägg in fix och koll för när man sparar och sen tar bort något (credentials ska bli som innan man lagt till)
* Lägg in så att när man sparar så ersätts profile med en ny och flaggor resettas

* Frågar fortfarnade om ok att stänga fönster på loginrutan när man gjort "CLose anyway" utan att spara
* Fixa så att man får en property som avgör om man behöver spara eller ej och styr så att man inte kan stänga vissa saker utan varning gäller huvudfönstret främst
* kolla att stängning med kryss och cancel ger samma resultat ( påmminner när något är osparat etc...)
* How to deal with errors? e.g-. when a mainidentifiers already exists?
* Fix delete entry
* Testa hur det blir när man byter mainidentifier på ett entry och sen sparar och sen gör vissa saker igen.. hmmm
* Sortera om entries om man döper om main identifier.. eller förbjud att döpa om hmmm
* Hur ändra iterations i PBKDF?

* Write in readme about how this software is supposed to be used
* Write in readme about WHEN and HOW things are saved and make sure this is correct
* Kolla igenom att allt är likadant överallt och så och att vi har kontroll på alla lägen
* Kolla på gamla koden som inspiration för om jag glömt ngt
* gå igenom allt och om jag verkligen gör saker i rätt ordning

* Sätt allt inom Platform.runLater i initialize som rör bindings och listeners
* Samma ska fortsätta vara selected även om man filtrerar, helst ska man selecta null om den som var selectad inte lägre är kvar
* hantera last updated i credentials (och i entry?)
* ändra button text på en del ställen till "Quit anyways" eller "Cose anyways" istället för "Ok"
* hur görs översättning till ascii om fel bytes används? Säkert
* Lägg till pointer cursor på alla knappar

### Inbox (to do MAYBE at some later point)
* When you add the password, also add it with asterisks except if a checkbox is filled indicating clear text (like when passwords are shown)
* Add feature to see stats (or use a different word), for example list usernames and how often they are used
* Implement coloring of entries that are changed and unsaved in the entries list and also color fields that are changed.
* Remove default Java menu (and content) from the application
* Use Java modules and add module-info.java to perhaps get rid of error when starting with jar
* Fixa ny ikon till jaren
* Röda knappar i en del dialoger där man ska bekräfta delete

