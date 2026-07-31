window.CustomerApp = {

    async init() {

        try {

            this.showLoading();

            await this.initialize();

        } catch (e) {

            console.error(e);

        } finally {

            this.hideLoading();

        }

    },

    async initialize() {

        this.validateSession();

        CustomerRouter.initialize();

    },

    validateSession() {

        /*
         * We will integrate customer authentication here.
         *
         * Example:
         *
         * if (!Auth.isAuthenticated()) {
         *     window.location.href = "../auth/login.html";
         *     return;
         * }
         *
         */

    },

    showLoading() {

        document
            .getElementById("loadingOverlay")
            .classList
            .remove("d-none");

    },

    hideLoading() {

        document
            .getElementById("loadingOverlay")
            .classList
            .add("d-none");

    }

};

document.addEventListener(

    "DOMContentLoaded",

    () => CustomerApp.init()

);