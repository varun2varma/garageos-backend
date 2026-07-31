window.CustomerProfile = {

    async init() {

        document
            .getElementById("refreshProfileButton")
            ?.addEventListener(

                "click",

                () => this.load()

            );

        await this.load();

    },

    async load() {

        try {

            CustomerApp.showLoading();

            const customer =
                await CustomerPortalService.getProfile();

            this.render(customer);

        } catch (e) {

            console.error(e);

        } finally {

            CustomerApp.hideLoading();

        }

    },

    render(customer) {

        if (!customer) {

            return;

        }

        document.getElementById("customerName").value =
            customer.name ?? "";

        document.getElementById("customerMobile").value =
            customer.mobileNumber ?? "";

        document.getElementById("customerEmail").value =
            customer.email ?? "";

        document.getElementById("customerGst").value =
            customer.gstNumber ?? "";

        document.getElementById("customerAddress").value =
            customer.address ?? "";

    }

};