window.CustomerSidebar = {

    menus: [

        {
            id: "dashboard",
            title: "Dashboard",
            icon: "bi-speedometer2"
        },

        {
            id: "vehicles",
            title: "My Vehicles",
            icon: "bi-car-front"
        },

        {
            id: "repair",
            title: "Repair Workflow",
            icon: "bi-tools"
        },

        {
            id: "estimates",
            title: "Estimate Approval",
            icon: "bi-receipt"
        },

        {
            id: "invoices",
            title: "Invoices",
            icon: "bi-file-earmark-text"
        },

        {
            id: "profile",
            title: "Profile",
            icon: "bi-person"
        }

    ],

    render(activePage = "dashboard") {

        const sidebar =
            document.getElementById("customerSidebar");

        let html = `

            <div class="sidebar">

                <div class="sidebar-logo">

                    <div class="logo-icon">

                        <i class="bi bi-car-front-fill"></i>

                    </div>

                    <div class="logo-text">

                        <h4>

                            GarageOS

                        </h4>

                        <small>

                            Customer Portal

                        </small>

                    </div>

                </div>

                <div class="sidebar-menu">

                    <div class="sidebar-title">

                        MAIN MENU

                    </div>

        `;

        this.menus.forEach(menu => {

            html += `

                <div
                    class="sidebar-item ${activePage === menu.id ? "active" : ""}"
                    onclick="CustomerRouter.navigate('${menu.id}')">

                    <i class="bi ${menu.icon}"></i>

                    <span>

                        ${menu.title}

                    </span>

                </div>

            `;

        });

        html += `

                </div>

                <div class="sidebar-footer">

                    <div class="customer-summary">

                        <h6 id="sidebarCustomerName">

                            Customer

                        </h6>

                        <small>

                            GarageOS Member

                        </small>

                    </div>

                    <button
                        class="logout-button mt-3"
                        onclick="CustomerSidebar.logout()">

                        <i class="bi bi-box-arrow-right"></i>

                        Logout

                    </button>

                </div>

            </div>

        `;

        sidebar.innerHTML = html;

        this.loadCustomer();

    },

    async loadCustomer() {

        try {

            if (!CustomerPortalService.getProfile) {

                return;

            }

            const customer =
                await CustomerPortalService.getProfile();

            if (!customer) {

                return;

            }

            document.getElementById(
                "customerName"
            ).textContent =
                customer.name;

            document.getElementById(
                "sidebarCustomerName"
            ).textContent =
                customer.name;

        } catch (e) {

            console.error(e);

        }

    },

    logout() {

        if (window.Auth && Auth.logout) {

            Auth.logout();

            return;

        }

        window.location.href =
            "../auth/login.html";

    }

};