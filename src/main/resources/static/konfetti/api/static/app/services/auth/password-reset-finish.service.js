(function() {
    'use strict';

    angular
        .module('app')
        .factory('PasswordResetFinish', PasswordResetFinish);

    PasswordResetFinish.$inject = ['$resource'];

    function PasswordResetFinish($resource) {
        var service = $resource('../../../konfetti/api/account/reset_password/finish', {}, {});
        return service;
    }
})();