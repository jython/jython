package org.python.util.install;

import java.util.ListResourceBundle;

public class TextConstants_de extends ListResourceBundle implements TextKeys, UnicodeSequences {

    static final Object[][] contents = {
        // Die folgenden Texte duerfen Umlaute und Sonderzeichen enthalten, aber nur als Unicode Escape Sequenzen aus UnicodeSequences
        // The following texts may contain special characters, but only as unicode escape sequences from UnicodeSequences
        { ACCEPT, "Ja, ich akzeptiere" }, // license
        { ALL, "Alles (volle Installation, inklusive Quellcode)" }, // installation type
        { BROWSE, "Suchen..." }, // button (open the JFileChooser)
        { CANCEL, "Abbrechen" }, // button text
        { CHOOSE_LOCATION, "W"+a2+"hlen Sie das Verzeichnis, in das Jython installiert werden soll" }, // selection
        { CHOOSE_JRE, "Bestimmen Sie die Java Version (JRE/JDK), mit welcher Jython gestartet werden soll" }, // selection
        { CONFIRM_START, "Bitte dr"+u2+"cken Sie {0}, um die Installation zu starten" }, // overview
        { CONGRATULATIONS, "Gratulation!" }, // congratulations
        { CORE, "Kern" }, // installation type
        { CREATED_DIRECTORY, "Verzeichnis {0} wurde erstellt" }, // directory
        { CURRENT, "Das aktuelle" }, // directory
        { CUSTOM, "Benutzerdefiniert" }, // installation type
        { DEMOS_EXAMPLES, "Demos und Beispiele" }, // installation type
        { DIRECTORIES_ONLY, "Nur Verzeichnisse" }, // file chooser
        { DOCUMENTATION, "Dokumentation" }, // installation type
        { DO_NOT_ACCEPT, "Nein, ich akzeptiere nicht" }, // license
        { EMPTY_TARGET_DIRECTORY, "Das Zielverzeichnis darf nicht leer sein" }, // error
        { ENGLISH, "Englisch" }, // language
        { ENSUREPIP, "Installieren pip und setuptools"},
        { ERROR, "Fehler" }, // error
        { ERROR_ACCESS_JARFILE, "Problem beim Zugriff auf das jar File" }, // error
        { FINISH, "Beenden" }, // button
        { GENERATING_START_SCRIPTS, "Start Scripts werden generiert ..." }, // progress
        { GERMAN, "Deutsch" }, // language
        { INFLATING, "Entpacke {0}" }, // progress
        { INFORMATION, "Information" }, // information
        { INSTALLATION_CANCELLED, "Sie haben die Installation abgebrochen." }, // final
        { INSTALLATION_IN_PROGRESS, "Die Installation l"+a2+"uft" }, // progress
        { INSTALLATION_TYPE_DESCRIPTION, "Die folgenden Installationstypen sind verf"+u2+"gbar" }, // installation type
        { INSTALLATION_TYPE, "Installationstyp" }, // installation type
        { JAVA_INFO, "Java Hersteller / Version" }, // version
        { JAR_NOT_FOUND, "Jar File {0} nicht gefunden." }, // error
        { JYTHON_INSTALL, "Jython Installation" }, // title
        { LANGUAGE_PROPERTY, "Sprache" }, // language
        { LIBRARY_MODULES, "Bibliotheksmodule" }, // installation type
        { LICENSE, "Lizenzvereinbarung" }, // license
        { MAYBE_NOT_SUPPORTED, "Eventuell nicht unterst"+u2+"tzt" }, // version
        { MINIMUM, "Minimum (Kern)" }, // installation type
        { NEXT, "Weiter" }, // button
        { NON_EMPTY_TARGET_DIRECTORY, "Das Zielverzeichnis enth"+a2+"lt bereits Daten." }, // error
        { NO_MANIFEST, "Jar File {0} enth"+a2+"lt kein Manifest." }, // error
        { NOT_OK, "Nicht ok !" }, // version
        { OK, "Ok" }, // version
        { OS_INFO, "Betriebssystem / Version" }, // version
        { OTHER, "Ein abweichendes" }, // directory
        { OVERVIEW_DESCRIPTION, "Sie haben folgende Einstellungen f"+u2+"r die Installation ausgew"+a2+"hlt" }, // overview
        { OVERVIEW_TITLE, U2+"bersicht "+u2+"ber die gew"+a2+"hlten Einstellungen" }, // overview
        { PACKING_STANDALONE_JAR, "Das standalone " + JarInstaller.JYTHON_JAR + " File wird erstellt ..." }, // progress
        { PLEASE_ACCEPT_LICENSE, "Bitte lesen und akzeptieren Sie die Lizenzvereinbarung" }, // license
        { PLEASE_README, "Bitte lesen Sie die folgenden Informationen" }, // readme
        { PLEASE_READ_LICENSE, "Bitte lesen Sie die Lizenzvereinbarung sorf"+a2+"ltig durch" }, // license
        { PLEASE_WAIT, "Bitte um etwas Geduld, die Installation kann einige Sekunden dauern ..." }, // progress
        { PRESS_FINISH, "Bitte dr"+u2+"cken Sie {0}, um die Installation abzuschliessen." }, // finish
        { PREVIOUS, "Zur"+u2+"ck" }, // button
        { PROGRESS, "Fortschritt" }, // progress
        { README, "README" }, // readme
        { SELECT, "Ausw"+a2+"hlen" }, // button (approval in JFileChooser)
        { SELECT_INSTALLATION_TYPE, "Bitte w"+a2+"hlen Sie den Installationstyp" }, // installation type
        { SELECT_JAVA_HOME, "Bitte w"+a2+"hlen Sie das Java Home Verzeichnis" }, // directory
        { SELECT_LANGUAGE, "Bitte w"+a2+"hlen Sie Ihre Sprache" }, // language
        { SELECT_TARGET_DIRECTORY, "Bitte w"+a2+"hlen Sie das Zielverzeichnis" }, // directory
        { SOURCES, "Quellcode" }, // installation type
        { STANDARD, "Standard (Kern, Bibliotheksmodule, Demos, Beispiele, Dokumentation)" }, // installation type
        { STANDALONE, "Standalone (ein ausf"+u2+"hrbares .jar File)" }, // installation type
        { SUCCESS, "Sie haben Jython {0} erfolgreich im Verzeichnis {1} installiert." }, // final
        { TARGET_DIRECTORY_PROPERTY, "Zielverzeichnis" }, // property als Titel
        { TARGET_JAVA_HOME_PROPERTY, "Java Home Verzeichnis" }, // property als Titel
        { UNABLE_CREATE_DIRECTORY, "Fehler beim Erstellen von Verzeichnis {0}." }, // error
        { UNABLE_CREATE_FILE, "Fehler beim Erstellen von File {0}." }, // error
        { UNABLE_TO_DELETE, "Fehler beim L"+o2+"schen von {0}" }, // console
        { UNEXPECTED_URL, "Das Jar File f"+u2+"r die Installation weist eine unerwartete URL {0} auf." }, // error
        { VERSION_INFO, "Sie sind im Begriff, Jython Version {0} zu installieren." }, // version
        { WELCOME_TO_JYTHON, "Willkommen bei Jython !" }, // welcome
        { ZIP_ENTRY_SIZE, "Der Zip Eintrag {0} hat eine unbekannte Gr"+o2+"sse." }, // error
        { ZIP_ENTRY_TOO_BIG, "Der Zip Eintrag {0} ist zu gross." }, // error

        // Konsole Texte (beginnend mit C_) duerfen keine Umlaute und andere Sonderzeichen enthalten:
        // console texts (beginning with C_) must not contain special characters (use ASCII only):
        { C_ACCEPT, "Akzeptieren Sie die Lizenzvereinbarung ?" }, // license
        { C_ALL, "Alles (volle Installation, inklusive Quellcode)" }, // installation type
        { C_AT_ANY_TIME_CANCEL, "(Sie koennen die Installation jederzeit durch Eingabe von {0} abbrechen)" }, // console
        { C_AVAILABLE_LANGUAGES, "Die folgenden Sprachen sind fuer den Installationsvorgang verfuegbar: {0}" }, // languages
        { C_CHECK_JAVA_VERSION, "Ueberpruefung der Java Version ..." }, // progress
        { C_CLEAR_DIRECTORY, "Der Inhalt von Verzeichnis {0} wird anschliessend geloescht! Moechten Sie wirklich weiterfahren ?" }, //console
        { C_CONFIRM_TARGET, "Bitte bestaetigen Sie den Start des Kopiervorgangs ins Verzeichnis {0}" }, // console
        { C_CONGRATULATIONS, "Gratulation!" }, // congratulations
        { C_CREATE_DIRECTORY, "Das Verzeichnis {0} gibt es nicht - soll es erstellt werden ?" }, // console
        { C_ENTER_TARGET_DIRECTORY, "Bitte geben Sie das Zielverzeichnis ein" }, // console
        { C_ENTER_JAVA_HOME, "Bitte geben Sie das gewuenschte Java Home Verzeichnis ein (Enter fuer das aktuelle)" }, // console
        { C_ENGLISH, "Englisch" }, // language
        { C_ENSUREPIP, "pip und setuptools werden installiert"},
        { C_EXCLUDE, "Moechten Sie Teile von der Installation ausschliessen ?" }, // installation type
        { C_GENERATING_START_SCRIPTS, "Start Scripts werden generiert ..." }, // progress
        { C_GERMAN, "Deutsch" }, // language
        { C_INCLUDE, "Moechten Sie weitere Teile installieren ?" }, // installation type
        { C_INEXCLUDE_PARTS, "Folgende Teile stehen zur Auswahl ({0} = keine weiteren)" }, // installation type
        { C_INSTALL_TYPES, "Die folgenden Installationstypen sind verfuegbar:" }, // installation type
        { C_INVALID_ANSWER, "Die Antwort {0} ist hier nicht gueltig" }, // error
        { C_JAVA_VERSION, "Ihre Java Version fuer den Start von Jython ist: {0} / {1}" }, // version
        { C_MINIMUM, "Minimum (Kern)" }, // installation type
        { C_NO, "n" }, // answer
        { C_NO_BIN_DIRECTORY, "Es gibt kein /bin Verzeichnis unterhalb {0}." }, //error
        { C_NO_JAVA_EXECUTABLE, "Es gibt kein ausfuehrbares java in {0}." }, // error
        { C_NO_VALID_JAVA, "Keine gueltige Java Version gefunden in {0}." }, // error
        { C_NON_EMPTY_TARGET_DIRECTORY, "Das Zielverzeichnis {0} enthaelt bereits Daten" }, // error
        { C_NOT_A_DIRECTORY, "{0} ist kein Verzeichnis. " }, // error
        { C_NOT_FOUND, "{0} nicht gefunden." }, // error
        { C_OS_VERSION, "Ihre Betriebssystem Version ist: {0} / {1}" }, // version
        { C_OVERWRITE_DIRECTORY, "Das Verzeichnis {0} enthaelt bereits Daten, und die Installation wuerde diese ueberschreiben - ok ?" }, // console
        { C_PACKING_STANDALONE_JAR, "Das standalone " + JarInstaller.JYTHON_JAR + " File wird erstellt ..." }, // progress
        { C_PROCEED, "Bitte druecken Sie Enter um weiterzufahren" }, // console
        { C_PROCEED_ANYWAY, "Bitte druecken Sie Enter um trotzdem weiterzufahren" }, // console
        { C_READ_LICENSE, "Moechten Sie die Lizenzvereinbarung jetzt lesen ?" }, // license
        { C_READ_README, "Moechten Sie den Inhalt von README jetzt lesen ?" }, // readme
        { C_SCHEDULED, "{0} zur Installation vorgemerkt" }, // installation type
        { C_SELECT_INSTALL_TYPE, "Bitte waehlen Sie den Installationstyp" }, // installation type
        { C_SELECT_LANGUAGE, "Bitte waehlen Sie Ihre Sprache" }, // language
        { C_SILENT_INSTALLATION, "Die Installation wird ohne Benutzerinteraktion ausgefuehrt" }, // installation mode
        { C_STANDALONE, "Standalone (ein ausfuehrbares .jar File)" }, //installation mode
        { C_STANDARD, "Standard (Kern, Bibliotheksmodule, Demos und Beispiele, Dokumentation)" }, // installation type
        { C_SUCCESS, "Sie haben Jython {0} erfolgreich im Verzeichnis {1} installiert." }, // final
        { C_SUMMARY, "Zusammenfassung:" }, // summary
        { C_TO_CURRENT_JAVA, "Warnung: Wechsel zum aktuellen JDK wegen Fehler: {0}." }, // warning
        { C_UNABLE_CREATE_DIRECTORY, "Fehler beim Erstellen von Verzeichnis {0}." }, // error
        { C_UNABLE_CREATE_TMPFILE, "Fehler beim Erstellen der temporaeren Datei {0}." }, // error
        { C_UNABLE_TO_DELETE, "Fehler beim Loeschen von {0}" }, // console
        { C_UNSCHEDULED, "{0} von der Installation ausgeschlossen" }, // installation type
        { C_UNSUPPORTED_JAVA, "Diese Java Version ist nicht unterstuetzt." }, // version
        { C_UNSUPPORTED_OS, "Dieses Betriebssystem ist eventuell nicht vollstaendig unterstuetzt." }, // version
        { C_USING_TYPE, "Installationstyp ist {0}" }, // installation type
        { C_VERSION_INFO, "Sie sind im Begriff, Jython Version {0} zu installieren." }, // version
        { C_WELCOME_TO_JYTHON, "Willkommen bei Jython !" }, // welcome
        { C_YES, "j" }, // answer

    };

    public Object[][] getContents() {
        return contents;
    }

}