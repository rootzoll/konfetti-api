angular.module('starter.controllers', [])

.controller('DashCtrl', function($rootScope, $scope, $translate, $timeout, $ionicPopup, $log) {

        $scope.actualKonfettiCount = 1000;
        $scope.loadingParty = false;
        $scope.actualSorting = null;

        $scope.notifications = [
            {id: 12, type:1, ref:123},
            {id: 87, type:2, ref:655},
            {id: 87, type:3, ref:633}
        ];

        $scope.requestsReview = [];

        $scope.requestsPosted = [
            {   id: 12,
                userId: 123,
                konfettiCount: 999,
                title: 'Hecke am Spielplatz schneiden',
                imageUrl: 'http://img2.timeinc.net/people/i/2011/database/110214/christian-bale-300.jpg',
                state: 'review'
            }
        ];

        $scope.requestsInteraction = [
            {   id: 13,
                userId: 124,
                konfettiCount: 1,
                title: 'Aufbau Grillfest bei Jannes auf dem Acker',
                imageUrl: 'http://www.mnf.uni-greifswald.de/fileadmin/Biochemie/AK_Heinicke/bilder/kontaktbilder/Fischer__Christian_II_WEB.jpg',
                state: 'open'
            }
        ];

        $scope.requestsOpen = [];

        // available app languages
        $scope.langSet = [
            {code:'en', display:'English', dir:'ltr'},
            {code:'de', display:'Deutsch', dir:'ltr'},
            {code:'ar', display:'عربي', dir:'rtl'}
        ];

        // sorting options
        $scope.sortSet = [];
        $scope.updateSortOptions = function() {
            $translate("POSTSORT_MOST").then(function (POSTSORT_MOST) {
                $translate("POSTSORT_NEW").then(function (POSTSORT_NEW) {
                    $timeout(function() {
                        console.log("RESET");
                        $scope.sortSet = [
                            {sort:'most', display:POSTSORT_MOST},
                            {sort:'new', display:POSTSORT_NEW}
                        ];
                        $scope.actualSorting = $scope.sortSet[0];
                    },10);
                });
            });
        };
        $scope.updateSortOptions();

        // the sorting of open tasks changed
        $scope.changedSorting = function(actualSorting) {
            alert("TODO");
            if ((typeof actualSorting != "undefined") && (actualSorting!=null)) {
                $scope.actualSorting = actualSorting;
            } else {
                $scope.actualSorting = $scope.sortSet[0];
            }
            $timeout(function(){
                console.dir($scope.actualSorting.sort);
            },100);
        };

        // setting selected lang in view to setting
        $scope.actualLangSelect = $scope.langSet[0];
        for (i = 0; i < $scope.langSet.length; i++) {
            if ($scope.langSet[i].code===$rootScope.lang) {
                $scope.actualLangSelect = $scope.langSet[i];
                break;
            }
        }

        // receiving changes lang settings --> with i18n
        $scope.selectedLang = function(selected) {
            $translate.use(selected.code);
            $rootScope.spClass = selected.dir;
            $scope.updateSortOptions();
        };

        // the previous party from list (closer)
        $scope.buttonPartyPrev = function() {
            $scope.loadingParty = true;
            $timeout(function(){
                $scope.loadingParty = false;
            }, 2000);
        };

        // next party in list (more far away)
        $scope.buttonPartyNext = function() {
            $scope.loadingParty = true;
            $timeout(function(){
                $scope.loadingParty = false;
            }, 2000);
        };

        $scope.tapNotificationMore = function($event, noti) {
            alert("TODO more notification id("+noti.id+")");
        };

        $scope.tapNotificationDelete = function($event, noti) {
            $event.stopPropagation();
            alert("TODO delete notification id("+noti.id+")");
        };

        $scope.tapRequestMore = function($event, request) {
            alert("TODO more request id("+request.id+") ");
        };

        $scope.tapRequestKonfetti = function($event, request) {
            $event.stopPropagation();
            request.konfettiCount++;
        };

        // pop jup with more info in party orga
        $scope.showPartyInfo = function() {

            $scope.orga = {
                name: 'Helferverein Nord e.V.',
                town: 'Berlin-Pankow',
                address: 'Berliner Str. 99, 13189 Berlin, GERMANY',
                person: 'Max Mustermann',
                website: 'http://pankowhilft.blogsport.de'
            };

            $translate("ORGAINFO_TITLE").then(function (ORGAINFO_TITLE) {
                $translate("ORGAINFO_SUB").then(function (ORGAINFO_SUB) {
                    // An elaborate, custom popup
                    var myPopup = $ionicPopup.show({
                        template: '<h4>{{orga.name}}</h4><br>{{orga.address}}<br>{{orga.person}}',
                        title: ORGAINFO_TITLE,
                        subTitle: ORGAINFO_SUB,
                        scope: $scope,
                        buttons: [
                            { text: '<i class="icon ion-ios-close-outline"></i>' },
                            {
                                text: '<i class="icon ion-information-circled"></i>',
                                type: 'button-positive',
                                onTap: function(e) {
                                    window.open($scope.orga.website, "_system");
                                }
                            }
                        ]
                    });
                    myPopup.then(function(res) {
                        console.log('Tapped!', res);
                    });
                });
            });


        };

    })

.controller('ChatsCtrl', function($scope, Chats) {
  // With the new view caching in Ionic, Controllers are only called
  // when they are recreated or on app start, instead of every page change.
  // To listen for when this page is active (for example, to refresh data),
  // listen for the $ionicView.enter event:
  //
  //$scope.$on('$ionicView.enter', function(e) {
  //});

  $scope.chats = Chats.all();
  $scope.remove = function(chat) {
    Chats.remove(chat);
  };
})

.controller('ChatDetailCtrl', function($scope, $stateParams, Chats) {
  $scope.chat = Chats.get($stateParams.chatId);
})

.controller('AccountCtrl', function($scope) {
  $scope.settings = {
    enableFriends: true
  };
});
