window.Sidebar = {
    render() {
        return `
            <div style="padding:20px;color:white;">
                <a class="menu-item active"
                   href="#"
                   data-page="dashboard">

                    <i class="bi bi-speedometer2"></i>

                    Dashboard

                </a>

                <a class="menu-item"
                   href="#"
                   data-page="workflow">

                    <i class="bi bi-car-front-fill"></i>

                    Start Service

                </a>

                <a class="menu-item"
                   href="#">

                    <i class="bi bi-list-task"></i>

                    Active Jobs

                </a>

                <a class="menu-item"
                   href="#">

                    <i class="bi bi-people"></i>

                    Customers

                </a>

                <a class="menu-item"
                   href="#">

                    <i class="bi bi-car-front"></i>

                    Vehicles

                </a>
            </div>
        `;
    },
    bindEvents() {

        document
            .querySelectorAll(".menu-item[data-page]")
            .forEach(item => {

                item.addEventListener("click", e => {

                    e.preventDefault();

                    Router.navigate(
                        item.dataset.page
                    );

                });

            });

    }
};

console.log("Sidebar Loaded");