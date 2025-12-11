package com.example.walksafe

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object Translator {
    private val translations = mapOf(
        // --- GENERAL ---
        "Sign Out" to mapOf("es" to "Cerrar Sesión", "fr" to "Déconnexion", "de" to "Abmelden", "el" to "Αποσύνδεση"),
        "Camera" to mapOf("es" to "Cámara", "fr" to "Caméra", "de" to "Kamera", "el" to "Κάμερα"),
        "Gallery" to mapOf("es" to "Galería", "fr" to "Galerie", "de" to "Galerie", "el" to "Συλλογή"),
        "Email" to mapOf("es" to "Correo", "fr" to "E-mail", "de" to "E-Mail", "el" to "Email"),
        "Password" to mapOf("es" to "Contraseña", "fr" to "Mot de passe", "de" to "Passwort", "el" to "Κωδικός"),
        "Cancel" to mapOf("es" to "Cancelar", "fr" to "Annuler", "de" to "Abbrechen", "el" to "Ακύρωση"),
        "OK" to mapOf("es" to "Aceptar", "fr" to "OK", "de" to "OK", "el" to "ΟΚ"),
        "I Understand" to mapOf("es" to "Entendido", "fr" to "Je comprends", "de" to "Verstanden", "el" to "Καταλαβαίνω"),
        "Terms & Conditions" to mapOf("es" to "Términos y Condiciones", "fr" to "Termes et Conditions", "de" to "Allgemeine Geschäftsbedingungen", "el" to "Όροι και Προϋποθέσεις"),
        "TermsLink" to mapOf(
            "en" to "By logging in, you accept our Terms & Conditions",
            "es" to "Al iniciar sesión, aceptas nuestros Términos y Condiciones",
            "fr" to "En vous connectant, vous acceptez nos Termes et Conditions",
            "de" to "Durch die Anmeldung akzeptieren Sie unsere AGB",
            "el" to "Με τη σύνδεση, αποδέχεστε τους Όρους και τις Προϋποθέσεις μας"
        ),

        // --- NEW: T&C ACCEPTANCE FLOW ---
        "IAgree" to mapOf(
            "en" to "I agree to the Terms & Conditions",
            "es" to "Acepto los Términos y Condiciones",
            "fr" to "J'accepte les Termes et Conditions",
            "de" to "Ich stimme den AGB zu",
            "el" to "Συμφωνώ με τους Όρους και τις Προϋποθέσεις"
        ),
        "AcceptContinue" to mapOf(
            "en" to "Accept & Continue",
            "es" to "Aceptar y Continuar",
            "fr" to "Accepter et Continuer",
            "de" to "Akzeptieren & Fortfahren",
            "el" to "Αποδοχή & Συνέχεια"
        ),

        // --- SETTINGS ---
        "Settings" to mapOf("es" to "Configuración", "fr" to "Paramètres", "de" to "Einstellungen", "el" to "Ρυθμίσεις"),
        "General" to mapOf("es" to "General", "fr" to "Général", "de" to "Allgemein", "el" to "Γενικά"),
        "Language" to mapOf("es" to "Idioma", "fr" to "Langue", "de" to "Sprache", "el" to "Γλώσσα"),
        "Dark Mode" to mapOf("es" to "Modo Oscuro", "fr" to "Mode Sombre", "de" to "Dunkelmodus", "el" to "Σκοτεινή Λειτουργία"),
        "Use Metric (meters)" to mapOf("es" to "Usar Métrico (metros)", "fr" to "Utiliser Métrique (mètres)", "de" to "Metrisch verwenden", "el" to "Χρήση μετρικού"),
        "AI & Analysis" to mapOf("es" to "IA y Análisis", "fr" to "IA et Analyse", "de" to "KI & Analyse", "el" to "AI & Ανάλυση"),
        "Default to Online AI" to mapOf("es" to "IA en línea por defecto", "fr" to "IA en ligne par défaut", "de" to "Standardmäßig Online-KI", "el" to "Προεπιλογή Online AI"),
        "Save Original Photos" to mapOf("es" to "Guardar fotos originales", "fr" to "Enregistrer les photos originales", "de" to "Originalfotos speichern", "el" to "Αποθήκευση αρχικών φωτογραφιών"),
        "Account" to mapOf("es" to "Cuenta", "fr" to "Compte", "de" to "Konto", "el" to "Λογαριασμός"),
        "Edit Profile" to mapOf("es" to "Editar Perfil", "fr" to "Modifier le profil", "de" to "Profil bearbeiten", "el" to "Επεξεργασία Προφίλ"),
        "Delete Account" to mapOf("es" to "Eliminar Cuenta", "fr" to "Supprimer le compte", "de" to "Konto löschen", "el" to "Διαγραφή Λογαριασμού"),
        "Delete Warning" to mapOf(
            "en" to "Are you sure? This will permanently delete your reports and data.",
            "es" to "¿Estás seguro? Esto eliminará permanentemente tus informes.",
            "fr" to "Êtes-vous sûr? Cela supprimera définitivement vos données.",
            "de" to "Bist du sicher? Dies wird Ihre Daten dauerhaft löschen.",
            "el" to "Είστε σίγουροι; Αυτό θα διαγράψει οριστικά τα δεδομένα σας."
        ),
        "Privacy Policy" to mapOf("es" to "Política de Privacidad", "fr" to "Politique de confidentialité", "de" to "Datenschutzrichtlinie", "el" to "Πολιτική Απορρήτου"),

        // --- LOGIN SCREEN ---
        "WALKSAFE TO SCHOOL" to mapOf("es" to "CAMINA SEGURO A LA ESCUELA", "fr" to "MARCHEZ EN SÉCURITÉ", "de" to "SICHER ZUR SCHULE", "el" to "ΑΣΦΑΛΗΣ ΔΙΑΔΡΟΜΗ ΠΡΟΣ ΤΟ ΣΧΟΛΕΙΟ"),
        "Welcome Back" to mapOf("es" to "Bienvenido de nuevo", "fr" to "Bon retour", "de" to "Willkommen zurück", "el" to "Καλώς ήρθατε πάλι"),
        "Log In" to mapOf("es" to "Iniciar Sesión", "fr" to "Connexion", "de" to "Anmelden", "el" to "Σύνδεση"),
        "Sign in with Google" to mapOf("es" to "Inicia con Google", "fr" to "Continuer avec Google", "de" to "Mit Google anmelden", "el" to "Σύνδεση με Google"),
        "Don't have an account? Sign Up" to mapOf("es" to "¿No tienes cuenta? Regístrate", "fr" to "Pas de compte? S'inscrire", "de" to "Kein Konto? Registrieren", "el" to "Δεν έχετε λογαριασμό; Εγγραφή"),
        "Protected by reCAPTCHA Enterprise" to mapOf("es" to "Protegido por reCAPTCHA", "fr" to "Protégé par reCAPTCHA", "de" to "Geschützt durch reCAPTCHA", "el" to "Προστατεύεται από reCAPTCHA"),

        // --- SIGN UP SCREEN ---
        "Create Account" to mapOf("es" to "Crear Cuenta", "fr" to "Créer un compte", "de" to "Konto erstellen", "el" to "Δημιουργία Λογαριασμού"),
        "First Name" to mapOf("es" to "Nombre", "fr" to "Prénom", "de" to "Vorname", "el" to "Όνομα"),
        "Last Name" to mapOf("es" to "Apellido", "fr" to "Nom", "de" to "Nachname", "el" to "Επίθετο"),
        "School / University" to mapOf("es" to "Escuela / Universidad", "fr" to "École / Université", "de" to "Schule / Universität", "el" to "Σχολείο / Πανεπιστήμιο"),
        "Email Address" to mapOf("es" to "Dirección de correo", "fr" to "Adresse e-mail", "de" to "E-Mail-Adresse", "el" to "Διεύθυνση Email"),
        "Confirm Password" to mapOf("es" to "Confirmar Contraseña", "fr" to "Confirmer le mot de passe", "de" to "Passwort bestätigen", "el" to "Επιβεβαίωση Κωδικού"),
        "Register" to mapOf("es" to "Registrarse", "fr" to "S'inscrire", "de" to "Registrieren", "el" to "Εγγραφή"),
        "Already have an account? Log In" to mapOf("es" to "¿Ya tienes cuenta? Inicia sesión", "fr" to "Déjà un compte? Connexion", "de" to "Bereits ein Konto? Anmelden", "el" to "Έχετε ήδη λογαριασμό; Σύνδεση"),

        // --- HOME SCREEN ---
        "Welcome" to mapOf("es" to "Bienvenido", "fr" to "Bienvenue", "de" to "Willkommen", "el" to "Καλώς ήρθατε"),
        "You are securely logged in." to mapOf("es" to "Has iniciado sesión de forma segura.", "fr" to "Vous êtes connecté en toute sécurité.", "de" to "Sie sind sicher eingeloggt.", "el" to "Έχετε συνδεθεί με ασφάλεια."),
        "Start Safe Walk" to mapOf("es" to "Iniciar Caminata Segura", "fr" to "Commencer la Marche", "de" to "Sicheren Weg Starten", "el" to "Έναρξη Ασφαλούς Διαδρομής"),
        "Edit Profile" to mapOf("es" to "Editar Perfil", "fr" to "Modifier le profil", "de" to "Profil bearbeiten", "el" to "Επεξεργασία Προφίλ"),

        // --- REPORT SCREEN ---
        "New WalkSafe Report" to mapOf("es" to "Nuevo Reporte WalkSafe", "fr" to "Nouveau Rapport", "de" to "Neuer WalkSafe Bericht", "el" to "Νέα Αναφορά WalkSafe"),
        "Step 1: Capture Sidewalk" to mapOf("es" to "Paso 1: Capturar Acera", "fr" to "Étape 1: Capturer le trottoir", "de" to "Schritt 1: Gehweg aufnehmen", "el" to "Βήμα 1: Καταγραφή Πεζοδρομίου"),
        "Retake Photo" to mapOf("es" to "Retomar Foto", "fr" to "Reprendre la photo", "de" to "Foto wiederholen", "el" to "Λήψη ξανά"),
        "Step 2: AI Analysis" to mapOf("es" to "Paso 2: Análisis IA", "fr" to "Étape 2: Analyse IA", "de" to "Schritt 2: KI-Analyse", "el" to "Βήμα 2: Ανάλυση AI"),
        "Analyze Sidewalk with AI" to mapOf("es" to "Analizar con IA", "fr" to "Analyser avec l'IA", "de" to "Mit KI analysieren", "el" to "Ανάλυση Πεζοδρομίου με AI"),
        "Analysis Complete" to mapOf("es" to "Análisis Completo", "fr" to "Analyse terminée", "de" to "Analyse abgeschlossen", "el" to "Η Ανάλυση Ολοκληρώθηκε"),
        "Step 3: Location" to mapOf("es" to "Paso 3: Ubicación", "fr" to "Étape 3: Emplacement", "de" to "Schritt 3: Standort", "el" to "Βήμα 3: Τοποθεσία"),
        "Get GPS Location" to mapOf("es" to "Obtener Ubicación GPS", "fr" to "Obtenir la localisation GPS", "de" to "GPS-Standort abrufen", "el" to "Λήψη τοποθεσίας GPS"),
        "Add Report to List & Save to Cloud" to mapOf("es" to "Añadir a lista y guardar", "fr" to "Ajouter à la liste", "de" to "Zur Liste hinzufügen", "el" to "Προσθήκη στη λίστα & Αποθήκευση"),
        "Collected Reports" to mapOf("es" to "Reportes Recolectados", "fr" to "Rapports collectés", "de" to "Gesammelte Berichte", "el" to "Συλλεχθείσες Αναφορές"),
        "Finish & Download Full CSV" to mapOf("es" to "Finalizar y Descargar CSV", "fr" to "Terminer et télécharger CSV", "de" to "Beenden & CSV herunterladen", "el" to "Τέλος & Λήψη CSV"),

        // --- PROFILE SCREEN ---
        "Tap to change photo" to mapOf("es" to "Toca para cambiar foto", "fr" to "Appuyez pour changer", "de" to "Tippen zum Ändern", "el" to "Πατήστε para αλλαγή φωτογραφίας"),
        "Date of Birth (DD/MM/YYYY)" to mapOf("es" to "Fecha de Nacimiento", "fr" to "Date de naissance", "de" to "Geburtsdatum", "el" to "Ημερομηνία Γέννησης"),
        "New Password (Leave empty to keep current)" to mapOf("es" to "Nueva Contraseña (Dejar vacío)", "fr" to "Nouveau mot de passe", "de" to "Neues Passwort", "el" to "Νέος Κωδικός"),
        "Save Changes" to mapOf("es" to "Guardar Cambios", "fr" to "Sauvegarder", "de" to "Änderungen speichern", "el" to "Αποθήκευση Αλλαγών"),

        // --- TERMS & CONDITIONS (DETAILED) ---
        "TermsTitle" to mapOf(
            "en" to "WalkSafe User Agreement",
            "es" to "Acuerdo de Usuario de WalkSafe",
            "fr" to "Accord d'Utilisateur WalkSafe",
            "de" to "WalkSafe Benutzervereinbarung",
            "el" to "Συμφωνία Χρήστη WalkSafe"
        ),
        "TermsContent" to mapOf(
            "en" to """
                1. Introduction
                Welcome to WalkSafe. By accessing or using our mobile application, you agree to be bound by these Terms and Conditions and our Privacy Policy. If you disagree with any part of these terms, you may not access the service.

                2. User Accounts
                When you create an account with us, you must provide information that is accurate, complete, and current at all times. Failure to do so constitutes a breach of the Terms, which may result in immediate termination of your account. You are responsible for safeguarding the password that you use to access the service and for any activities or actions under your password.

                3. Acceptable Use
                You agree not to use the application for any unlawful purpose or in any way that interrupts, damages, or impairs the service. You are solely responsible for the content (images, reports) you upload. You must not upload content that is illegal, offensive, or infringes on the rights of others.

                4. Privacy and Data Protection
                We respect your privacy. We collect GPS data and images solely for the purpose of analyzing sidewalk safety. We employ AI technology to blur faces and license plates to protect the privacy of individuals in the images you upload. However, no system is 100% secure, and we cannot guarantee absolute privacy of uploaded content before processing. Your data is stored securely in the cloud.

                5. AI Analysis Disclaimer
                The safety reports, measurements, and assessments provided by WalkSafe are generated by Artificial Intelligence (including Gemini, TFLite, or Azure models). These are estimates for informational purposes only. WalkSafe does not guarantee the accuracy, completeness, or reliability of any AI-generated data. Users should exercise their own judgment and not rely solely on the App for navigation or safety decisions.

                6. Limitation of Liability
                In no event shall WalkSafe, nor its directors, employees, partners, agents, suppliers, or affiliates, be liable for any indirect, incidental, special, consequential or punitive damages, including without limitation, loss of profits, data, use, goodwill, or other intangible losses, resulting from your access to or use of or inability to access or use the Service.

                7. Changes
                We reserve the right, at our sole discretion, to modify or replace these Terms at any time. By continuing to access or use our Service after those revisions become effective, you agree to be bound by the revised terms.

                Last Updated: December 2025
            """.trimIndent(),
            "es" to """
                1. Introducción
                Bienvenido a WalkSafe. Al acceder o utilizar nuestra aplicación móvil, aceptas estar sujeto a estos Términos y Condiciones y a nuestra Política de Privacidad. Si no estás de acuerdo con alguna parte de estos términos, no podrás acceder al servicio.

                2. Cuentas de Usuario
                Al crear una cuenta con nosotros, debes proporcionar información precisa, completa y actual en todo momento. El incumplimiento de esto constituye una violación de los Términos, lo que puede resultar en la terminación inmediata de tu cuenta. Eres responsable de salvaguardar la contraseña que utilizas para acceder al servicio.

                3. Uso Aceptable
                Aceptas no utilizar la aplicación para ningún propósito ilegal o de cualquier manera que interrumpa, dañe o perjudique el servicio. Eres el único responsable del contenido (imágenes, informes) que subas. No debes subir contenido ilegal u ofensivo.

                4. Privacidad y Protección de Datos
                Respetamos tu privacidad. Recopilamos datos GPS e imágenes únicamente con el fin de analizar la seguridad de las aceras. Utilizamos tecnología de IA para difuminar rostros y matrículas. Sin embargo, ningún sistema es 100% seguro.

                5. Descargo de Responsabilidad de Análisis de IA
                Los informes de seguridad proporcionados por WalkSafe son generados por Inteligencia Artificial. Estas son estimaciones solo para fines informativos. WalkSafe no garantiza la precisión de las mediciones o evaluaciones de seguridad.

                6. Limitación de Responsabilidad
                En ningún caso WalkSafe será responsable de daños indirectos, incidentales, especiales, consecuentes o punitivos resultantes de tu acceso o uso del Servicio.

                7. Cambios
                Nos reservamos el derecho de modificar estos Términos en cualquier momento.

                Última Actualización: Diciembre 2025
            """.trimIndent(),
            "fr" to """
                1. Introduction
                Bienvenue sur WalkSafe. En accédant ou en utilisant notre application, vous acceptez d'être lié par ces Termes et Conditions.

                2. Comptes Utilisateur
                Vous devez fournir des informations exactes lors de la création d'un compte. Vous êtes responsable de la sécurité de votre mot de passe.

                3. Utilisation Acceptable
                Vous acceptez de ne pas utiliser l'application à des fins illégales. Vous êtes seul responsable du contenu que vous téléchargez.

                4. Confidentialité
                Nous collectons des données GPS et des images pour analyser la sécurité des trottoirs. Nous utilisons l'IA pour flouter les visages, mais nous ne pouvons garantir une confidentialité absolue.

                5. Avertissement sur l'IA
                Les rapports de sécurité sont générés par l'IA et sont des estimations. WalkSafe ne garantit pas leur exactitude. Ne vous fiez pas uniquement à l'application pour la navigation.

                6. Limitation de Responsabilité
                WalkSafe n'est pas responsable des dommages indirects ou consécutifs résultant de l'utilisation du service.

                7. Modifications
                Nous nous réservons le droit de modifier ces termes à tout moment.

                Dernière mise à jour: Décembre 2025
            """.trimIndent(),
            "de" to """
                1. Einführung
                Willkommen bei WalkSafe. Durch die Nutzung unserer App stimmen Sie diesen Bedingungen zu.

                2. Benutzerkonten
                Sie müssen genaue Informationen angeben. Sie sind für die Sicherheit Ihres Passworts verantwortlich.

                3. Zulässige Nutzung
                Sie dürfen die App nicht für illegale Zwecke verwenden. Sie sind für Ihre hochgeladenen Inhalte verantwortlich.

                4. Datenschutz
                Wir sammeln GPS-Daten und Bilder zur Analyse der Gehwegsicherheit. Wir verwenden KI, um Gesichter unkenntlich zu machen, garantieren aber keine absolute Privatsphäre.

                5. KI-Haftungsausschluss
                Sicherheitsberichte werden von KI erstellt und sind nur Schätzungen. WalkSafe garantiert nicht für deren Genauigkeit.

                6. Haftungsbeschränkung
                WalkSafe haftet nicht für indirekte Schäden, die aus der Nutzung des Dienstes entstehen.

                7. Änderungen
                Wir behalten uns das Recht vor, diese Bedingungen jederzeit zu ändern.

                Letzte Aktualisierung: Dezember 2025
            """.trimIndent(),
            "el" to """
                1. Εισαγωγή
                Καλώς ήρθατε στο WalkSafe. Με την πρόσβαση στην εφαρμογή μας, συμφωνείτε να δεσμεύεστε από αυτούς τους Όρους και Προϋποθέσεις.

                2. Λογαριασμοί Χρηστών
                Πρέπει να παρέχετε ακριβείς πληροφορίες κατά τη δημιουργία λογαριασμού. Είστε υπεύθυνοι για την ασφάλεια του κωδικού πρόσβασής σας.

                3. Αποδεκτή Χρήση
                Συμφωνείτε να μην χρησιμοποιείτε την εφαρμογή για παράνομους σκοπούς. Είστε αποκλειστικά υπεύθυνοι για το περιεχόμενο που ανεβάζετε.

                4. Απόρρητο και Προστασία Δεδομένων
                Συλλέγουμε δεδομένα GPS και εικόνες για την ανάλυση της ασφάλειας των πεζοδρομίων. Χρησιμοποιούμε τεχνητή νοημοσύνη για τη θόλωση προσώπων, αλλά δεν μπορούμε να εγγυηθούμε απόλυτη ιδιωτικότητα.

                5. Αποποίηση Ευθύνης AI
                Οι αναφορές ασφαλείας παράγονται από AI και αποτελούν εκτιμήσεις. Το WalkSafe δεν εγγυάται την ακρίβεια των μετρήσεων.

                6. Περιορισμός Ευθύνης
                Το WalkSafe δεν ευθύνεται για έμμεσες ζημίες που προκύπτουν από τη χρήση της υπηρεσίας.

                7. Αλλαγές
                Διαιτηρούμε το δικαίωμα να τροποποιήσουμε αυτούς τους όρους ανά πάσα στιγμή.

                Τελευταία Ενημέρωση: Δεκέμβριος 2025
            """.trimIndent()
        ),

        // --- PRIVACY POLICY (NEW) ---
        "PrivacyTitle" to mapOf(
            "en" to "Privacy Policy",
            "es" to "Política de Privacidad",
            "fr" to "Politique de Confidentialité",
            "de" to "Datenschutzrichtlinie",
            "el" to "Πολιτική Απορρήτου"
        ),
        "PrivacyContent" to mapOf(
            "en" to """
                1. Information We Collect
                - Location Data: We access your precise location (GPS) only when you actively report an issue or use the map feature. This data is linked to your submitted reports.
                - Camera & Photos: We access your camera and gallery only when you choose to capture or upload an image of a sidewalk.
                - User Account: We store your email, name, and profile photo to manage your account and history.

                2. How We Use Information
                - Safety Reports: Images and locations are analyzed by AI to detect hazards. These reports are stored in our database to build a safety map.
                - AI Analysis: Images are processed by Cloud AI (Azure) or On-Device AI (TFLite) to identify sidewalk features.
                - Improvement: Anonymized data may be used to improve our AI models.

                3. Privacy & Redaction
                - We value your privacy. Before any image is stored or analyzed for sidewalk features, we use ML Kit technology to detect and blur faces and license plates.
                - While we strive for 100% accuracy, automated redaction may occasionally miss items.

                4. Data Storage
                - Your data is stored securely on Google Firebase servers.
                - You can request deletion of your account and associated data at any time via the Settings menu.

                5. Third-Party Services
                - We use Google Maps for location visualization.
                - We use Google Firebase for authentication and storage.
                - We use Microsoft Azure OpenAI for advanced image analysis (if Online Mode is selected).
            """.trimIndent(),
            "es" to """
                1. Información que Recopilamos
                - Datos de Ubicación: Accedemos a tu GPS solo cuando reportas un problema.
                - Cámara y Fotos: Solo cuando subes una imagen.
                - Cuenta de Usuario: Guardamos tu email y nombre.

                2. Uso de la Información
                - Informes de Seguridad: Las imágenes se analizan para detectar peligros.
                - Análisis IA: Usamos Azure o TFLite para procesar imágenes.

                3. Privacidad y Redacción
                - Difuminamos rostros y matrículas antes de guardar las imágenes.

                4. Almacenamiento
                - Tus datos están seguros en Google Firebase.
                - Puedes eliminar tu cuenta en Configuración.

                5. Terceros
                - Usamos Google Maps, Firebase y Azure OpenAI.
            """.trimIndent(),
            "fr" to """
                1. Informations collectées
                - Localisation, Caméra, Compte utilisateur.

                2. Utilisation
                - Rapports de sécurité, Analyse IA.

                3. Confidentialité
                - Nous floutons les visages et les plaques d'immatriculation.

                4. Stockage
                - Données sécurisées sur Firebase. Suppression possible via les Paramètres.

                5. Services tiers
                - Google Maps, Firebase, Azure OpenAI.
            """.trimIndent(),
            "de" to """
                1. Gesammelte Informationen
                - Standort, Kamera, Benutzerkonto.

                2. Nutzung
                - Sicherheitsberichte, KI-Analyse.

                3. Datenschutz
                - Wir machen Gesichter und Nummernschilder unkenntlich.

                4. Speicherung
                - Sicher auf Firebase. Löschung in den Einstellungen möglich.

                5. Drittanbieter
                - Google Maps, Firebase, Azure OpenAI.
            """.trimIndent(),
            "el" to """
                1. Πληροφορίες που Συλλέγουμε
                - Δεδομένα Τοποθεσίας, Κάμερα, Λογαριασμός Χρήστη.

                2. Πώς Χρησιμοποιούμε τις Πληροφορίες
                - Αναφορές Ασφαλείας, Ανάλυση AI.

                3. Απόρρητο & Διαγραφή
                - Θολώνουμε πρόσωπα και πινακίδες πριν την αποθήκευση.

                4. Αποθήκευση Δεδομένων
                - Τα δεδομένα αποθηκεύονται στο Firebase. Μπορείτε να διαγράψετε τον λογαριασμό σας.

                5. Υπηρεσίες Τρίτων
                - Google Maps, Firebase, Azure OpenAI.
            """.trimIndent()
        )
    )

    fun get(text: String, lang: String): String {
        return translations[text]?.get(lang) ?: text
    }
}

fun applySavedLocale(context: Context): String {
    val sharedPrefs = context.getSharedPreferences("WalkSafePrefs", Context.MODE_PRIVATE)
    val savedLang = sharedPrefs.getString("LANGUAGE", "en") ?: "en"
    val locale = Locale(savedLang)
    Locale.setDefault(locale)
    val config = Configuration()
    config.setLocale(locale)
    context.resources.updateConfiguration(config, context.resources.displayMetrics)
    return savedLang
}