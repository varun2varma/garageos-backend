window.OwnerSidebar = {

    menus: [

        {

            id: "dashboard",

            title: "Dashboard",

            icon: "bi-speedometer2"

        },

        {

            id: "employees",

            title: "Employees",

            icon: "bi-people-fill"

        },

        {

            id: "customers",

            title: "Customers",

            icon: "bi-person-lines-fill"

        },

        {

            id: "vehicles",

            title: "Vehicles",

            icon: "bi-car-front"

        },

        {

            id: "inventory",

            title: "Inventory",

            icon: "bi-box-seam"

        },

        {

            id: "reports",

            title: "Reports",

            icon: "bi-bar-chart"

        },

        {

            id: "settings",

            title: "Settings",

            icon: "bi-gear"

        }

    ],

    render(activePage = "dashboard") {

        const sidebar =

            document.getElementById(

                "ownerSidebar"

            );

        let html = `

<div class="sidebar">

<div class="sidebar-logo">

<div class="logo-icon">

<i class="bi bi-building"></i>

</div>

<div class="logo-text">

<h4>GarageOS</h4>

<small>Owner Portal</small>

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

class="sidebar-item ${activePage===menu.id?"active":""}"

onclick="OwnerRouter.navigate('${menu.id}')">

<i class="bi ${menu.icon}"></i>

<span>${menu.title}</span>

</div>

`;

        });

        html += `

</div>

<div class="sidebar-footer">

<h6>

Garage Owner

</h6>

<button

class="logout-button mt-3"

onclick="OwnerSidebar.logout()">

<i class="bi bi-box-arrow-right"></i>

Logout

</button>

</div>

</div>

`;

        sidebar.innerHTML = html;

    },

    logout() {

        Auth.logout();

    }

};