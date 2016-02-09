// Ionic Starter App

// angular.module is a global place for creating, registering and retrieving Angular modules
// 'starter' is the name of this angular module example (also set in a <body> attribute in index.html)
// the 2nd parameter is an array of 'requires'
// 'starter.services' is found in services.js
// 'starter.controllers' is found in controllers.js
angular.module('starter', ['ionic', 'starter.controllers', 'starter.controller.dash', 'starter.controller.request', 'starter.controller.account', 'starter.services', 'starter.api', 'starter.mock', 'ngCordova', 'pascalprecht.translate'])

.run(function(AppContext, $rootScope, $ionicPlatform, $cordovaGlobalization, $cordovaGeolocation, $log, $cordovaToast, $translate) {
  $ionicPlatform.ready(function() {

    // Hide the accessory bar by default (remove this to show the accessory bar above the keyboard
    // for form inputs)
    if (window.cordova && window.cordova.plugins && window.cordova.plugins.Keyboard) {
      cordova.plugins.Keyboard.hideKeyboardAccessoryBar(false);
      cordova.plugins.Keyboard.disableScroll(true);
    }

    try {
        // hide native status bar
        ionic.Platform.fullScreen();
        if (typeof window.StatusBar != "undefined")  {
            window.StatusBar.hide();
            console.log("OK window.StatusBar.hide()");
        } else {
            console.log("FAIL no window.StatusBar");
        }
    } catch (e) {
        alert("FAIL on hide native status bar: "+e);
    }

    // set running os info
    try {
        $rootScope.os = "browser";
        if (typeof window.device != "undefined") $rootScope.os = window.device.platform;
    } catch (e) {
        alert("FAIL set running os info: "+e);
    }

    /*
     * GET LANGUAGE OF DEVICE
     * http://ngcordova.com/docs/plugins/globalization/
     */
    var gotLang = false;
    $cordovaGlobalization.getLocaleName().then(
          function(result) {
              // WIN
              if (!gotLang) {
                  gotLang=true;

                  // check available lang
                  var lang = result.value.substr(0,2);
                  if ((lang!="en") && (lang!="de") && (lang!="ar")) {
                      $log.warn("lang '"+lang+"' not available ... using 'en'");
                      lang = "en";
                  }

                  // check if changed
                  if (AppContext.getAppLang()!=lang) {
                      $log.info("switching to lang("+lang+")");
                      AppContext.setAppLang(lang);
                      $translate.use(AppContext.getAppLang());
                      $rootScope.spClass = AppContext.getAppLangDirection();
                  } else {
                      $log.info("already running lang("+lang+") ... no need to switch");
                  }

                  // check if user spoken lang is set
                  var account = AppContext.getAccount();
                  if (account.spokenLangs.length==0) {
                      $log.info("user default lang in account set to: "+lang);
                      account.spokenLangs.push(lang);
                      AppContext.setAccount(account);
                  }

              } else {
                  $log.warn("double call prevent of $cordovaGlobalization.getLocaleName()");
              }

          },
          function(err) {
              // FAIL
              $log.info("cordovaGlobalization: FAIL "+err);
          }
    );

    $rootScope.lat  = 0;
    $rootScope.lon = 0;

    /*
     * START GEOLOCATION
     * http://ngcordova.com/docs/plugins/geolocation/
     */
    var posOptions = {timeout: 10000, enableHighAccuracy: false};
    $rootScope.gps  = 'wait';
    $rootScope.lat  = 0;
    $rootScope.lon = 0;
      $cordovaGeolocation
          .getCurrentPosition(posOptions)
          .then(function (position) {
              $rootScope.lat  = position.coords.latitude;
              $rootScope.lon = position.coords.longitude;
              $rootScope.gps  = 'win';
              $log.info("lat("+$rootScope.lat+") long("+$rootScope.lon+")");
          }, function(err) {
              // error

              alert("GPS ERROR ---> FOR TESTING RUNNING WITH FAKE COORDINATES ---> REMOVE LATER");
              $rootScope.lat  = 52.52;
              $rootScope.lon = 13.13;
              $rootScope.gps  = 'win';

              /* TODO enable again later on
              $log.info("GPS ERROR");
              $rootScope.gps  = 'fail';
              */
          });

    /*
     * App Context
     */
    try {
        AppContext.loadContext(function(){
            /*
             * i18n SETTINGS
             */
            $translate.use(AppContext.getAppLang());
            $rootScope.spClass = AppContext.getAppLangDirection();
        });
    } catch (e) {
        alert("FAIL i18n SETTINGS: "+e);
    }

    // global scope data
    $rootScope.party = {id:0};

  });
})

