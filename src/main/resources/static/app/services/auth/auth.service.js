(function() {
    'use strict';

    angular
        .module('app')
        .factory('Auth', Auth);

    Auth.$inject = ['PasswordResetFinish'];

    function Auth (PasswordResetFinish) {
        var service = {
            resetPasswordFinish: resetPasswordFinish
        };

        return service;

        function resetPasswordFinish (keyAndPassword, callback) {
            var cb = callback || angular.noop;

            return PasswordResetFinish.save(keyAndPassword, function () {
                return cb();
            }, function (err) {
                return cb(err);
            }).$promise;
        }
    }
})();
