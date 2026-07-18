window.Dashboard = {

    render() {

        return `

<div class="fade-in">

    <div class="container-fluid">

        <!-- Welcome -->

        <div class="row mb-4">

            <div class="col">

                <h2 class="fw-bold">
                    Welcome Back 👋
                </h2>

                <p class="text-secondary">
                    Manage today's garage operations from one place.
                </p>

            </div>

        </div>

        <!-- Hero Card -->

        <div class="row mb-4">

            <div class="col">

                <div class="card shadow-sm border-0">

                    <div class="card-body p-5">

                        <div class="row align-items-center">

                            <div class="col-lg-8">

                                <h3 class="fw-bold mb-3">

                                    🚗 Start New Service

                                </h3>

                                <p class="text-secondary mb-4">

                                    Receive a vehicle, inspect it,
                                    prepare estimate, generate invoice
                                    and complete delivery.

                                </p>

                                <button
                                    id="startServiceBtn"
                                    class="btn btn-primary btn-lg">

                                    Start Service

                                </button>

                            </div>

                            <div
                                class="col-lg-4 text-center">

                                <i
                                    class="bi bi-car-front-fill"
                                    style="
                                        font-size:120px;
                                        color:#2563EB;
                                    ">

                                </i>

                            </div>

                        </div>

                    </div>

                </div>

            </div>

        </div>

        <!-- Statistics -->

        <div class="row g-4 mb-4">

            <div class="col-lg-3">

                <div class="card">

                    <div class="card-body">

                        <h6 class="text-secondary">

                            Active Jobs

                        </h6>

                        <h2>

                            18

                        </h2>

                    </div>

                </div>

            </div>

            <div class="col-lg-3">

                <div class="card">

                    <div class="card-body">

                        <h6 class="text-secondary">

                            Pending Estimates

                        </h6>

                        <h2>

                            5

                        </h2>

                    </div>

                </div>

            </div>

            <div class="col-lg-3">

                <div class="card">

                    <div class="card-body">

                        <h6 class="text-secondary">

                            Ready Delivery

                        </h6>

                        <h2>

                            3

                        </h2>

                    </div>

                </div>

            </div>

            <div class="col-lg-3">

                <div class="card">

                    <div class="card-body">

                        <h6 class="text-secondary">

                            Completed Today

                        </h6>

                        <h2>

                            12

                        </h2>

                    </div>

                </div>

            </div>

        </div>

        <!-- Recent Jobs -->

        <div class="card">

            <div class="card-header bg-white">

                <h5 class="mb-0">

                    Recent Jobs

                </h5>

            </div>

            <div class="table-responsive">

                <table class="table table-hover mb-0">

                    <thead>

                    <tr>

                        <th>Job Card</th>

                        <th>Customer</th>

                        <th>Vehicle</th>

                        <th>Status</th>

                    </tr>

                    </thead>

                    <tbody>

                    <tr>

                        <td>JC000101</td>

                        <td>Rahul</td>

                        <td>Hyundai i20</td>

                        <td>

                            <span class="badge bg-warning">

                                Inspection

                            </span>

                        </td>

                    </tr>

                    <tr>

                        <td>JC000102</td>

                        <td>Suresh</td>

                        <td>Honda City</td>

                        <td>

                            <span class="badge bg-info">

                                Estimate

                            </span>

                        </td>

                    </tr>

                    <tr>

                        <td>JC000103</td>

                        <td>Ajay</td>

                        <td>Creta</td>

                        <td>

                            <span class="badge bg-success">

                                Ready

                            </span>

                        </td>

                    </tr>

                    </tbody>

                </table>

            </div>

        </div>

    </div>

</div>

`;

    },

    bindEvents() {

        document
            .getElementById("startServiceBtn")
            ?.addEventListener("click", () => {

                Router.navigate("workflow");

            });

    }

};