window.Navbar = {

    render(title = "Today's Garage") {

        const today = new Date().toLocaleDateString("en-IN", {
            weekday: "long",
            day: "numeric",
            month: "long",
            year: "numeric"
        });

        return `

<div class="navbar-container">

    <div class="navbar-left">

        <h4 class="page-title mb-0">

            ${title}

        </h4>

        <small class="text-secondary">

            ${today}

        </small>

    </div>

    <div class="navbar-right">

        <button class="icon-button">

            <i class="bi bi-bell"></i>

        </button>

        <button class="icon-button">

            <i class="bi bi-moon"></i>

        </button>

        <div class="user-profile">

            <div class="avatar">

                V

            </div>

            <div>

                <div class="user-name">

                    Varun

                </div>

                <small class="text-secondary">

                    Administrator

                </small>

            </div>

        </div>

    </div>

</div>

`;

    }

};