.config(function($stateProvider, $urlRouterProvider, $translateProvider, $ionicConfigProvider) {

   $ionicConfigProvider.tabs.position('bottom');

  /*
   * i18n --> https://angular-translate.github.io/docs
   */

   $translateProvider.translations('en', {
            'TAB_PARTIES' : 'Parties',
            'TAB_REQUEST' : 'Request',
            'TAB_MORE' : 'More',
            'KONFETTI' : 'confetti',
            'KONFETTI-APP' : 'Konfetti',
            'PARTYINFO_TITLE': 'Party Info',
            'PARTYINFO_SUB': 'editorial responsibility',
            'POSTSORT_MOST': 'top confetti',
            'POSTSORT_NEW': 'newest entry',
            'DASHHEADER_REVIEW': 'Please Review',
            'DASHHEADER_POSTED': 'You Posted',
            'DASHHEADER_ACTIVE': 'You Answered',
            'DASHHEADER_OPEN': 'Open Requests',
            'NOTIFICATION_REVIEWOK' : 'Your request is now public.',
            'NOTIFICATION_REVIEWFAIL' : 'Your request was rejected.',
            'NOTIFICATION_CHATREQUEST' : 'You got a chat message.',
            'NOCONTENT' : 'no request yet',
            'NEWREQUEST' : 'Publish a new Request',
            'YOURNAME' : 'Your Name',
            'YOURREQUEST' : 'Your Request',
            'PUBLICINFO' : 'Public Information',
            'PRIVATECHATS' : 'Private Chats',
            'ADDINFO' : 'add image, text, location',
            'ISPEAK' : 'I speak',
            'SUBMIT' : 'Submit Request',
            'REWARD' : 'reward',
            'YOUGIVE' : 'you give',
            'IMPORTANT' : 'Important',
            'ENTERNAME' : 'Please enter your name before submit.',
            'THANKYOU' : 'Thank You',
            'SUBMITINFO' : 'Your request gets now reviewed. You will get a notification once it is public.',
            'ENTERREQUEST' : 'Please enter a short request description.',
            'PARTYWAIT' : 'loading party',
            'INTERNETFAIL' : 'no internet',
            'ACCOUNTWAIT' : 'registering',
            'GPSWAIT' : 'getting position',
            'GPSFAIL' : 'please activate GPS',
            'PARTYLISTWAIT' : 'loading parties',
            'YOUCOLLECT' : 'You collected total',
            'YOUTOP' : 'You are within the best',
            'REDEEMCOUPON' : 'Redeem Konfetti Coupon',
            'MAGICCODE' : 'Enter Magic Password',
            'GLOBALSETTINGS' : 'Global Settings',
            'ENABLEPUSH' : 'Enable Pushnotifications',
            'PAUSECHAT' : 'Pause Chat',
            'NEEDSGPS'  : 'turn on location',
            'NEEDSINTERNET'  : 'needs internet connection',
            'LOWKONFETTI'  : 'You have too little confetti to open a request.',
            'MINKONFETTI'  : 'Minimal amount needed',
            'CONTACT' : 'Contact',
            'HELPOUT' : 'help out and earn up to',
            'GETREWARD' : 'as reward',
            'HELPCHAT' : 'Start Chat',
            'INTERNETPROBLEM' : 'Problem with Connection. Please try again later.',
            'ENTERNAME' : 'Please enter your name',
            'SENDMESSAGE' : 'send a message',
            'INTRO_WELCOME' : 'Welcome to Konfetti App',
            'INTRO_STEP1A' : 'This app is about',
            'INTRO_STEP1B' : 'neighborly help parties.',
            'INTRO_STEP2A' : 'Earn Konfetti',
            'INTRO_STEP2B' : 'by taking care of tasks or by donating.',
            'INTRO_STEP3A' : 'Use Konfetti',
            'INTRO_STEP3B' : 'to post tasks to community or to up vote existing tasks.',
            'INTRO_LETSGO' : 'Show confetti parties in my area.',
            'CREATENEW'    : 'create new',
            'REDEEM_MAGIC_SUB' : 'activate features, add privileges ..',
            'REDEEM_COUPON_SUB' : 'Please enter the code number of your coupon:',
            'ANSWERE' : 'Result',
            'REQUESTEDON' : 'Requested on',
            'REQUESTDONE': 'mark as done - reward konfetti',
            'REQUESTDELETE': 'delete request',
            'REQUESTAPPROVE': 'approve request',
            'REQUESTREJECT': 'reject request',
            'REQUESTPROCESS' : 'help found - pause new chats',
            'REQUESTREOPEN' : 're-open for help offers',
            'ENTERREASON' : 'reason for rejection',
            'CONFIRM_DELETE' : 'Do you really want to delete?',
            'CONFIRM_DELETE_AUTHOR' : 'All konfetti spend by the author will get lost. Voting konfetti by others will get refunded. Do you really want to delete?',
            'REQUESTREJECT_AFTER' : 'revoke request',
            'EXPLAIN_REVIEW_USER' : 'Is not public yet - waiting for review.',
            'EXPLAIN_REVIEW_ADMIN' : 'Please choose reject or approve this request.',
            'EXPLAIN_REJECTED' : 'Not public - was rejected.',
            'EXPLAIN_PROCESSING_AUTHOR' : 'Is public - but new help offers are blocked. Re-open if you need more help or reward konfetti when done.',
            'EXPLAIN_PROCESSING_PUBLIC' : 'Still open, but author was already promised help. So chat is deactivated.',
            'EXPLAIN_OPEN_AUTHOR' : 'Is public. Please answere incoming chats and reward konfetti once is done.',
            'EXPLAIN_OPEN_PUBLIC' : 'If you are interested to help out, start a chat and ask for details. Our just upvote with your konfetti.',
            'EXPLAIN_DONE_PUBLIC' : 'Sucessfully done. Visible just for the archive.',
            'EXPLAIN_DONE_AUTHOR' : 'Done. Still public for people to see.',
            'IMAGE' : 'image',
            'TEXT'  : 'text',
            'LOCATION' : 'location',
            'ADDTEXT' : 'Add Text',
            'ENTERTEXT' : 'Enter the text you like to add:',
            'REWARDKONFETTI' : 'reward konfetti',
            'SELECTREWARD' : 'select one or more chat partners',
            'INFO' : 'info',
            'INFO_ZEROKONFETTI' : 'You have no konfetti to support this request. See party info how to get konfetti.',
            'OK' : 'OK',
            'CANCEL' : 'Cancel',
            'PLEASE_REVIEW' : 'please review',
            'INFO_REQUESTFAIL' : 'Check Internet or try again later.',
            'INFO_FAILTRYAGAIN': 'This failed. Please try again or report to developers.',
            'AUTOTRANSLATE_HEAD' : 'Auto Translate',
            'AUTOTRANSLATE_INFO' : 'This text was auto translated by a robot. Please keep in mind, that robots are not perfect and make mistakes.',
            'USELOCATION' : 'Would you like to add your current location?'
   });

   $translateProvider.translations('de', {
            'TAB_PARTIES' : 'Parties',
            'TAB_REQUEST' : 'Anfrage',
            'TAB_MORE' : 'Mehr',
            'KONFETTI' : 'Konfetti',
            'KONFETTI-APP' : 'Konfetti',
            'PARTYINFO_TITLE': 'Party Info',
            'PARTYINFO_SUB': 'inhaltlich verantwortlich',
            'POSTSORT_MOST': 'top konfetti',
            'POSTSORT_NEW': 'neuste posts',
            'DASHHEADER_REVIEW': 'Bitte Prüfen',
            'DASHHEADER_POSTED': 'Deine Anfragen',
            'DASHHEADER_ACTIVE': 'Deine Antworten',
            'DASHHEADER_OPEN': 'Offene Anfragen',
            'NOTIFICATION_REVIEWOK' : 'Deine Anfrage ist jetzt öffentlich.',
            'NOTIFICATION_REVIEWFAIL' : 'Deine Anfrage wurde abgelehnt.',
            'NOTIFICATION_CHATREQUEST' : 'Du hast eine Nachricht.',
            'NOCONTENT' : 'bisher keine Anfragen',
            'NEWREQUEST' : 'Neue Anfrage erstellen',
            'YOURNAME' : 'Dein Name',
            'YOURREQUEST' : 'Deine Anfrage',
            'PUBLICINFO' : 'Öffentliche Informationen',
            'PRIVATECHATS' : 'Private Chats',
            'ADDINFO' : 'Bild, Text, Ortsinformationen hinzufügen',
            'ISPEAK' : 'Ich spreche',
            'SUBMIT' : 'Anfrage abschicken',
            'REWARD' : 'Belohnung',
            'YOUGIVE' : 'Du gibst',
            'IMPORTANT' : 'Wichtig',
            'ENTERNAME' : 'Bitte trage deinen Namen ein.',
            'THANKYOU' : 'Danke',
            'SUBMITINFO' : 'Ihre Anfrage wird nun überprüft. Sie erhalten eine Benachrichtigung, sobald sie veröffentlicht wird.',
            'ENTERREQUEST' : 'Bitte geben Sie eine kurze Beschreibung der Anfrage ein.',
            'PARTYWAIT' : 'Lade Party',
            'INTERNETFAIL' : 'Kein Internet',
            'ACCOUNTWAIT' : 'Anmeldung',
            'GPSWAIT' : 'Bestimme Position',
            'GPSFAIL' : 'Bitte GPS aktvieren',
            'PARTYLISTWAIT' : 'Lade Parties',
            'YOUCOLLECT' : 'Du hast bisher insgesamt gesammelt',
            'YOUTOP' : 'Du bist unter den Besten ',
            'REDEEMCOUPON' : 'Konfetti Gutschein einlösen',
            'MAGICCODE' : 'Magisches Passwort eingeben',
            'GLOBALSETTINGS' : 'Einstellungen',
            'ENABLEPUSH' : 'Push-Meldungen einschalten',
            'PAUSECHAT' : 'Chats pausieren',
            'NEEDSGPS'  : 'bitte GPS aktivieren',
            'NEEDSINTERNET'  : 'Internetverbindung benötigt',
            'LOWKONFETTI'  : 'Du hast zuwenig Konfetti, um eine Anfrage zu starten.',
            'MINKONFETTI'  : 'Minimal nötige Menge',
            'CONTACT' : 'Kontakt',
            'HELPOUT' : 'Helfen und bis zu',
            'GETREWARD' : 'erhalten.',
            'HELPCHAT' : 'Starte Chat',
            'INTERNETPROBLEM' : 'Problem mit der Verbindung. Bitte später nochmal probieren.',
            'ENTERNAME' : 'Bitte gib deinen Namen ein',
            'SENDMESSAGE' : 'sende eine Nachricht',
            'INTRO_WELCOME' : 'Willkommen zur Konfetti App :)',
            'INTRO_STEP1A' : 'In dieser App geht es um',
            'INTRO_STEP1B' : 'Nachbarschaftshilfe-Parties.',
            'INTRO_STEP2A' : 'Verdiene Konfetti',
            'INTRO_STEP2B' : 'indem du Aufgaben erledigst oder spendest.',
            'INTRO_STEP3A' : 'Nutze Konfetti',
            'INTRO_STEP3B' : 'um selber Aufgaben an die Gemeinschaft zu stellen oder bestehende Aufgaben zu unterstützen.',
            'INTRO_LETSGO' : 'Zeige Konfetti-Parties in meiner Nähe.',
            'CREATENEW'    : 'Neu Erstellen',
            'REDEEM_MAGIC_SUB' : 'Aktivierung von zusätzlichen Funktionen oder Rechten ..',
            'REDEEM_COUPON_SUB' : 'Bitte gib die Code-Nummer deines Gutscheines ein:',
            'ANSWERE' : 'Ergebnis',
            'REQUESTEDON' : 'Angefragt am',
            'REQUESTDONE': 'Auf erledigt setzen - Konfetti verteilen',
            'REQUESTDELETE': 'Anfrage löschen',
            'REQUESTAPPROVE': 'Anfrage freischalten',
            'REQUESTREJECT': 'Anfrage ablehnen',
            'REQUESTPROCESS' : 'Hilfe gefunden - keine weiteren Chats',
            'REQUESTREOPEN' : 'Anfrage wieder für Chats öffnen',
            'ENTERREASON' : 'Ablehungsbegründung',
            'CONFIRM_DELETE' : 'Wirklich löschen?',
            'CONFIRM_DELETE_AUTHOR' : 'Bei Löschung erhält der Autor kein Konfetti zurück. Hinzugegebenes Konfetto von anderen wird zurückerstattet. Wirklich löschen?',
            'REQUESTREJECT_AFTER' : 'Anfrage zurückziehen',
            'EXPLAIN_REVIEW_USER' : 'Noch nicht öffentlich - Warten auf Freigabe.',
            'EXPLAIN_REVIEW_ADMIN' : 'Bitte freigeben oder ablehnen.',
            'EXPLAIN_REJECTED' : 'Nicht öffentlich - Wurde abgelehnt.',
            'EXPLAIN_PROCESSING_AUTHOR' : 'Öffentlich - doch für weitere Hilfsangebote geblockt. Wenn weitere Hilfe benötigt wird, Anfrage wieder öffnen oder Konfetti verteilen, wenn erledigt.',
            'EXPLAIN_PROCESSING_PUBLIC' : 'Ausreichend Hilfe wurde zugesagt. Daher ist der Chat deaktiviert.',
            'EXPLAIN_OPEN_AUTHOR' : 'Öffentlich. Bitte Chatanfrage beantworten und und wenn erledigt Konfetti verteilen.',
            'EXPLAIN_OPEN_PUBLIC' : 'Du willst mithelfen? Dann frage im Chat nach Details. Du willst diese Anfrage unterstützen? Dann spende dein Konfetti für die Belohnung mit dem Gefällt-Mir Button.',
            'EXPLAIN_DONE_PUBLIC' : 'Wurde bereits erledigt. Kommt bald ins Archiv.',
            'EXPLAIN_DONE_AUTHOR' : 'Erledigt. Noch öffentlich sichtbar.',
            'IMAGE' : 'Bild',
            'TEXT'  : 'Text',
            'LOCATION' : 'Ort',
            'ADDTEXT' : 'Add Text',
            'ENTERTEXT' : 'Enter the text you like to add:',
            'REWARDKONFETTI' : 'Konfetti verteilen',
            'SELECTREWARD' : 'wähle eine oder mehrere Personen',
            'INFO' : 'Info',
            'INFO_ZEROKONFETTI' : 'Du hast kein Konfetti um diese Anfrage zu unterstützen. Für mehr Konfetti siehe Party Info.',
            'OK' : 'OK',
            'CANCEL' : 'Abbrechen',
            'PLEASE_REVIEW' : 'bitte freigeben',
            'INFO_REQUESTFAIL' : 'Internet überprüfen. Später noch einmal probieren.',
            'INFO_FAILTRYAGAIN': 'Das hat nicht geklappt. Bitte noch einmal probieren oder an die Entwickler melden.',
            'AUTOTRANSLATE_HEAD' : 'Automatische Übersetzung',
            'AUTOTRANSLATE_INFO' : 'Dieser Text wurde automatisiert von einem Roboter aus einer anderen Sprache übersetzt. Roboter sind nicht perfekt. Die Übersetzung kann fehlerhaft sein.',
            'USELOCATION' : 'Die aktuelle Position hinzufügen?'
        });

   $translateProvider.translations('ar', {
            'TAB_PARTIES' : 'حفلات',
            'TAB_REQUEST' : 'طلب',
            'TAB_MORE' : 'مهر',
            'KONFETTI' : 'حلويات',
            'KONFETTI-APP' : 'حلويات',
            'PARTYINFO_TITLE': 'منظم',
            'PARTYINFO_SUB': 'المسؤولية التحريرية',
            'POSTSORT_MOST': 'شعبية',
            'POSTSORT_NEW': 'جديد',
            'DASHHEADER_REVIEW': 'مراجعة',
            'DASHHEADER_POSTED': 'استفساراتك',
            'DASHHEADER_ACTIVE': 'ردكم',
            'DASHHEADER_OPEN': 'طلبات نشطة',
            'NOTIFICATION_REVIEWOK' : 'طلبك الآن العام',
            'NOTIFICATION_REVIEWFAIL' : 'وقد رفض طلبك',
            'NOTIFICATION_CHATREQUEST' : 'كنت حصلت على رسالة دردشة',
            'NOCONTENT' : 'أي طلب حتى الآن',
            'NEWREQUEST' : 'جعل طلب جديد',
            'YOURNAME' : 'اسم الدين',
            'YOURREQUEST' : 'اسم الدين',
            'PUBLICINFO' : 'معلومات عامة',
            'PRIVATECHATS' : 'دردشات خاصة',
            'ADDINFO' : 'الصورة والنص و إضافة معلومات الموقع',
            'ISPEAK' : 'أتكلم',
            'SUBMIT' : 'إرسال طلب',
            'REWARD' : 'مكافأة',
            'YOUGIVE' : 'انت تعطى',
            'IMPORTANT' : 'مهم',
            'ENTERNAME' : 'الرجاء إدخال اسمك.',
            'THANKYOU' : 'شكرا',
            'SUBMITINFO' : 'يحصل استعرض طلبك الآن . سوف تحصل على إخطار مرة واحدة فمن العام.',
            'ENTERREQUEST' : 'الرجاء إدخال وصف طلب القصير.',
            'PARTYWAIT' : 'حزب تحميل',
            'INTERNETFAIL' : 'لا إنترنت',
            'ACCOUNTWAIT' : 'تسجيل',
            'GPSWAIT' : 'الحصول على موقف',
            'GPSFAIL' : 'يرجى تفعيلها GPS',
            'PARTYLISTWAIT' : 'الأحزاب التحميل',
            'YOUCOLLECT' : 'كنت جمعت الكلي',
            'YOUTOP' : 'كنت ضمن أفضل',
            'REDEEMCOUPON' : 'استبدال القسيمة',
            'MAGICCODE' : 'أدخل كلمة المرور ماجيك',
            'GLOBALSETTINGS' : 'إعدادات',
            'ENABLEPUSH' : 'تمكين إخطارات',
            'PAUSECHAT' : 'وقفة الدردشة',
            'NEEDSGPS'  : 'بدوره على الموقع',
            'NEEDSINTERNET'  : 'يحتاج اتصال بالإنترنت',
            'LOWKONFETTI'  : 'لديك حلويات صغيرة جدا لفتح الطلب.',
            'MINKONFETTI'  : 'الحد الأدنى اللازم',
            'CONTACT' : 'اتصال',
            'HELPOUT' : 'مساعدة و تكسب ما يصل الى',
            'GETREWARD' : 'كمكافأة',
            'HELPCHAT' : 'بدء الدردشة',
            'INTERNETPROBLEM' : 'مشكلة مع الاتصال. الرجاء معاودة المحاولة في وقت لاحق.',
            'ENTERNAME' : 'من فضلك أدخل إسمك',
            'SENDMESSAGE' : 'ارسل رسالة',
            'INTRO_WELCOME' : 'أهلا بك',
            'INTRO_STEP1A' : 'هذا التطبيق هو عن الأحزاب مساعدة الجوار .',
            'INTRO_STEP1B' : '',
            'INTRO_STEP2A' : '',
            'INTRO_STEP2B' : 'كسب حلويات من رعاية المهام أو عن طريق التبرع .',
            'INTRO_STEP3A' : '',
            'INTRO_STEP3B' : 'استخدام الورق الملون لإضافة مهام ل مجتمع أو لغاية التصويت المهام الحالية .',
            'INTRO_LETSGO' : 'تظهر الأطراف حلويات في مجال اختصاصي .',
            'CREATENEW'    : 'خلق فرص عمل جديدة',
            'REDEEM_MAGIC_SUB' : 'تنشيط الميزات ، إضافة امتيازات',
            'REDEEM_COUPON_SUB' : 'الرجاء إدخال قانون رقم الكوبون الخاص بك:',
            'ANSWERE' : 'نتيجة',
            'REQUESTEDON' : 'طلب على',
            'REQUESTDONE': 'تأكيد الطلب كما فعلت',
            'REQUESTDELETE': 'حذف الطلب',
            'REQUESTAPPROVE': 'الموافقة على طلب',
            'REQUESTREJECT': 'رفض طلب',
            'REQUESTPROCESS' : 'مساعدة وجدت - لا مزيد من الأحاديث',
            'REQUESTREOPEN' : 'إعادة فتح للعروض المساعدة',
            'ENTERREASON' : 'سبب الرفض',
            'CONFIRM_DELETE' : 'حذف بالتأكيد ؟',
            'CONFIRM_DELETE_AUTHOR' : 'حذف بالتأكيد ؟',
            'REQUESTREJECT_AFTER' : 'إلغاء طلب',
            'EXPLAIN_REVIEW_USER' : 'ليس الجمهور حتى الآن - في انتظار المراجعة.',
            'EXPLAIN_REVIEW_ADMIN' : 'الرجاء اختيار رفض أو الموافقة على هذا الطلب.',
            'EXPLAIN_REJECTED' : 'لا الجمهور - رفض .',
            'EXPLAIN_PROCESSING_AUTHOR' : 'المقرر افتتاحه مرة أخرى اذا كنت بحاجة الى مزيد من المساعدة. مكافأة النثار عند الانتهاء.',
            'EXPLAIN_PROCESSING_PUBLIC' : 'كان لا يزال مفتوحا ، ولكن المؤلف وعدت بالفعل المساعدة. حتى الدردشة و إبطال مفعولها .',
            'EXPLAIN_OPEN_AUTHOR' : 'هو الجمهور . الرجاء الاجابه الأحاديث الواردة و مكافأة حلويات بمجرد القيام به.',
            'EXPLAIN_OPEN_PUBLIC' : 'إذا كنت مهتما للمساعدة، بدء الدردشة و السؤال عن التفاصيل. لدينا فقط حتى التصويت مع حلويات الخاص بك .',
            'EXPLAIN_DONE_PUBLIC' : 'فعلت بنجاح . فقط مرئية للأرشيف',
            'EXPLAIN_DONE_AUTHOR' : 'تم الانتهاء من. لا يزال الجمهور ليرى الناس .',
            'IMAGE' : 'صورة',
            'TEXT'  : 'نص',
            'LOCATION' : 'موقع',
            'ADDTEXT' : 'اضافة نص',
            'ENTERTEXT' : 'أدخل النص الذي ترغب في إضافة :',
            'REWARDKONFETTI' : 'حلويات مكافأة',
            'SELECTREWARD' : 'اختيار واحد أو أكثر من شخص',
            'INFO' : 'المعلومات',
            'INFO_ZEROKONFETTI' : 'لا يوجد لديك حلويات لدعم هذا الطلب . انظر المعلومات الأحزاب كيفية الحصول على قصاصات من الورق .',
            'OK' : 'حسنا',
            'CANCEL' : 'إلغاء',
            'PLEASE_REVIEW' : 'من فضلك اعد النظر',
            'INFO_REQUESTFAIL' : 'تحقق الإنترنت أو المحاولة مرة أخرى في وقت لاحق .',
            'INFO_FAILTRYAGAIN': 'وهذا فشل. الرجاء المحاولة مرة أخرى أو يقدم للمطورين.',
            'AUTOTRANSLATE_HEAD' : 'السيارات ترجمة',
            'AUTOTRANSLATE_INFO' : 'كان هذا النص السيارات وترجم من قبل الروبوت . يرجى أن نضع في اعتبارنا، أن الروبوتات ليست مثالية و ارتكاب الأخطاء .',
            'USELOCATION' : 'هل ترغب في إضافة موقعك الحالي ؟'
   });

  $translateProvider.preferredLanguage("en");
  $translateProvider.useSanitizeValueStrategy('escape');

  // Ionic uses AngularUI Router which uses the concept of states
  // Learn more here: https://github.com/angular-ui/ui-router
  // Set up the various states which the app can be in.
  // Each state's controller can be found in controllers.js
  $stateProvider

  // setup an abstract state for the tabs directive
    .state('tab', {
    url: '/tab',
    abstract: true,
    templateUrl: 'templates/tabs.html'
  })

  // Each tab has its own nav history stack:

  .state('tab.dash', {
    url: '/dash/:id',
    views: {
      'tab-dash': {
        templateUrl: 'templates/tab-dash.html',
        controller: 'DashCtrl'
      }
    }
  })

  .state('tab.request', {
          url: '/request',
          views: {
              'tab-chats': {
                  templateUrl:'templates/tab-request.html',
                  controller: 'RequestCtrl'
              }
          }
  })

  .state('tab.request-detail', {
      url: '/request/:id/:area',
      views: {
        'tab-chats': {
          templateUrl: 'templates/tab-request.html',
          controller: 'RequestCtrl'
        }
      }
  })

  .state('tab.chat-detail', {
      url: '/chats/:id',
      views: {
        'tab-chats': {
          templateUrl: 'templates/chat-detail.html',
          controller: 'ChatDetailCtrl'
        }
      }
  })

  .state('tab.account', {
    url: '/account',
    views: {
      'tab-account': {
        templateUrl: 'templates/tab-account.html',
        controller: 'AccountCtrl'
      }
    }
  });

  // if none of the above states are matched, use this as the fallback
  $urlRouterProvider.otherwise('/tab/dash/0');

});

Array.prototype.contains = function(obj) {
    var i = this.length;
    while (i--) {
        if (this[i] == obj) {
            return true;
        }
    }
    return false;
};

function cloneObject(obj) {
    if (obj === null || typeof obj !== 'object') {return obj;}
    var temp = obj.constructor();
    for (var key in obj) {
        temp[key] = cloneObject(obj[key]);
    }
    return temp;
}