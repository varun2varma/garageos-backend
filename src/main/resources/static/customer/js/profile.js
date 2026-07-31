window.CustomerProfile = {

    async init() {

        document

            .getElementById(
                "refreshProfileButton"
            )

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

                await CustomerPortalService
                    .getProfile();

            this.render(customer);

        }

        catch (error) {

            console.error(error);

            this.renderEmpty();

        }

        finally {

            CustomerApp.hideLoading();

        }

    },

    render(customer) {

        if (!customer) {

            this.renderEmpty();

            return;

        }

        document.getElementById(
            "profileHeadingName"
        ).textContent =
            customer.name;

        document.getElementById(
            "customerId"
        ).value =
            customer.id ?? "";

        document.getElementById(
            "customerName"
        ).value =
            customer.name ?? "";

        document.getElementById(
            "customerMobile"
        ).value =
            customer.mobileNumber ?? "";

        document.getElementById(
            "customerEmail"
        ).value =
            customer.email ?? "";

        document.getElementById(
            "customerAddress"
        ).value =
            customer.address ?? "";

            },

            renderEmpty() {

                const container =
                    document.getElementById(
                        "profileContainer"
                    );

                if (!container) {

                    return;

                }

                container.innerHTML = `

                    <div class="customer-card text-center py-5">

                        <i class="bi bi-person-x display-3 text-secondary"></i>

                        <h3 class="mt-4">

                            Profile Not Available

                        </h3>

                        <p class="text-muted">

                            Unable to load your profile.

                        </p>

                    </div>

                `;

            }

        };

