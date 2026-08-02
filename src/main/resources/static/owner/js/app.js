window.OwnerApp = {

    async init() {

        try {

            this.showLoading();

            await OwnerRouter.initialize();

        }

        catch (e) {

            console.error(e);

        }

        finally {

            this.hideLoading();

        }

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

    () => OwnerApp.init()

);