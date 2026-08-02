window.Navbar = {

    render(title = "Dashboard") {

        const user =
            Auth.getUser();

        const firstName =
            user.firstName || "";

        const lastName =
            user.lastName || "";

        const fullName =
            `${firstName} ${lastName}`.trim();

        const role =
            this.getPrimaryRole(
                user.roles || []
            );

        const avatar =
            firstName
                ? firstName.charAt(0).toUpperCase()
                : "U";

        const today =
            new Date().toLocaleDateString(

                "en-IN",

                {

                    weekday: "long",

                    day: "numeric",

                    month: "long",

                    year: "numeric"

                }

            );

        return `

<div class="navbar-container">

    <div class="navbar-left">

        <h4
            class="page-title mb-0">

            ${title}

        </h4>

        <small class="text-secondary">

            ${today}

        </small>

    </div>

    <div class="navbar-right">

        <button
            class="icon-button">

            <i class="bi bi-search"></i>

        </button>

        <button
            class="icon-button">

            <i class="bi bi-bell"></i>

        </button>

        <button
            class="icon-button">

            <i class="bi bi-moon"></i>

        </button>

        <div class="user-profile">

            <div class="avatar">

                ${avatar}

            </div>

            <div>

                <div class="user-name">

                    ${fullName}

                </div>

                <small class="text-secondary">

                    ${role}

                </small>

            </div>

        </div>

    </div>

</div>

`;

    },

    getPrimaryRole(roles) {

        const priority = [

            "MANAGER",

            "SERVICE_ADVISOR",

            "TECHNICIAN",

            "ACCOUNTANT",

            "CASHIER",

            "INVENTORY_MANAGER"

        ];

        const labels = {

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
            priority.find(r =>
                roles.includes(r)
            );

        return labels[role] || "Employee";

    }

};