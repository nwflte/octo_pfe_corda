"use strict";

const app = angular.module('demoAppModule', ['ui.bootstrap']);

// Fix for unhandled rejections bug.
app.config(['$qProvider', function ($qProvider) {
    $qProvider.errorOnUnhandledRejections(false);
}]);

app.controller('DemoAppController', function($http, $location, $uibModal) {
    const demoApp = this;

    const apiBaseURL = "/api/obligations/";
    let peers = [];

    $http.get(apiBaseURL + "me").then((response) => demoApp.thisNode = response.data.me);

    $http.get(apiBaseURL + "peers").then((response) => peers = response.data.peers);

    demoApp.openModal = () => {
        const modalInstance = $uibModal.open({
            templateUrl: 'demoAppModal.html',
            controller: 'ModalInstanceCtrl',
            controllerAs: 'modalInstance',
            resolve: {
                demoApp: () => demoApp,
                apiBaseURL: () => apiBaseURL,
                peers: () => peers
            }
        });

        modalInstance.result.then(() => {}, () => {});
    };

    demoApp.getObligations = () => $http.get(apiBaseURL + "obligations")
        .then((response) => { console.log(response.data); return demoApp.obligations = Object.keys(response.data)
            .map((key) => response.data[key].state.data)
            .reverse()});

    demoApp.getMyObligations = () => $http.get(apiBaseURL + "my-obligations")
        .then((response) => demoApp.myobligations = Object.keys(response.data)
            .map((key) => response.data[key].state.data)
            .reverse());

    demoApp.getObligations();
    demoApp.getMyObligations();
});

app.controller('ModalInstanceCtrl', function ($http, $location, $uibModalInstance, $uibModal, demoApp, apiBaseURL, peers) {
    const modalInstance = this;

    modalInstance.peers = peers;
    modalInstance.form = {};
    modalInstance.formError = false;

    // Validates and sends IOU.
    modalInstance.create = function validateAndSendDDRObligationRedeem() {
        if (modalInstance.form.value <= 0) {
            modalInstance.formError = true;
        } else {
            modalInstance.formError = false;
            $uibModalInstance.close();

            let CREATE_OBLIGATIONS_PATH = apiBaseURL + "create-obligation"

            let createObligationData = $.param({
                amount : modalInstance.form.value
            });

            let createObligationHeaders = {
                headers : {
                    "Content-Type": "application/x-www-form-urlencoded"
                }
            };

            // Create Obligation  and handles success / fail responses.
            $http.post(CREATE_OBLIGATIONS_PATH, createObligationData, createObligationHeaders).then(
                modalInstance.displayMessage,
                modalInstance.displayMessage
            );
        }
    };

    modalInstance.displayMessage = (message) => {
        const modalInstanceTwo = $uibModal.open({
            templateUrl: 'messageContent.html',
            controller: 'messageCtrl',
            controllerAs: 'modalInstanceTwo',
            resolve: { message: () => message }
        });

        // No behaviour on close / dismiss.
        modalInstanceTwo.result.then(() => {}, () => {});
    };

    // Close create Obligation modal dialogue.
    modalInstance.cancel = () => $uibModalInstance.dismiss();

    // Validate the IOU.
    function invalidFormInput() {
        return isNaN(modalInstance.form.value);
    }
});

// Controller for success/fail modal dialogue.
app.controller('messageCtrl', function ($uibModalInstance, message) {
    const modalInstanceTwo = this;
    modalInstanceTwo.message = message.data;
});