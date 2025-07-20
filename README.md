# Vault - registration, subscription and password organiser

Rewritten with only JavaFX instead of the initial project which used TornadoFX which is now deprecated.

Current versions used are JavaFX 24.0.2 and Java 23 with Gradle 8.14.3 and Kotlin plugin 2.2.0.

## Base functionality

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

## Dokumentation:

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

## Kunskap
		
### Bra att veta om JavaFX

* Sätt bindings och event listeners i `Platform.runLater {}` i `initialize` istället för direkt i `initialize`. I vissa
fall kan timing issues rörande bindings och listeners göra att saker och ting inte funkar som det ska. Detta verkar hända
oftare då man ansluter subvyer och sånt snarare än då man sätter en vy direkt i rotelementet.

### Icons
This project uses icons from a series of different libraries:
* Fontawesome
* Material Icons
* Material Design Font

Even more icons are available. To see icons online, check the build file and extract the version of the icon library used 
and search for that, for example Font Awesome 4.7.0

### JavaFX CSS

* You can't target elements
* Classes are added with `styleClass` attribute in FXML and may be found in the global css file or a local one
* Some attributes are not the same as the ones used in HTML, for example, the button color is set with `-fx-color` 
instead and one should NOT use `-fx-background-color` because then focus behaviours are messed up as well.

### Frågor:
  
### Frågor och svar

#### Hur funkar konversion till 7-bit ASCII tex? Vad görs med den sista biten?
ALLA bytearrayer som omvandlas till en sträng genom new String(bytes, Charset) har automatiskt en replacement character 
för bytes som inte finns i deras representation. Vill man ha mer kontroll över detta så får man använda en 
CharacterEncoder. Detta innebär att i krypteringsavseende är det bra om man ser till att det Charset man använder kan 
hantera allt tänkbart bytesinput som man skickar in, eftersom annars begränsas säkerheten genom att OLIKA bytes mappas 
till SAMMA karaktär vilket är ofördelaktigt. Kom också ihåg att även tecken som inte SYNS faktiskt ÄR tecken och lagras 
som sådana i en sträng. Ska strängen användas för I7O av en människa finns det dock såklart en begränsning här.

## TODO

### Backlog:
* Färdig settings
* Efterforska NULL ITEM i entriesList (när man väljer)

* Fixa så att man får en property som avgör om man behöver spara eller ej och styr så att man inte kan stänga vissa saker utan varning
* Only save credential if something is changed
* skriv klart filterfunktionenr
  * kolla att regex funkar.. verkar inte som det just nu
* Sätt allt inom Platform.runLater i initialize som rör bindings och listeners
* Samma ska fortsätta vara selected även om man filtrerar, helst ska man selecta null om den som var selectad inte lägre är kvar
* Add a way to point out where vaultfiles are stored (settings)
* hantera last updated i credentials (och i entry?)
* kolla att stängning med kryss och cancel ger samma resultat ( påmminner när något är osparat etc...)
* fixa java-menyn uppe i hörnet
* ändra button text på en del ställen till "Quit anyways" eller "Cose anyways" istället för "Ok"
* kolla beteende med när man byter entry och stänger utan att ha sparat å så så att de varningar jag vill ha upp kommer upp som de ska
* hur görs översättning till ascii om fel bytes används? Säkert
* gå igenom allt och om jag verkligen gör saker i rätt ordning
* How to deal with errors? e.g-. when a mainidentifiers already exists?
* Fix delete entry
* Write in readme about how this software is supposed to be used
* Factor out colors into constants and use them as much as possible
* Write in readme about WHEN and HOW things are saved and make sure this is correct

### Inbox
* When you add the password, also add it with asterisks except if a checkbox is filled indicating clear text (like when passwords are shown)
* Add feature to see stats (or use a different word), for example list usernames and how often they are used
* Implement coloring of entries that are changed and unsaved in the entries list and also color fields that are changed.
