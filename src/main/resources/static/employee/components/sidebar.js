window.Sidebar = {

    menu: [

        {
            page: "dashboard",
            title: "Dashboard",
            icon: "bi-speedometer2",
            roles: [
                "MANAGER",
                "SERVICE_ADVISOR",
                "TECHNICIAN",
                "ACCOUNTANT",
                "CASHIER",
                "INVENTORY_MANAGER"
            ]
        },

        {
            page: "workflow",
            title: "Start Service",
            icon: "bi-car-front-fill",
            roles: [
                "MANAGER",
                "SERVICE_ADVISOR"
            ]
        },

        {
            page: "activeJobs",
            title: "Active Jobs",
            icon: "bi-list-task",
            roles: [
                "MANAGER",
                "SERVICE_ADVISOR",
                "TECHNICIAN"
            ]
        },

        {
            page: "customers",
            title: "Customers",
            icon: "bi-people",
            roles: [
                "MANAGER",
                "SERVICE_ADVISOR"
            ]
        },

        {
            page: "vehicles",
            title: "Vehicles",
            icon: "bi-car-front",
            roles: [
                "MANAGER",
                "SERVICE_ADVISOR"
            ]
        },

        {
            page: "inventory",
            title: "Inventory",
            icon: "bi-box-seam",
            roles: [
                "MANAGER",
                "INVENTORY_MANAGER"
            ]
        },

        {
            page: "reports",
            title: "Reports",
            icon: "bi-graph-up",
            roles: [
                "MANAGER",
                "ACCOUNTANT"
            ]
        }

    ],
    render() {

        const user =
            Auth.getUser();

        const roles =
            user.roles || [];

        let html = `

<div class="sidebar-container">

    <div class="sidebar-logo">

        <i
            class="bi bi-tools">

        </i>

        <div>

            <h4>

                GarageOS

            </h4>

            <small>

                Employee Portal

            </small>

        </div>

    </div>

    <div class="sidebar-menu">

`;

        this.menu.forEach(item => {

            const visible =
                item.roles.some(role =>
                    roles.includes(role)
                );

            if (!visible) {

                return;

            }

            html += `

<a

    href="#"

    class="menu-item"

    data-page="${item.page}">

    <i class="bi ${item.icon}"></i>

    <span>

        ${item.title}

    </span>

</a>

`;

        });

        html += `

    </div>

`;

        const firstName =
            user.firstName || "";

        const lastName =
            user.lastName || "";

        const fullName =
            `${firstName} ${lastName}`.trim();

        const avatar =
            firstName
                ? firstName.charAt(0).toUpperCase()
                : "U";

        html += `

<div class="sidebar-footer">

    <div class="employee-profile">

        <div class="avatar">

            ${avatar}

        </div>

        <div>

            <strong>

                ${fullName}

            </strong>

            <br>

            <small>

                ${this.primaryRole(user.roles)}

            </small>

        </div>

    </div>

    <button

        id="logoutBtn"

        class="btn btn-outline-light w-100 mt-3">

        Logout

    </button>

</div>

</div>

`;

        return html;

    },

    bindEvents() {

        document

            .querySelectorAll(".menu-item")

            .forEach(menu => {

                menu.onclick = e => {

                    e.preventDefault();

                    Router.navigate(

                        menu.dataset.page

                    );

                };

            });

        document

            .getElementById("logoutBtn")

            ?.addEventListener(

                "click",

                async () => {

                    await Auth.logout();

                }

            );

    },

    primaryRole(roles) {

        const order = [

            "MANAGER",

            "SERVICE_ADVISOR",

            "TECHNICIAN",

            "ACCOUNTANT",

            "CASHIER",

            "INVENTORY_MANAGER"

        ];

        const names = {

            MANAGER:
                "Manager",

            SERVICE_ADVISOR:
                "Service Advisor",

            TECHNICIAN:
                "Technician",

            ACCOUNTANT:
                "Accountant",

            CASHIER:
                "Cashier",

            INVENTORY_MANAGER:
                "Inventory Manager"

        };

        const role =
            order.find(r =>
                roles.includes(r)
            );

        return names[role] || "Employee";

    }

};